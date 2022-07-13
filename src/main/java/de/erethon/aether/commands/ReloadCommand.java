package de.erethon.aether.commands;

import de.erethon.aether.Aether;
import de.erethon.aether.creature.ActiveNPC;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import org.bukkit.command.CommandSender;

public class ReloadCommand extends ECommand {

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
            try { // sometimes NPCs get unloaded by the server during the reload
                activeNPC.setProperties();
            } catch (Exception ignored) {}
        }
        Aether.getInstance().getSpawnerManager().reloadSpawners();
        MessageUtil.sendMessage(commandSender, "&aReload complete!");
    }
}
