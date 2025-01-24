package de.erethon.aether.ai.goals.custom;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public class EnemyTeamTargetGoal extends TargetGoal {

    protected final int randomInterval = 10;
    protected LivingEntity target;
    private final int range;

    public EnemyTeamTargetGoal(Mob mob, boolean checkVisibility, boolean checkNavigable, int range) {
        super(mob, checkVisibility, checkNavigable);
        this.range = range;
    }

    @Override
    public boolean canUse() {
        if (mob.getRandom().nextInt(randomInterval) != 0) {
            return false;
        } else {
            //findTarget();
            return target != null;
        }
    }

    /*protected void findTarget() {
        target = mob.level().getentit(mob.level().getEntitiesOfClass(LivingEntity.class, getTargetSearchArea(getFollowDistance()), (entityliving) -> {
            return entityliving.getTeam() != mob.getTeam();
        }), TargetingConditions.forCombat().range(range), mob, mob.getX(), mob.getEyeY(), mob.getZ());
    }*/

    protected AABB getTargetSearchArea(double distance) {
        return mob.getBoundingBox().inflate(distance, 4.0D, distance);
    }
}
