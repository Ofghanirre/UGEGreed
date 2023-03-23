package fr.uge.ugegreed;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Logger;

public class Controller {
  private static final Logger logger = Logger.getLogger(Controller.class.getName());
  private final Selector selector;
  private final InetSocketAddress parentAddress;
  private final Path resultPath;
  private final int listenPort;
  private final ServerSocketChannel serverSocketChannel;
  private final SocketChannel parentSocketChannel;

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
  public Controller(int listenPort, Path resultPath) throws IOException {
    this(listenPort, resultPath, null);
  }

  public void launch() throws IOException {
    serverSocketChannel.configureBlocking(false);
    serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    logger.info("Listening on port " + listenPort);

    if (parentAddress != null) {
      parentSocketChannel.configureBlocking(false);
      // TODO: create context + attach
      parentSocketChannel.register(selector, SelectionKey.OP_CONNECT);
      parentSocketChannel.connect(parentAddress);
    }

    // TODO: start console thread

    while(!Thread.interrupted()) {
      try {
        selector.select(this::treatKey);
      } catch (UncheckedIOException tunneled) {
        throw tunneled.getCause();
      }
    }
  }

  private void treatKey(SelectionKey key) {
  }
}
