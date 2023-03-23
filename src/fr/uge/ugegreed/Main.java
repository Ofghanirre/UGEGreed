package fr.uge.ugegreed;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.logging.Logger;

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
    var resultPath = Path.of(args[1]);

    if (args.length == 4) {
      var parentAddress = new InetSocketAddress(args[2], Integer.parseInt(args[3]));
      new Controller(listenPort, resultPath, parentAddress).launch();
    } else {
      new Controller(listenPort, resultPath).launch();
    }

    logger.info("Application closed.");
  }
}
