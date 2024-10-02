package de.erethon.aether.ai;

import de.erethon.aether.Aether;
import de.erethon.aether.ai.goals.*;
import de.erethon.aether.ai.goals.HurtByTargetGoal;
import de.erethon.bedrock.chat.MessageUtil;

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
    NEAREST_ATTACKABLE,
    PANIC,
    RANDOM_LOOK_AROUND,
    RANDOM_STROLL,
    RANDOM_SWIM,
    RANGED_BOW_ATTACK,
    RANGED_CROSSBOW_ATTACK,
    RESTRICT_SUN;

}

public class GoalLoader {

    public static Set<AEPathfinderGoal> loadGoals(List<String> cfg) {
        Set<AEPathfinderGoal> goals = new HashSet<>();
        for (String cfgString : cfg) {
            MessageUtil.log("Raw: " + cfgString);
            String[] split = cfgString.split(";");
            int priority;
            try {
                priority = Integer.parseInt(split[0]);
            }
            catch (NumberFormatException e) {
                Aether.addException("GoalLoader", "Invalid priority: " + split[0], "Check if the priority is a valid number. Format: '<prio>;<goal_name>;<goal_config>'", e);
                continue;
            }
            AIGoalType goalType;
            try {
                goalType = AIGoalType.valueOf(split[1].toUpperCase());
            }
            catch (IllegalArgumentException e) {
                Aether.addException("GoalLoader", "Invalid goal type: " + split[1], "Check if the goal type is a valid goal type. Format: '<prio>;<goal_name>;<goal_config>'", e);
                continue;
            }
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
                case NEAREST_ATTACKABLE -> {
                    goal = new NearestAttackableTargetGoal();
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
                default -> {
                    Aether.addException("GoalLoader", "Invalid goal type: " + split[1], "This goal simply does not exist", null);
                }
            }
            String[] args = Arrays.copyOfRange(split, 2,split.length);
            MessageUtil.log("SplitArgs: " + Arrays.toString(args));
            try {
                goal.load(args);
            }
            catch (Exception e) {
                Aether.addException("GoalLoader", "Error loading goal " + split[1], "Check if the goal config is correct", e);
                continue;
            }
            goal.setPrio(priority);
            goals.add(goal);
        }
        return goals;
    }
}

