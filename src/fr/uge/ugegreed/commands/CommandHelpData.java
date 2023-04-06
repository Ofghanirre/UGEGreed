package fr.uge.ugegreed.commands;

import java.util.Arrays;
import java.util.stream.Collectors;

public record CommandHelpData(String name, Argument[] arguments, String description) {
    public record Argument(String name, String description) {
        @Override
        public String toString() {
            return "\t - " + name + " -> " + description;
        }
    }

    @Override
    public String toString() {
        String result = "COMMAND -*- " + name + "\n" +
                "USAGE:\n\t" + name + " " + Arrays.stream(arguments).map(Argument::name).collect(Collectors.joining(" ")) + "\n";
        if (arguments.length != 0) {
            result += "PARAMETERS :\n" +
                    Arrays.stream(arguments).map(Argument::toString).collect(Collectors.joining("\n")) + "\n";
        }

        result += "DESCRIPTION :\n\t" +
                description;

        return result;
    }
}
