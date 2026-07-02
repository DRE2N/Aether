package de.erethon.aether.ai.goals;

import de.erethon.aether.ai.GoalClass;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.Optional;

public class AEAvoidTargetGoal extends AEPathfinderGoal {

    double walkSpeedModifier;
    double sprintSpeedModifier;
    float maxDist;
    Class<? extends LivingEntity> toAvoid;

    public AEAvoidTargetGoal() {
        goalClass = GoalClass.MOVE;
    }

    @Override
    public Goal get(LivingEntity entity) {
        if (toAvoid == null) {
            throw new IllegalStateException("Avoid target class was not loaded.");
        }
        return new AvoidEntityGoal<>((PathfinderMob) entity, toAvoid, maxDist, walkSpeedModifier, sprintSpeedModifier);
    }

    @Override
    public void load(String[] args) {
        isCreatureOnly = true;
        if (args.length < 4) {
            throw new IllegalArgumentException("Expected avoid_target config: <entityType>;<maxDistance>;<walkSpeedModifier>;<sprintSpeedModifier>");
        }
        Optional<Class<? extends LivingEntity>> toAvoidClass = TargetEntityClassResolver.resolve(args[0]);
        if (toAvoidClass.isPresent()) {
            toAvoid = toAvoidClass.get();
            maxDist = Float.parseFloat(args[1]);
            walkSpeedModifier = Double.parseDouble(args[2]);
            sprintSpeedModifier = Double.parseDouble(args[3]);
            return;
        }

        // Legacy docs used: <walkSpeedModifier>;<sprintSpeedModifier>;<maxDistance>;<entityType>
        toAvoidClass = TargetEntityClassResolver.resolve(args[3]);
        if (toAvoidClass.isEmpty()) {
            throw new IllegalArgumentException("Could not find entity type " + args[0] + " or " + args[3]);
        }
        toAvoid = toAvoidClass.get();
        walkSpeedModifier = Double.parseDouble(args[0]);
        sprintSpeedModifier = Double.parseDouble(args[1]);
        maxDist = Float.parseFloat(args[2]);
    }
}
