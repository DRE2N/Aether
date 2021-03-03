package de.erethon.mobsxl.commands;

import com.destroystokyo.paper.PaperCommand;
import com.destroystokyo.paper.entity.ai.*;
import de.erethon.commons.chat.MessageUtil;
import de.erethon.commons.command.DRECommand;
import de.erethon.mobsxl.MobsXL;
import de.erethon.mobsxl.groups.FormationDirection;
import de.erethon.mobsxl.groups.FormationTools;
import net.minecraft.server.v1_16_R3.PathfinderGoal;
import net.minecraft.server.v1_16_R3.PathfinderGoalRandomStroll;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class TestCommand extends DRECommand {

    MobsXL plugin = MobsXL.getInstance();

    public TestCommand() {
        setCommand("test");
        setAliases("t");
        setMinArgs(0);
        setMaxArgs(4);
        setPlayerCommand(true);
        setHelp("Help.");
        setPermission("mxl.test");
    }

    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        Player player = (Player) commandSender;
        World world = player.getWorld();
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
}
