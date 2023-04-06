package fr.uge.ugegreed.commands;

/**
 * Represents a console command to disconnect the node
 */
public record CommandDisconnect() implements Command {
    @Override
    public CommandHelpData getHelp() {
        return new CommandHelpData("DISCONNECT", new CommandHelpData.Argument[]{}, "Initiate the disconnection of the network and the application stop");
    }

    @Override
    public String getName() {
        return "DISCONNECT";
    }
}
