package fr.uge.ugegreed.commands;

public enum CommandDebugCode {
    POTENTIAL(1);

    private final int intValue;

    CommandDebugCode(int value) {
     this.intValue = value;
    }

    int getIntValue() {
        return intValue;
    }
    public static CommandDebugCode fromInt(int intValue) {
        for (var value : CommandDebugCode.values()) {
            if (value.getIntValue() == intValue) {
                return value;
            }
        }
        throw new IllegalArgumentException("Invalid int value: " + intValue);
    }
}
