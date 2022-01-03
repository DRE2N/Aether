package de.erethon.aether.ai.pathfinder;

import de.erethon.aether.ai.pathfinder.goals.*;
import de.erethon.commons.chat.MessageUtil;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

enum AIGoalType {
    AVOID_TARGET,
    AVOID_WATER,
    DOOR_INTERACT,
    FLEE_SUN,
    FLOAT,
    HURT_TARGET,
    LEAP_AT,
    LOOK_AT_PLAYERS,
    MELEE_ATTACK,
    PANIC,
    RANDOM_LOOK_AROUND,
    RANDOM_STROLL,
    RANDOM_SWIM,
    RANGED_BOW_ATTACK,
    RANGED_CROSSBOW_ATTACK,
    RESTRICT_SUN;

}
enum AITargetType {
    NEAREST_ATTACKABLE;
}
public class GoalLoader {

    public static Set<AEPathfinderGoal> loadGoals(List<String> cfg) {
        Set<AEPathfinderGoal> goals = new HashSet<>();
        for (String cfgString : cfg) {
            MessageUtil.log("Raw: " + cfgString);
            String[] split = cfgString.split(";");
            int priority = Integer.parseInt(split[0]);
            AIGoalType goalType = AIGoalType.valueOf(split[1].toUpperCase());
            AEPathfinderGoal goal = null;
            MessageUtil.log("Split1: " + Arrays.toString(split));
            switch (goalType) {
                case AVOID_TARGET -> {
                    goal = new AEAvoidTargetGoal();
                }
                case AVOID_WATER -> {
                    goal = new AvoidWaterGoal();
                }
                case DOOR_INTERACT -> {
                    goal = new DoorInteractGoal();
                }
                case FLEE_SUN -> {
                    goal = new FleeSunGoal();
                }
                case FLOAT -> {
                    goal = new FloatGoal();
                }
                case HURT_TARGET -> {
                    goal = new HurtByTargetGoal();
                }
                case LEAP_AT -> {
                    goal = new LeapAtTargetGoal();
                }
                case LOOK_AT_PLAYERS -> {
                    goal = new LookAtPlayer();
                }
                case MELEE_ATTACK -> {
                    goal = new MeleeAttackGoal();
                }
                case PANIC -> {
                    goal = new PanicGoal();
                }
                case RANDOM_LOOK_AROUND -> {
                    goal = new RandomLookAroundGoal();
                }
                case RANDOM_STROLL -> {
                    goal = new RandomStrollGoal();
                }
                case RANDOM_SWIM -> {
                    goal = new RandomSwimGoal();
                }
                case RANGED_BOW_ATTACK -> {
                    goal = new RangedBowAttackGoal();
                }
                case RANGED_CROSSBOW_ATTACK -> {
                    goal = new RangedCrossbowAttackGoal();
                }
                case RESTRICT_SUN -> {
                    goal = new RestrictSunGoal();
                }
                default -> throw new IllegalStateException("Unexpected value: " + goalType);
            }
            String[] args = Arrays.copyOfRange(split, 2,split.length);
            MessageUtil.log("SplitArgs: " + Arrays.toString(args));
            goal.load(args);
            goal.setPrio(priority);
            goals.add(goal);
        }
        return goals;
    }
}

