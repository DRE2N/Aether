package de.erethon.aether.commands;

import de.erethon.aether.Aether;
import de.erethon.aether.creature.ActiveNPC;
import de.erethon.aether.groups.FormationDirection;
import de.erethon.aether.groups.MobGroup;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.util.BoundingBox;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class FormationCommand extends ECommand implements Listener {
    Aether plugin = Aether.getInstance();
    Map<UUID, BoundingBox> boundingBoxMap = new HashMap<>();

    private Map<Player, MobGroup> groups = new HashMap<>();

    private Map<Player, Boolean> follow = new HashMap<>();

    public FormationCommand() {
        setCommand("formation");
        setAliases("f");
        setMinArgs(1);
        setMaxArgs(4);
        setPlayerCommand(true);
        setHelp("Help.");
        setPermission("mxl.test");
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        Player player = (Player) commandSender;
        if (args[1].equals("create")) {
            MobGroup group = new MobGroup();
            player.getLocation().getNearbyLivingEntities(64).forEach(e -> {
               if (plugin.getActiveCreatureManager().get(e.getUniqueId()) != null) {
                   group.add(plugin.getActiveCreatureManager().get(e.getUniqueId()));
               }
            });
            groups.put(player, group);
            MessageUtil.sendMessage(player, "Created group with " + group.getNpcs().size() + " members.");
            return;
        }
        if (args[1].equals("square")) {
            if (groups.get(player) == null) {
                MessageUtil.sendMessage(player, "You have no group.");
                return;
            }
            if (args.length < 4) {
                MessageUtil.sendMessage(player, "Usage: /ae f square <width> <length> <spacing>");
                return;
            }
            int width = Integer.parseInt(args[2]);
            int length = Integer.parseInt(args[3]);
            double spacing = Double.parseDouble(args[4]);
            groups.get(player).makeSquare(player.getTargetBlockExact(64).getLocation(), width, length, spacing, spacing, FormationDirection.X);
            return;
        }
        if (args[1].equals("follow")) {
            if (groups.get(player) == null) {
                MessageUtil.sendMessage(player, "You have no group.");
                return;
            }
            if (args.length < 4) {
                MessageUtil.sendMessage(player, "Usage: /ae f follow <width> <length> <spacing>");
                return;
            }
            int width = Integer.parseInt(args[2]);
            int length = Integer.parseInt(args[3]);
            double spacing = Double.parseDouble(args[4]);
            groups.get(player).makeFollowInFormation(player, width, length, spacing, spacing);
            follow.put(player, true);
            return;
        }
        if (args[1].equals("stop")) {
            if (groups.get(player) == null) {
                MessageUtil.sendMessage(player, "You have no group.");
                return;
            }
            groups.get(player).stopFollowInFormation();
            follow.put(player, false);
            groups.get(player).stopAttack();
            return;
        }
        if (args[1].equals("attack")) {
            if (groups.get(player) == null) {
                MessageUtil.sendMessage(player, "You have no group.");
                return;
            }
            if (player.getTargetEntity(64) == null) {
                MessageUtil.sendMessage(player, "You have no target.");
                return;
            }
            groups.get(player).makeAttack((LivingEntity) player.getTargetEntity(64));
        }
        if (args[1].equals("shoot")) {
            if (groups.get(player) == null) {
                MessageUtil.sendMessage(player, "You have no group.");
                return;
            }
            groups.get(player).shootArrowsAt(player.getLocation().getDirection());
        }
    }
}
