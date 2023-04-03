package fr.uge.ugegreed.commands;

import java.util.Objects;

/**
 * Represents a console command to start a job
 * @param urlJAR url to the JAR containing the code to run
 * @param className name of the class implementing the Checker interface
 * @param rangeStart number to start at (included)
 * @param rangeEnd number to end at (excluded)
 * @param filename name of the file where to store the results
 */
public record CommandStart(String urlJAR, String className, long rangeStart, long rangeEnd, String filename) implements Command {
  public CommandStart {
    Objects.requireNonNull(urlJAR);
    Objects.requireNonNull(className);
    Objects.requireNonNull(filename);
    if (rangeEnd < rangeStart) {
      throw new IllegalArgumentException("rangeEnd must be >= rangeStart");
    }
    if (urlJAR.isEmpty()) {
      throw new IllegalArgumentException("the JAR url cannot be empty");
    }
    if (className.isEmpty()) {
      throw new IllegalArgumentException("the class name cannot be empty");
    }
    if (filename.isEmpty()) {
      throw new IllegalArgumentException("the filename cannot be empty");
    }
  }
}
