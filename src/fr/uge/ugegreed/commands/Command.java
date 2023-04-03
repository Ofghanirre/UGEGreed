package fr.uge.ugegreed.commands;

/**
 * Represents console commands
 */
public sealed interface Command permits CommandDebug, CommandDisconnect, CommandStart {
}
