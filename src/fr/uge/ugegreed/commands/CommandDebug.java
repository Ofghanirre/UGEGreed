package fr.uge.ugegreed.commands;

/**
 * Represents a debug console command
 */
public record CommandDebug(int debugCode) implements Command {
  public CommandDebug {
    if (debugCode < 0) {
      throw new IllegalArgumentException("debug code must be positive or null");
    }
  }
}
