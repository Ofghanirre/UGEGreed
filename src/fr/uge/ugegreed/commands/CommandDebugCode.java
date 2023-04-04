package fr.uge.ugegreed.commands;

import java.util.Optional;

public enum CommandDebugCode {
    POTENTIAL(1);

    private final int intValue;

    CommandDebugCode(int value) {
     this.intValue = value;
    }

    int getIntValue() {
        return intValue;
    }
    public static Optional<CommandDebugCode> fromInt(int intValue) {
        for (var value : CommandDebugCode.values()) {
            if (value.getIntValue() == intValue) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}
