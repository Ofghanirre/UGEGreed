package fr.uge.ugegreed;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Entry point for the UGE Greed application
 */
public final class Main {
  private static final Logger logger = Logger.getLogger(Main.class.getSimpleName());
  private static int errorLogged;
  private static void usage() {
    System.out.println("Usage : UGEGreed port resultPath [ip port] [-t [ThreadAmount]]");
  }
  public static void main(String[] args) throws IOException {
    if (args.length < 2 || args.length > 6) {
      usage();
      return;
    }

    logger.info("Application start.");
    int listenPort = -1;
    try {
      listenPort = Integer.parseInt(args[0]);
      if (listenPort < 0 || listenPort > 65535) {
        logger.warning("port out of range:" + listenPort);
        errorLogged += 1;
      }
    } catch (NumberFormatException e) {
      logger.warning("The given port : " + args[0] + " is invalid");
      errorLogged += 1;
    }

    Path resultPath = null;
    try {
      resultPath = Path.of(args[1]);
      if (!resultPath.toFile().exists()) {
        logger.info("Result directory does not exists, creating it...");
        Files.createDirectories(resultPath);
      }
    } catch (InvalidPathException e) {
      logger.warning("The given result_path \"" + args[1] + "\" could not been resolved as a correct Path");
      errorLogged += 1;
    }
    InetSocketAddress parentAddress = null;

    for (int i = 2; i < args.length; i++) {
      if (args[i].equals("-t")) {
        if (args.length > i + 1) {
          int threadAmount = Integer.parseInt(args[i + 1]);
          TaskExecutor.setThreadAmount(threadAmount);
          logger.info("Set Thread Amount to " + TaskExecutor.getThreadAmount());
          i++;
        } else {
          logger.info("Thread Default Amount is " + TaskExecutor.getThreadAmount() + ", you can set it up by inputing a number after the -t option");
        }
      }
      else if (args.length - i > 2) {
        try {
          parentAddress = new InetSocketAddress(args[i], Integer.parseInt(args[i+1]));
          i++;
        } catch (IllegalArgumentException e) {
          logger.warning(e.getMessage());
          errorLogged += 1;
        }
      }
    }


    if (errorLogged != 0) {
      logger.warning(errorLogged + ((errorLogged > 1) ? " errors were" : " error was") + " found while starting the Application, please fix them and restart the application");
    } else {
      new Controller(listenPort, resultPath, parentAddress).launch();
    }

    logger.info("Application closed.");
  }
}