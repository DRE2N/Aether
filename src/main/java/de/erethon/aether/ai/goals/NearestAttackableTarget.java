package de.erethon.aether.ai.goals;

import de.erethon.aether.ai.GoalClass;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.Optional;

public class NearestAttackableTarget extends AEPathfinderGoal {

    Class<? extends LivingEntity> target;
    boolean checkVisibility;

    public NearestAttackableTarget() {
        goalClass = GoalClass.TARGET;
    }

    @Override
    public Goal get(LivingEntity entity) {
        if (target == null) {
            throw new IllegalStateException("Nearest attackable target class was not loaded.");
        }
        return new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>((Mob) entity, target, checkVisibility, false);
    }

    @Override
    public void load(String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("Expected nearest_attackable config: <entityType>;<checkVisibility>");
        }
        Optional<Class<? extends LivingEntity>> targetClass = TargetEntityClassResolver.resolve(args[0]);
        if (targetClass.isEmpty()) {
            throw new IllegalArgumentException("Could not find entity type " + args[0]);
        }
        target = targetClass.get();
        checkVisibility = Boolean.parseBoolean(args[1]);
    }

    public Class<? extends LivingEntity> getTarget() {
        return target;
    }

    public boolean isCheckVisibility() {
        return checkVisibility;
    }
}
