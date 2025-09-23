package de.erethon.aether.qxl.actions;

import de.erethon.aether.Aether;
import de.erethon.aether.creature.AetherBaseMob;
import de.erethon.aether.creature.CreatureManager;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.action.QBaseAction;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLocation;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.error.FriendlyError;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;

public class MobWalkAction extends QBaseAction {

    Aether aether = Aether.getInstance();
    CreatureManager creatureManager = aether.getCreatureManager();

    @QParamDoc(name = "tag" , description = "Tag the spawned mob with an ID to be able to reference it later")
    String tag = null;
    @QParamDoc(name = "location", description = "The QLocation to walk to", required = true)
    QLocation location = null;
    @QParamDoc(name = "moveHome", description = "Should the homing location be updated to the new location? Default: false", required = false)
    boolean moveHome = false;


    @Override
    public void play(Quester quester) {
        if (!conditions(quester)) return;
        try {
            AetherBaseMob mob = creatureManager.getTaggedMob(tag);
            if (mob == null) {
                throw new IllegalArgumentException("No mob with tag " + tag + " found.");
            }
            // Remove the random walk goal to prevent it from interfering
            Set<Goal> toRemove = new HashSet<>();
            for (WrappedGoal goal : mob.goalSelector.getAvailableGoals()) {
                if (goal.getGoal() instanceof RandomStrollGoal || goal.getGoal() instanceof RandomSwimmingGoal) {
                    toRemove.add(goal.getGoal());
                    break;
                }
            }
            toRemove.forEach(goal -> mob.goalSelector.removeGoal(goal));
            mob.getMoveControl().setWantedPosition(location.getX(quester.getLocation()), location.getY(quester.getLocation()), location.getZ(quester.getLocation()), 1);
            if (moveHome) {
                BlockPos home = new BlockPos((int) location.getX(quester.getLocation()), (int) location.getY(quester.getLocation()), (int) location.getZ(quester.getLocation()));
                mob.setHomeTo(home, 8);
            }
            // Check if mob reached destination
            Bukkit.getScheduler().runTaskTimer(aether, () -> {
                double distance = mob.distanceToSqr(location.getX(quester.getLocation()), location.getY(quester.getLocation()), location.getZ(quester.getLocation()));
                if (distance < 4) {
                    onFinish(quester);
                    // Re-add the random walk goal
                    toRemove.forEach(goal -> mob.goalSelector.addGoal(1, goal));
                    cancel();
                } else {
                    mob.getMoveControl().setWantedPosition(location.getX(quester.getLocation()), location.getY(quester.getLocation()), location.getZ(quester.getLocation()), 1);
                }
            }, 20, 20);
        }
        catch (Exception e) {
            FriendlyError error = new FriendlyError(id,"Failed to walk mob", e.getMessage(), "Mob tag: " + tag).addStacktrace(e.getStackTrace());
            QuestsXL.get().addRuntimeError(error);
        }
        onFinish(quester);
    }


    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        tag = cfg.getString("tag", null);
        location = cfg.getQLocation("location");
        moveHome = cfg.getBoolean("moveHome", false);
        if (tag == null) {
            throw new IllegalArgumentException("Tag cannot be null.");
        }
    }
}
