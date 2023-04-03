package fr.uge.ugegreed;

import fr.uge.ugegreed.packets.InitPacket;
import fr.uge.ugegreed.packets.UpdtPacket;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Main controller for the application.
 * Manages the TCP connections and communication.
 */
public class Controller {
  private static final Logger logger = Logger.getLogger(Controller.class.getName());
  private final Selector selector;
  private final InetSocketAddress parentAddress;
  private final Path resultPath;
  private final int listenPort;
  private final ServerSocketChannel serverSocketChannel;
  private final SocketChannel parentSocketChannel;

  private int potential = 1;

  /**
   * Creates a new controller
   * @param listenPort port to listen for new connections on
   * @param resultPath path where to store results for jobs started by this application
   * @param parentAddress address of the parent to connect to, if NULL, application is in ROOT mode
   * @throws IOException in case of TCP layer errors
   */
  public Controller(int listenPort, Path resultPath, InetSocketAddress parentAddress) throws IOException {
    if (listenPort < 0 || listenPort > 65535) {
      throw new IllegalArgumentException("Port number is incorrect");
    }
    Objects.requireNonNull(resultPath);

    this.listenPort = listenPort;
    this.selector = Selector.open();
    this.resultPath = resultPath;
    this.parentAddress = parentAddress;
    this.serverSocketChannel = ServerSocketChannel.open();
    serverSocketChannel.bind(new InetSocketAddress(listenPort));
    this.parentSocketChannel = SocketChannel.open();
  }

  /**
   * Launches the controller
   * @throws IOException in case of TCP layer errors
   */
  public void launch() throws IOException {
    serverSocketChannel.configureBlocking(false);
    serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    logger.info("Listening on port " + listenPort);

    if (parentAddress != null) {
      parentSocketChannel.configureBlocking(false);
      var key = parentSocketChannel.register(selector, SelectionKey.OP_CONNECT);
      key.attach(new ConnectionContext(this, key));
      parentSocketChannel.connect(parentAddress);
    }

    // TODO: start console thread

    while(!Thread.interrupted()) {
      try {
        selector.select(this::treatKey);

        // DEBUG
        System.out.println("Total potential: " + potential);
        connectedNodeStream().forEach(ctx -> System.out.println(ctx.host() + " potential: " + ctx.potential()));
      } catch (UncheckedIOException tunneled) {
        throw tunneled.getCause();
      }
    }
  }

  private void treatKey(SelectionKey key) {
    try {
      if (key.isValid() && key.isAcceptable()) {
        doAccept(key);
      }
    } catch (IOException ioe) {
      // lambda call in select requires to tunnel IOException
      throw new UncheckedIOException(ioe);
    }
    try {
      if (key.isValid() && key.isConnectable()) {
        ((ConnectionContext) key.attachment()).doConnect();
      }
      if (key.isValid() && key.isWritable()) {
        ((ConnectionContext) key.attachment()).doWrite();
      }
      if (key.isValid() && key.isReadable()) {
        ((ConnectionContext) key.attachment()).doRead();
      }
    }
    catch (IOException e) {
      logger.log(Level.INFO, "Connection closed with client due to IOException", e);
      silentlyClose(key);
    }
  }

  private void doAccept(SelectionKey key) throws IOException {
    var ssc = (ServerSocketChannel) key.channel();
    var sc = ssc.accept();
    if (sc == null) {
      logger.warning("Failed to accept socket channel");
      return;
    }
    logger.info("Client " + sc.getRemoteAddress() + " connected.");
    sc.configureBlocking(false);
    var clientKey = sc.register(selector, SelectionKey.OP_READ);
    var context = new ConnectionContext(this, clientKey);
    clientKey.attach(context);

    // Potential management
    context.queuePacket(new InitPacket(potential));
    reevaluatePotential();
    connectedNodeStream().forEach(ctx -> {
      if (ctx.key() != clientKey) {
        ctx.queuePacket(new UpdtPacket(potential - ctx.potential()));
      }
    });
  }

  private void silentlyClose(SelectionKey key) {
    try {
      key.channel().close();
    } catch (IOException e) {
      // ignore exception
    }
  }

  // Returns a stream of the context of each connected node
  private Stream<ConnectionContext> connectedNodeStream() {
    return selector.keys().stream().map(SelectionKey::attachment)
        .flatMap(a -> {
          if (a instanceof ConnectionContext) { return Stream.of((ConnectionContext) a); }
          return Stream.empty();
        });
  }

  /**
   * Reevaluates the total potential of the network
   */
  public void reevaluatePotential() {
    potential = 1 + connectedNodeStream().reduce(0, (n, ctx) -> n + ctx.potential(), Integer::sum);
  }

  /**
   * Broadcasts new potential value to all neighbor except the one the update
   * package came from
   * @param incomingHost host the update package came from
   */
  public void updateNeighbors(SelectionKey incomingHost) {
    reevaluatePotential();
    connectedNodeStream().forEach(ctx -> {
      if (ctx.key() != incomingHost) {
        ctx.queuePacket(new UpdtPacket(potential - ctx.potential()));
      }
    });
  }
}
