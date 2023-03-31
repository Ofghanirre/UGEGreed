package fr.uge.ugegreed;

import fr.uge.ugegreed.packets.Packet;
import fr.uge.ugegreed.readers.PacketReader;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.logging.Logger;

public class ConnectionContext {
  private final static Logger logger = Logger.getLogger(ConnectionContext.class.getName());
  private final static int BUFFER_SIZE = 10_000;

  private final SelectionKey key;
  private final SocketChannel sc;
  private InetSocketAddress remoteHost;
  private final ByteBuffer bufferIn = ByteBuffer.allocate(BUFFER_SIZE);
  private final ByteBuffer bufferOut = ByteBuffer.allocate(BUFFER_SIZE);
  private final Controller controller;
  private boolean closed = false;
  private final PacketReader packetReader = new PacketReader();

  private final ArrayDeque<Packet> queue = new ArrayDeque<>();

  private int potential;

  public ConnectionContext(Controller controller, SelectionKey key) {
    this.controller = Objects.requireNonNull(controller);
    this.key = Objects.requireNonNull(key);
    sc = (SocketChannel) key.channel();
  }

  /**
   * Process the content of bufferIn
   * The convention is that bufferIn is in write-mode before the call to process
   * and after the call
   *
   */
  private void processIn() {
    for (;;) {
      var status = packetReader.process(bufferIn);
      switch (status) {
        case DONE -> {
          var packet = packetReader.get();
          logger.info("Received packet from " + remoteHost + ": " + packet);
          packetReader.reset();
        }
        case REFILL -> {return;}
        case ERROR -> {
          silentlyClose();
          return;
        }
      }
    }
  }

  /**
   * Add a packet to the packet queue, tries to fill bufferOut and updateInterestOps
   *
   * @param packet packet to queue for sending
   */
  public void queuePacket(Packet packet) {
    Objects.requireNonNull(packet);
    queue.add(packet);
    processOut();
    updateInterestOps();
  }

  /**
   * Try to fill bufferOut from the message queue
   *
   */
  private void processOut() {
    ByteBuffer nextPacket;
    while (!queue.isEmpty() && bufferOut.remaining() >= (nextPacket = queue.peek().toBuffer()).remaining()) {
      bufferOut.put(nextPacket);
      queue.remove();
    }
  }

  /**
   * Update the interestOps of the key looking only at values of the boolean
   * closed and of both ByteBuffers.
   * The convention is that both buffers are in write-mode before the call to
   * updateInterestOps and after the call. Also it is assumed that process has
   * been be called just before updateInterestOps.
   */

  private void updateInterestOps() {
    int ops = 0;
    if (!closed && bufferIn.hasRemaining()) {
      ops |= SelectionKey.OP_READ;
    }
    bufferOut.flip();
    if (bufferOut.hasRemaining()) {
      ops |= SelectionKey.OP_WRITE;
    }
    bufferOut.compact();
    key.interestOps(ops);
  }

  /**
   * Performs the read action on sc
   * The convention is that both buffers are in write-mode before the call to
   * doRead and after the call
   *
   * @throws IOException
   */
  public void doRead() throws IOException {
    var bytesRead = sc.read(bufferIn);
    if (bytesRead == -1) {
      closed = true;
    }
    processIn();
    updateInterestOps();
  }

  /**
   * Performs the write action on sc
   * The convention is that both buffers are in write-mode before the call to
   * doWrite and after the call
   *
   * @throws IOException
   */

  public void doWrite() throws IOException {
    bufferOut.flip();
    sc.write(bufferOut);
    bufferOut.compact();
    processOut();
    updateInterestOps();
  }

  public void doConnect() throws IOException {
    if (!sc.finishConnect()) return;
    key.interestOps(SelectionKey.OP_READ);
    remoteHost = (InetSocketAddress) sc.getRemoteAddress();
  }

  private void silentlyClose() {
    try {
      sc.close();
    } catch (IOException ignored) {
      // ignore exception
    }
  }
}
