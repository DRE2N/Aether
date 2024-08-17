package de.erethon.aether.commands;

import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.PaperMobGoals;
import com.destroystokyo.paper.entity.ai.VanillaGoal;
import de.erethon.aether.Aether;
import de.erethon.aether.creature.ActiveNPC;
import de.erethon.aether.creature.AetherModelEntity;
import de.erethon.aether.groups.FormationDirection;
import de.erethon.aether.groups.FormationTools;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.*;
import org.bukkit.event.Listener;
import org.bukkit.util.BoundingBox;
import team.unnamed.hephaestus.Model;
import team.unnamed.hephaestus.bukkit.ModelView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class TestCommand extends ECommand implements Listener {
    Aether plugin = Aether.getInstance();
    Map<UUID, BoundingBox> boundingBoxMap = new HashMap<>();

    public TestCommand() {
        setCommand("test");
        setAliases("t");
        setMinArgs(1);
        setMaxArgs(5);
        setPlayerCommand(true);
        setHelp("Help.");
        setPermission("mxl.test");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        Player player = (Player) commandSender;
        World world = player.getWorld();
        if (args[1].equals("model")) {
            Model model = plugin.getModelRegistry().model(args[2]);
            if (model == null) {
                MessageUtil.sendMessage(player, "Model not found.");
                return;
            }
            ModelView view = plugin.getModelEngine().createViewAndTrack(model, player.getLocation());
            return;
        }
        if (args[1].equals("test")) {
            Model model = plugin.getModelRegistry().model("redstone_monstrosity");
            ModelView view = plugin.getModelEngine().spawn(model, player);
            MessageUtil.sendMessage(player, "Spawned " + view);
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
            List<Location> locations = FormationTools.getLine(player.getTargetBlockExact(10).getLocation(), FormationDirection.X, mobs.size(), 2);
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
                GoalKey<Creature> key1 = VanillaGoal.WATER_AVOIDING_RANDOM_STROLL;
                Pig pig = (Pig) mob;
                goals.removeGoal(pig, key);
                goals.removeAllGoals(pig);
                mob.teleport(location);
                world.spawnParticle(Particle.HAPPY_VILLAGER, location, 1);
                mobs.remove(mob);
            }
            return;
        }
    }

    /*public void entityMove(EntityMoveEvent event) {
        MessageUtil.broadcastMessage("Entity moved " + event.getEntityType());
        if (event.getEntityType() == EntityType.PIG) {
            boundingBoxMap.get(event.getEntity().getUniqueId()).shift(event.getTo());
        }
    }*/


}
