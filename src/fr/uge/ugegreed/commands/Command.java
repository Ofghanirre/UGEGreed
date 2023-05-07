package fr.uge.ugegreed.commands;

/**
 * Represents console commands
 */
public sealed interface Command permits CommandCache, CommandDebug, CommandDisconnect, CommandHelp, CommandStart {
    CommandHelpData getHelp();
    String getName();
}
