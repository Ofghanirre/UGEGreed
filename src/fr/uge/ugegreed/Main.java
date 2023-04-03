package fr.uge.ugegreed;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Entry point for the UGE Greed application
 */
public class Main {
  private static final Logger logger = Logger.getLogger(Main.class.getSimpleName());

  private static void usage() {
    System.out.println("Usage : UGEGreed port resultPath [ip port]");
  }
  public static void main(String[] args) throws IOException {
    if (args.length != 2 && args.length != 4) {
      usage();
      return;
    }

    logger.info("Application start.");

    var listenPort = Integer.parseInt(args[0]);
    if (listenPort < 0 || listenPort > 65535) {
      logger.warning("Port given <" + listenPort + "> is invalid, exiting...");
      throw new IllegalArgumentException("Port given is invalid");
    }

    var resultPath = Path.of(args[1]);
    Files.createDirectories(resultPath);

    if (args.length == 4) {
      var parentAddress = new InetSocketAddress(args[2], Integer.parseInt(args[3]));
      new Controller(listenPort, resultPath, parentAddress).launch();
    } else {
      new Controller(listenPort, resultPath).launch();
    }

    logger.info("Application closed.");
  }
}
