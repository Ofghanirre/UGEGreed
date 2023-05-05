package fr.uge.ugegreed.commands;

public record CommandCache(boolean useCache) implements Command {
    @Override
    public CommandHelpData getHelp() {
        return new CommandHelpData("CACHE",
                new CommandHelpData.Argument[]{
                        new CommandHelpData.Argument("use-cache", "wether or not to use cached file if they exists for the requests"),
                }, "Enable/Disable the usage of cached file");
    }

    @Override
    public String getName() {
        return "CACHE";
    }
}
