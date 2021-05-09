package de.erethon.aether.commands;

import de.erethon.aether.Aether;
import de.erethon.aether.creature.ActiveNPC;
import de.erethon.aether.creature.NPC;
import de.erethon.commons.chat.MessageUtil;
import de.erethon.commons.command.DRECommand;
import de.erethon.questsxl.QuestsXL;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class SpawnCommand extends DRECommand {

    public SpawnCommand() {
        setCommand("spawn");
        setAliases("s");
        setMinArgs(0);
        setMaxArgs(3);
        setPlayerCommand(true);
        setHelp("Help.");
        setPermission("mxl.spawn");
    }
    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        Player player = (Player) commandSender;
        NPC npc = Aether.getInstance().getCreatureManager().getByID(args[1]);
        if (npc == null) {
            MessageUtil.sendMessage(player, "&cDer NPC " + args[1] + " &cexistiert nicht.");
            return;
        }
        for (int i = 0; i <= Integer.parseInt(args[2]); i++) {
            ActiveNPC activeNPC = new ActiveNPC(npc);
            activeNPC.spawn(player.getLocation());
        }
    }
}
