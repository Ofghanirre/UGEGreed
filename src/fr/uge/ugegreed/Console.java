package fr.uge.ugegreed;

import fr.uge.ugegreed.commands.*;

import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;
import java.util.logging.Logger;

/**
 * Manages the console
 */
public final class Console {
  private static final Logger logger = Logger.getLogger(Console.class.getName());
  private final Controller controller;

  /**
   * Creates a new console for the given controller
   * @param controller controller running in the main thread
   */
  public Console(Controller controller) {
    Objects.requireNonNull(controller);
    this.controller = controller;
  }

  /**
   * Runs the console
   */
  public void consoleRun() {
    try {
      var scan = new Scanner(System.in);
      while (scan.hasNextLine()) {
        var line = scan.nextLine();
        var splitLine = line.split(" +");
        if (splitLine.length == 0) continue;
        boolean result = switch (splitLine[0].toUpperCase(Locale.ROOT)) {
          case "START" -> sendStartCommand(splitLine);
          case "DISCONNECT" -> sendDisconnectCommand();
          case "DEBUG" -> sendDebugCommand(splitLine);
          case "HELP" -> sendHelpCommand();
          case "CACHE" -> sendCacheCommand(splitLine);
          default -> false;
        };
        if (!result) {
          System.out.println("Invalid command: " + line);
        }
      }
    } catch (InterruptedException e) {
      logger.info("Console thread has been interrupted");
    } finally {
      logger.info("Console thread stopping");
    }
  }

  private boolean sendCacheCommand(String[] splitLine) throws InterruptedException {
    if (splitLine.length != 2) {
      return false;
    }
    controller.sendCommand(new CommandCache(Boolean.parseBoolean(splitLine[1])));
    return true;
  }

  private boolean sendStartCommand(String[] splitLine) throws InterruptedException {
    if (splitLine.length != 6) {
      return false;
    }
    try {
      var start = Integer.parseInt(splitLine[3]);
      var end = Integer.parseInt(splitLine[4]);
      if (end < start) { return false; }
      controller.sendCommand(new CommandStart(splitLine[1], splitLine[2], start, end, splitLine[5]));
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private boolean sendDisconnectCommand() throws InterruptedException {
    controller.sendCommand(new CommandDisconnect());
    return true;
  }

  private boolean sendDebugCommand(String[] splitLine) throws InterruptedException {
    if (splitLine.length != 2) {
      return false;
    }
    try {
      var debugCode = Integer.parseInt(splitLine[1]);
      if (debugCode < 0) { return false; }
      var optionalDebugCode = CommandDebugCode.fromInt(debugCode);
      if (optionalDebugCode.isEmpty()) return false;
      controller.sendCommand(new CommandDebug(optionalDebugCode.get()));
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private boolean sendHelpCommand() throws InterruptedException {
    controller.sendCommand(new CommandHelp());
    return true;
  }
}
