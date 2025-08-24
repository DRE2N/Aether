package de.erethon.aether.commands;

import de.erethon.aether.Aether;
import de.erethon.bedrock.command.ECommand;
import org.bukkit.command.CommandSender;

public class DebugCommand extends ECommand {

    public DebugCommand() {
        setCommand("debug");
        setAliases("d");
        setMinArgs(0);
        setMaxArgs(0);
        setPlayerCommand(true);
        setHelp("Help.");
        setPermission("aether.debug");
    }

    @Override
    public void onExecute(String[] strings, CommandSender commandSender) {
        Aether.AETHER_DEBUG_MODE = !Aether.AETHER_DEBUG_MODE;
        commandSender.sendMessage("Debug mode: " + (Aether.AETHER_DEBUG_MODE ? "enabled" : "disabled"));
    }
}
