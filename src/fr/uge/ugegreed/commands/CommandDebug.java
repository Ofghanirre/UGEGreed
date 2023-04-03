package fr.uge.ugegreed.commands;

/**
 * Represents a debug console command
 */
public record CommandDebug(CommandDebugCode debugCode) implements Command {
    @Override
    public CommandHelpData getHelp() {
        return new CommandHelpData("DEBUG", new CommandHelpData.Argument[]{new CommandHelpData.Argument("code", "code for command : 1 : Potential")}, "Debug commands used to test code");
    }

    @Override
    public String getName() {
        return "DEBUG";
    }
}
