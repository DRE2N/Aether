package de.erethon.aether.commands;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.destroystokyo.paper.entity.ai.*;
import de.erethon.commons.chat.MessageUtil;
import de.erethon.commons.command.DRECommand;
import de.erethon.aether.Aether;
import de.erethon.aether.groups.FormationDirection;
import de.erethon.aether.groups.FormationTools;
import de.erethon.aether.creature.ActiveNPC;
import de.erethon.aether.tools.hitbox.RayTraceUtils;
import net.minecraft.server.v1_16_R3.EntityPose;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftEntity;
import org.bukkit.entity.*;
import org.bukkit.event.Listener;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class TestCommand extends DRECommand implements Listener {
    Aether plugin = Aether.getInstance();
    ProtocolManager protocol = ProtocolLibrary.getProtocolManager();
    Map<UUID, BoundingBox> boundingBoxMap = new HashMap<>();

    public TestCommand() {
        setCommand("test");
        setAliases("t");
        setMinArgs(-1);
        setMaxArgs(-1);
        setPlayerCommand(true);
        setHelp("Help.");
        setPermission("mxl.test");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        protocol.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.ARM_ANIMATION) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                runSyncEntityCheck(event.getPlayer());
            }
        });
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        Player player = (Player) commandSender;
        World world = player.getWorld();
        if (args[1].equals("hide")) {
            plugin.getNpcInstancing().addInstanced(player.getTargetEntity(10).getUniqueId());
            plugin.getNpcInstancing().hide(player, player.getTargetEntity(10).getUniqueId());
            return;
        }
        if (args[1].equals("show")) {
            plugin.getNpcInstancing().show(player, player.getTargetEntity(10).getUniqueId());
            return;
        }
        if (args[1].equals("text")) {
            ActiveNPC activeNPC = null;
            String desc = new String();
            int i = 2;
            for (String arg : args) {
                if (args[0] != arg && args[i - 1] != arg) {
                    if (!desc.isEmpty()) {
                        desc += " ";
                    }
                    desc += arg;
                }
            }
            activeNPC.displayTextAboveHead(player, desc, 6, true);
            return;
        }
        if (args[1].equals("box")) {
            Location location = player.getLocation();
            EntityType type = EntityType.valueOf(args[2]);
            CraftEntity entity = (CraftEntity) world.spawnEntity(location, EntityType.PIG);
            entity.setCustomName("bb");
            BoundingBox box = RayTraceUtils.getHitboxFromEntity(new Location(world, 0, 0, 0), EntityType.WITHER, EntityPose.STANDING);
            boundingBoxMap.put(entity.getUniqueId(), box);
            return;
        }
        if (args[1].contains("walk")) {
            MessageUtil.sendMessage(player, "Testing walk");
            List<Mob> mobs = new CopyOnWriteArrayList<>();
            for (Entity entity : world.getNearbyEntities(player.getLocation(), 30, 10, 30)) {
                if (!(entity instanceof Mob)) {
                    continue;
                }
                if (entity.getType() != EntityType.PIG) {
                    return;
                }
                Mob mob = (Mob) entity;
                mobs.add(mob);
            }
            List<Location> locations = FormationTools.getLine(player.getTargetBlock(10).getLocation(), FormationDirection.X, mobs.size(), 2);
            MessageUtil.sendMessage(player, "Found " + mobs.size() + " mobs and created " + locations.size() + " points.");
            for (Location location : locations) {
                if (mobs.isEmpty()) {
                    return;
                }
                Mob mob = mobs.get(mobs.size() - 1);
                mob.setCollidable(false);
                mob.setCustomName("Moving to " + location.getBlockX() + "/" + location.getBlockZ());
                PaperMobGoals goals = new PaperMobGoals();
                GoalKey<Creature> key = VanillaGoal.RANDOM_STROLL;
                GoalKey<Creature> key1 = VanillaGoal.RANDOM_STROLL_LAND;
                Pig pig = (Pig) mob;
                goals.removeGoal(pig, key);
                goals.removeAllGoals(pig);
                mob.teleport(location);
                world.spawnParticle(Particle.VILLAGER_HAPPY, location, 1);
                mobs.remove(mob);
            }
            return;
        }
        for (Location location : FormationTools.getSquare(player.getLocation(), FormationDirection.X, Integer.parseInt(args[1]), Integer.parseInt(args[2]), 1, 2)) {
            plugin.getNpcManager().createNPC(player, location, "Test");
        }
        MessageUtil.sendMessage(player, "Spawned test formation.");
    }

    /*public void entityMove(EntityMoveEvent event) {
        MessageUtil.broadcastMessage("Entity moved " + event.getEntityType());
        if (event.getEntityType() == EntityType.PIG) {
            boundingBoxMap.get(event.getEntity().getUniqueId()).shift(event.getTo());
        }
    }*/

    // EHH PERFORMANCE
    public void runSyncEntityCheck(Player player) {
        Vector playerVector = player.getLocation().getDirection().clone().multiply(10);
        player.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, playerVector.toLocation(player.getWorld()), 5);
        for (Map.Entry<UUID, BoundingBox> box : boundingBoxMap.entrySet()) {
            MessageUtil.log(box.toString());
            if (box.getValue().contains(playerVector)) {
                player.getWorld().getEntity(box.getKey()).setCustomName("HIT");
            }
        }
    }
}
