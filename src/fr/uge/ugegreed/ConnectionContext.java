package fr.uge.ugegreed;

import fr.uge.ugegreed.readers.PacketReader;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Objects;

public class ConnectionContext {
  private final static int BUFFER_SIZE = 1024;

  private final SelectionKey key;
  private final SocketChannel sc;
  private final ByteBuffer bufferIn = ByteBuffer.allocate(BUFFER_SIZE);
  private final ByteBuffer bufferOut = ByteBuffer.allocate(BUFFER_SIZE);
  private final Controller controller;
  private boolean closed = false;
  private final PacketReader packetReader = new PacketReader();

  public ConnectionContext(Controller controller, SelectionKey key) {
    Objects.requireNonNull(this.controller = controller);
    Objects.requireNonNull(this.key = key);
    sc = (SocketChannel) key.channel();
  }
}
