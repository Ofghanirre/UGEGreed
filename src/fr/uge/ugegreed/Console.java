package fr.uge.ugegreed;

import fr.uge.ugegreed.commands.CommandDebug;
import fr.uge.ugegreed.commands.CommandDisconnect;
import fr.uge.ugegreed.commands.CommandStart;

import java.util.Objects;
import java.util.Scanner;
import java.util.logging.Logger;

/**
 * Manages the console
 */
public class Console {
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
        var result = switch (splitLine[0]) {
          case "START" -> sendStartCommand(splitLine);
          case "DISCONNECT" -> sendDisconnectCommand();
          case "DEBUG" -> sendDebugCommand(splitLine);
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
      controller.sendCommand(new CommandDebug(debugCode));
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }
}
