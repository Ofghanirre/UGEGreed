package fr.uge.ugegreed;


import fr.uge.ugegreed.commands.*;
import fr.uge.ugegreed.jobs.Jobs;
import fr.uge.ugegreed.packets.DiscPacket;
import fr.uge.ugegreed.packets.InitPacket;
import fr.uge.ugegreed.packets.Packet;
import fr.uge.ugegreed.packets.UpdtPacket;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Main controller for the application.
 * Manages the TCP connections and communication.
 */
public class Controller {
  private static final Logger logger = Logger.getLogger(Controller.class.getName());
  private final Selector selector;
  private final InetSocketAddress parentAddress;
  private final int listenPort;
  private final ServerSocketChannel serverSocketChannel;
  private final SocketChannel parentSocketChannel;
  private final Jobs jobs;
  private int potential = 1;

  private SelectionKey parentKey = null;
  private final ArrayBlockingQueue<Command> commandQueue = new ArrayBlockingQueue<>(8);

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
    jobs = new Jobs(resultPath, this);
    this.parentAddress = parentAddress;
    this.serverSocketChannel = ServerSocketChannel.open();
    serverSocketChannel.bind(new InetSocketAddress(listenPort));
    this.parentSocketChannel = SocketChannel.open();
  }

  /**
   * Sends a console command to the controller
   * @param command command to send
   * @throws InterruptedException if the thread sending the command is interrupted
   */
  public void sendCommand(Command command) throws InterruptedException {
    synchronized (commandQueue) {
      commandQueue.put(command);
      selector.wakeup();
    }
  }

  private void processCommands() {
    for (;;) {
      synchronized (commandQueue) {
        var command = commandQueue.poll();
        if (command == null) { return; }
        switch (command) {
          case CommandStart commandStart -> processStartCommand(commandStart);
          case CommandDisconnect commandDisconnect -> processDisconnectCommand(commandDisconnect);
          case CommandDebug commandDebug -> processDebugCommand(commandDebug);
          case CommandHelp commandHelp -> processHelpCommand();
          default -> throw new UnsupportedOperationException("Unknown command: " + command);
        }
      }
    }
  }

  private void processStartCommand(CommandStart command) {
    var result = jobs.createJob(command.urlJAR(), command.className(), command.rangeStart(), command.rangeEnd(), command.filename());
    if (!result) {
      logger.info("Job could not be started");
    }
  }

  private void processDisconnectCommand(CommandDisconnect command) {
    if (parentKey == null) {
      logger.warning("Cannot initiate disconnection on ROOT node");
    }
    logger.info("Initiating disconnection");
    broadcastDisconnection();
  }

  private void processDebugCommand(CommandDebug command) {
    switch (command.debugCode()) {
      case POTENTIAL -> {
        System.out.println("Total potential: " + potential);
        System.out.println("Neighboring potentials:");
        availableNodesStream().forEach(ctx -> System.out.println(ctx.host() + " -> " + ctx.potential()));
      }
      // Code other debug codes here!
      default -> System.out.println("Unknown debug code: " + command.debugCode());
    }
  }

  private void processHelpCommand() {
    System.out.println("Console Help:");
    List<Command> commands = List.of(
            new CommandHelp(),
            new CommandDebug(CommandDebugCode.POTENTIAL),
            new CommandStart("dummy", "dummy", 1, 2, "dummy"),
            new CommandDisconnect()
    );
    System.out.println("Commands (" + commands.size() + ")");
    System.out.println(commands.stream().map(Command::getName).collect(Collectors.joining(" - ")) + "\n");
    commands.forEach(c -> System.out.println(c.getHelp() + "\n"));
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
      parentKey = key;
      logger.info("Connecting to parent...");
    } else {
      logger.info("In ROOT mode.");
    }

    var console = new Console(this);
    Thread.ofPlatform().daemon().start(console::consoleRun);

    while(!Thread.interrupted()) {
      try {
        selector.select(this::treatKey, 100);
        jobs.processContextQueue();
        jobs.processTaskExecutorQueue();
        processCommands();
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

    // Configure new key
    logger.info("Client " + sc.getRemoteAddress() + " connected.");
    sc.configureBlocking(false);
    var clientKey = sc.register(selector, SelectionKey.OP_READ);
    var context = new ConnectionContext(this, clientKey);
    clientKey.attach(context);

    // Potential management
    context.queuePacket(new InitPacket(potential));
    reevaluatePotential();
    availableNodesStream().forEach(ctx -> {
      if (ctx.key() != clientKey) {
        ctx.queuePacket(new UpdtPacket(potential - ctx.potential()));
      }
    });
  }

  private void silentlyClose(SelectionKey key) {
    try {
      key.channel().close();
    } catch (IOException ignored) {
    }
  }

  /**
   * Returns a stream of all connected nodes that are available (i.e. not disconnecting)
   * @return stream of all connected nodes
   */
  public Stream<ConnectionContext> availableNodesStream() {
    return selector.keys().stream().map(SelectionKey::attachment)
        .mapMulti((a, consumer) -> {
          if (a instanceof ConnectionContext ctx && !ctx.isDisconnecting()) { consumer.accept((ConnectionContext) a); }
        });
  }

  /**
   * Returns total potential of the network
   * @return total potential of the network
   */
  public int potential() {
    return potential;
  }

  /**
   * Reevaluates the total potential of the network
   */
  public void reevaluatePotential() {
    potential = 1 + availableNodesStream().reduce(0, (n, ctx) -> n + ctx.potential(), Integer::sum);
  }

  /**
   * Broadcasts new potential value to all neighbor except the one the update
   * package came from
   * @param incomingHost host the update package came from
   */
  public void updateNeighbors(SelectionKey incomingHost) {
    reevaluatePotential();
    availableNodesStream().forEach(ctx -> {
      if (ctx.key() != incomingHost) {
        ctx.queuePacket(new UpdtPacket(potential - ctx.potential()));
      }
    });
  }

  /**
   * Transmits a packet from a context to the job manager
   * @param packet packet to transmit
   * @param context context it came from
   */
  public void transmitPacketToJobs(Packet packet, ConnectionContext context) {
    jobs.queueContextPacket(packet, context);
  }

  private void broadcastDisconnection() {
    int nbReco = (int) availableNodesStream().count() - 1;
    var jobsUpstreamOfParent = jobs.getJobsUpstreamOfNode(parentKey);
    availableNodesStream().forEach(ctx -> {
      if (ctx.key() == parentKey) {
        var innerDiskPacketSize = jobsUpstreamOfParent.size();
        var innerDiskPackets = new DiscPacket.InnerDiscPacket[innerDiskPacketSize];
        for (var i = 0 ; i < innerDiskPacketSize ; i++) {
          var job = jobsUpstreamOfParent.get(i);
          innerDiskPackets[i] = new DiscPacket.InnerDiscPacket(job.jobID(), job.getUpstreamContext().get().host());
        }
        ctx.queuePacket(new DiscPacket(nbReco, innerDiskPacketSize, innerDiskPackets));
      } else {
        ctx.queuePacket(new DiscPacket(0, 0, new DiscPacket.InnerDiscPacket[0]));
      }
    });
  }
}
