package de.erethon.aether.groups;

import de.erethon.aether.creature.ActiveNPC;
import net.minecraft.world.entity.PathfinderMob;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MobGroup {
    private List<ActiveNPC> npcs = new ArrayList<>();
    private BukkitRunnable followTask;
    private BukkitRunnable arrowShootTask;
    private List<ActiveNPC> toShoot;

    private boolean follow = false;

    public void makeSquare(Location center, int width, int length, double spacingWidth, double spacingLength, FormationDirection direction) {
        List<Location> locations = FormationTools.getSquare(center, direction, width, length, spacingWidth, spacingLength);
        for (int i = 0; i < npcs.size(); i++) {
            npcs.get(i).getNMSMob().removeFreeWill();
            npcs.get(i).getNMSMob().collides = false;
            if (i < locations.size()) {
                npcs.get(i).getNMSMob().getNavigation().moveTo(locations.get(i).getX(), locations.get(i).getY(), locations.get(i).getZ(), 1.5);
            }
        }
    }

    public void makeFollowInFormation(Player player, int width, int length, double spacingWidth, double spacingLength) {
        if (follow) {
            followTask.cancel();
        }
        follow = true;
        followTask = new BukkitRunnable() {
            @Override
            public void run() {
                makeSquare(player.getLocation(), width, length, spacingWidth, spacingLength, FormationDirection.X);
            }
        };
        followTask.runTaskTimer(de.erethon.aether.Aether.getInstance(), 0, 5);
    }

    public void stopFollowInFormation() {
        followTask.cancel();
        follow = false;
    }

    public void makeAttack(LivingEntity target) {
        net.minecraft.world.entity.LivingEntity nmsTarget = ((CraftLivingEntity) target).getHandle();
        for (ActiveNPC npc : npcs) {
            npc.getNMSMob().goalSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal((PathfinderMob) nmsTarget, 1.7, true));
            npc.getNMSMob().goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal((PathfinderMob) nmsTarget, 1.7, 64));
            npc.getNMSMob().setTarget(((CraftLivingEntity) target).getHandle());
        }
    }

    public void shootArrowsAt(Vector dir) {
        toShoot = new ArrayList<>(npcs);
        arrowShootTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (toShoot.isEmpty()) {
                    arrowShootTask.cancel();
                    return;
                }
                shootArrowAt(dir, toShoot.iterator().next());
                toShoot.remove(toShoot.iterator().next());
            }
        };
        //arrowShootTask.runTaskTimer(de.erethon.aether.Aether.getInstance(), 0, 1); we got an infinite loop here somewhere
    }

    private void shootArrowAt(Vector dir, ActiveNPC next) {
        if (next.getBaseEntity() instanceof ProjectileSource source) {
            source.launchProjectile(Arrow.class, dir.multiply(2));
        }
    }

    public void stopAttack() {
        for (ActiveNPC npc : npcs) {
            npc.getNMSMob().removeFreeWill();
            npc.getNMSMob().setTarget(null);
        }
    }

    public void add(ActiveNPC npc) {
        npcs.add(npc);
    }

    public void remove(ActiveNPC npc) {
        npcs.remove(npc);
    }

    public List<ActiveNPC> getNpcs() {
        return npcs;
    }
}
