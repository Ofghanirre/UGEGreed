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

  @Override
  public CommandHelpData getHelp() {
    return new CommandHelpData("START",
            new CommandHelpData.Argument[]{
                    new CommandHelpData.Argument("url-jar", "jar url"),
                    new CommandHelpData.Argument("fully-qualified-name", "the fully qualified name of the class contained in the jar implementing the interface fr.uge.ugegreed.Checker"),
                    new CommandHelpData.Argument("start-range", "the first value to test"),
                    new CommandHelpData.Argument("end-range", "the last value to test"),
                    new CommandHelpData.Argument("filename ", "the name of the output file to store result in"),
            }, "Start the checking of a given conjecture");
  }

  @Override
  public String getName() {
    return "START";
  }
}
