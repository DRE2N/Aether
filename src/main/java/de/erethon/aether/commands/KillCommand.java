package de.erethon.aether.commands;

import de.erethon.aether.Aether;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class KillCommand extends ECommand {

    public KillCommand() {
        setCommand("kill");
        setAliases("k");
        setMinArgs(0);
        setMaxArgs(1);
        setPermission("aether.kill");
        setDescription("Kills a mob");
        setPlayerCommand(true);
        setConsoleCommand(false);
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        Player player = (Player) commandSender;
        if (args.length >= 2) {
            Location location = player.getLocation();
            int radius = Integer.parseInt(args[1]);
            int killed = 0;
            for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                if (entity.getPersistentDataContainer().has(Aether.getInstance().getKey())) {
                    entity.remove();
                    killed++;
                }
            }
            MessageUtil.sendMessage(player, "<green>Killed " + killed + " entities in a " + radius + " block radius.");
            return;
        }
        Entity target = player.getTargetEntity(16);
        if (target == null) {
            MessageUtil.sendMessage(player, "<red>No target found.");
            return;
        }
        if (!target.getPersistentDataContainer().has(Aether.getInstance().getKey())) {
            MessageUtil.sendMessage(player, "<red>Target is not an Aether entity.");
            return;
        }
        target.remove();
        MessageUtil.sendMessage(player, "<green>Killed entity.");
    }
}
