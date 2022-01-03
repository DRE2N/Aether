package de.erethon.aether.commands;

import de.erethon.aether.Aether;
import de.erethon.aether.creature.ActiveNPC;
import de.erethon.commons.chat.MessageUtil;
import de.erethon.commons.command.DRECommand;
import org.bukkit.command.CommandSender;

public class ReloadCommand extends DRECommand {

    public ReloadCommand() {
        setCommand("reload");
        setAliases("r");
        setMinArgs(0);
        setMaxArgs(0);
        setPlayerCommand(true);
        setHelp("Help.");
        setPermission("aether.reload");
    }

    @Override
    public void onExecute(String[] strings, CommandSender commandSender) {
        MessageUtil.sendMessage(commandSender, "&aReloading...");
        Aether.getInstance().getCreatureManager().reload();
        for (ActiveNPC activeNPC : Aether.getInstance().getActiveCreatureManager().getGlobalNPCs().values()) {
            activeNPC.setProperties();
        }
        MessageUtil.sendMessage(commandSender, "&aReload complete!");
    }
}
