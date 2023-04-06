package fr.uge.ugegreed.commands;

public record CommandHelp() implements Command {
    @Override
    public CommandHelpData getHelp() {
        return new CommandHelpData("HELP", new CommandHelpData.Argument[]{}, "Help command to list all possibles commands and their usages");
    }

    @Override
    public String getName() {
        return "HELP";
    }
}
