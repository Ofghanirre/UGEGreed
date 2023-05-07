package fr.uge.ugegreed;


import fr.uge.ugegreed.commands.*;
import fr.uge.ugegreed.jobs.Job;
import fr.uge.ugegreed.jobs.Jobs;
import fr.uge.ugegreed.packets.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
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
public final class Controller {
  private static final Logger logger = Logger.getLogger(Controller.class.getName());
  private final Path jarPath;
  private final Selector selector;
  private InetSocketAddress parentAddress;

  private final int listenPort;
  private final ServerSocketChannel serverSocketChannel;
  private SocketChannel parentSocketChannel;
  private final Jobs jobs;
  private int potential = 1;

  private final int appID;

  // Related to disconnection
  private boolean disconnecting = false;
  private int disconnectionCounter;
  private final HashMap<Integer, ArrayList<Long>> upstreamHostsToreplace = new HashMap<>();

  private SelectionKey parentKey = null;
  private final ArrayBlockingQueue<Command> commandQueue = new ArrayBlockingQueue<>(8);

  private boolean useCache = false;

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

    jarPath = resultPath.resolve("_jars");
    Files.createDirectories(jarPath);

    this.listenPort = listenPort;
    selector = Selector.open();
    jobs = new Jobs(resultPath, this);
    this.parentAddress = parentAddress;
    serverSocketChannel = ServerSocketChannel.open();
    serverSocketChannel.bind(new InetSocketAddress(listenPort));

    parentSocketChannel = SocketChannel.open();

    // Insufficient if the network is deployed over multiple machines, but works in local...
    appID = serverSocketChannel.getLocalAddress().hashCode();
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

  private void processCommands() throws IOException {
    for (;;) {
      synchronized (commandQueue) {
        var command = commandQueue.poll();
        if (command == null) { return; }
        switch (command) {
          case CommandStart commandStart -> processStartCommand(commandStart);
          case CommandDisconnect ignored -> processDisconnectCommand();
          case CommandDebug commandDebug -> processDebugCommand(commandDebug);
          case CommandHelp ignored -> processHelpCommand();
          case CommandCache commandCache -> processCacheCommand(commandCache);
          default -> throw new UnsupportedOperationException("Unknown command: " + command);
        }
      }
    }
  }

  private void processStartCommand(CommandStart command) throws IOException {
    var result = jobs.createJob(command.urlJAR(), command.className(), command.rangeStart(), command.rangeEnd(), command.filename());
    if (!result) {
      logger.info("Job could not be started");
    }
  }

  private void processDisconnectCommand() {
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
        availableNodesStream().forEach(ctx -> System.out.println(ctx.host() + " | " + ctx.remoteAppId() + " -> " + ctx.potential()));
      }
      case ID -> System.out.println("App ID: " + appID);
      default -> System.out.println("Unknown debug code: " + command.debugCode());
    }
  }

  private void processCacheCommand(CommandCache commandCache) {
    logger.info("Set useCache value to " + commandCache.useCache());
    this.useCache = commandCache.useCache();
  }

  private void processHelpCommand() {
    System.out.println("Console Help:");
    List<Command> commands = List.of(
            new CommandHelp(),
            new CommandDebug(CommandDebugCode.POTENTIAL),
            new CommandStart("dummy", "dummy", 1, 2, "dummy"),
            new CommandDisconnect(),
            new CommandCache(false)
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
      key.attach(new ConnectionContext(this, key, true));
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
        if (!disconnecting) {
          jobs.processContextQueue();
          jobs.processTaskExecutorQueue();
          processCommands();
        }
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
        ((Context) key.attachment()).doConnect();
      }
      if (key.isValid() && key.isWritable()) {
        ((Context) key.attachment()).doWrite();
      }
      if (key.isValid() && key.isReadable()) {
        ((Context) key.attachment()).doRead();
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
    var context = new ConnectionContext(this, clientKey, false);
    clientKey.attach(context);

    // Reroute jobs if needed
    // TODO


    // Potential management
    context.queuePacket(new InitPacket(potential, appID));
    reevaluatePotential();
    availableNodesStream().forEach(ctx -> {
      if (ctx.key() != clientKey) {
        ctx.queuePacket(new UpdtPacket(potential - ctx.potential(), appID));
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
   * Reroutes jobs attributed to a certain appID to a new context
   * @param appID id of the app the jobs are attached to
   * @param context context to switch the job to
   */
  public void rerouteJobs(int appID, ConnectionContext context) {
    var jobsToReplace = upstreamHostsToreplace.get(appID);
    if (jobsToReplace != null) {
      jobsToReplace.forEach(id -> {
        jobs.swapUpstreamHost(id, context);
        logger.info("Rerouted job " + id + " to app " + appID);
      });
      upstreamHostsToreplace.remove(appID);
    }
  }

  /**
   * Returns a stream of all connected nodes that are available (i.e. not disconnecting)
   * @return stream of all connected nodes
   */
  public Stream<ConnectionContext> availableNodesStream() {
    return selector.keys().stream().map(SelectionKey::attachment)
        .mapMulti((a, consumer) -> {
          if (a instanceof ConnectionContext ctx && !ctx.isUnavailableForAnswerPackets()) { consumer.accept((ConnectionContext) a); }
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
   * Returns appID of the app
   * @return appID of the app
   */
  public int appID() { return appID; }

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
    Objects.requireNonNull(incomingHost);
    reevaluatePotential();
    availableNodesStream().forEach(ctx -> {
      if (ctx.key() != incomingHost) {
        ctx.queuePacket(new UpdtPacket(potential - ctx.potential(), appID));
      }
    });
  }

  /**
   * Transmits a packet from a context to the job manager
   * @param packet packet to transmit
   */
  public void transmitPacketToJobs(Packet packet) {
    Objects.requireNonNull(packet);
    jobs.queueContextPacket(packet);
  }

  public void processRequestPacket(ReqPacket packet, ConnectionContext context) throws IOException {
    Objects.requireNonNull(packet);
    Objects.requireNonNull(context);
    jobs.processReqPacket(packet, context);
  }

  private void broadcastDisconnection() {
    int nbReco = (int) availableNodesStream().count() - 1;
    var jobsUpstreamOfParent = jobs.getJobsUpstreamOfNode(parentKey);

    // Send ref packets for parts of jobs it had accepted to do
    jobs.cancelAllOngoingDownstreamWork();

    // Send disc packet to parent, redi to others
    availableNodesStream().forEach(ctx -> {
      if (ctx.key() == parentKey) {
        var innerDiskPacketSize = jobsUpstreamOfParent.size();
        var innerDiskPackets = new DiscPacket.InnerDiscPacket[innerDiskPacketSize];
        for (var i = 0 ; i < innerDiskPacketSize ; i++) {
          var job = jobsUpstreamOfParent.get(i);
          innerDiskPackets[i] = new DiscPacket.InnerDiscPacket(job.jobID(), job.getUpstreamContext().remoteAppId());
        }
        ctx.queuePacket(new DiscPacket(nbReco, innerDiskPacketSize, innerDiskPackets));
      } else {
        ctx.queuePacket(new RediPacket(parentAddress));
      }
    });
    disconnecting = true;
    disconnectionCounter = (int) availableNodesStream().count();
  }

  /**
   * Processes the reception of an OK_DISC packet from one of the neighbors
   */
  public void processOkDisc() {
    disconnectionCounter--;
    if (disconnectionCounter == 0) {
      // Shutdown server
      selector.keys().forEach(this::silentlyClose);
      Thread.currentThread().interrupt();
      System.exit(0);
    }
  }

  /**
   * Does what's necessary to reconnect the network after the disconnecting node has gone
   * @param disconnectionPacket that was received by the disconnecting node, should be a RediPacket or a
   *                            DiscPacket
   */
  public void reconnect(Packet disconnectionPacket) throws IOException {
    Objects.requireNonNull(disconnectionPacket);
    switch (disconnectionPacket) {
      case DiscPacket discPacket -> {
        for (var innerDiskPacket : discPacket.jobs()) {
          upstreamHostsToreplace.computeIfAbsent(innerDiskPacket.new_upstream(), k -> new ArrayList<>())
              .add(innerDiskPacket.job_id());
        }
      }
      case RediPacket rediPacket -> {
        // Get local address to reuse it so that this node can be identified properly by the new parent
        var oldAddress = parentSocketChannel.getLocalAddress();
        parentSocketChannel = SocketChannel.open();
        parentSocketChannel.bind(oldAddress);
        parentSocketChannel.configureBlocking(false);
        var key = parentSocketChannel.register(selector, SelectionKey.OP_CONNECT);
        key.attach(new ConnectionContext(this, key, true));
        parentSocketChannel.connect(rediPacket.new_parent());

        jobs.swapUpstreamHost(parentKey, key);
        parentKey = key;
        parentAddress = rediPacket.new_parent();
        logger.info("Connecting to new parent...");
      }
      default -> throw new IllegalArgumentException();
    }
  }

  /**
   * Starts the download of a JAR file
   * @param jarURL URL of the jar
   * @param job job which asked for the download
   * @throws IOException
   */
  public void downloadJar(String jarURL, Job job) throws IOException {
    try {
      URL url = new URL(jarURL);
      var socketChannel = SocketChannel.open();
      socketChannel.configureBlocking(false);
      var key = socketChannel.register(selector, SelectionKey.OP_CONNECT);
      key.attach(new HttpContext(key, jarURL, jarPath, job));
      socketChannel.connect(new InetSocketAddress(url.getHost(), 80));
    } catch (MalformedURLException | UnresolvedAddressException ignored) {
      logger.warning(jarURL + " is an invalid URL");
      job.jarDownloadFail();
    }
  }

  public boolean useCache() {
    return this.useCache;
  }
}
