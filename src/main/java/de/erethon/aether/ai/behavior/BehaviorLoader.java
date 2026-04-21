package de.erethon.aether.ai.behavior;

import de.erethon.aether.Aether;
import de.erethon.aether.ai.SpellTargetMode;
import de.erethon.aether.ai.behavior.actions.ApplyGoalProfileAction;
import de.erethon.aether.ai.behavior.actions.CastSpellAction;
import de.erethon.aether.ai.behavior.actions.ClearMobGoalsAction;
import de.erethon.aether.ai.behavior.actions.ClearTargetAction;
import de.erethon.aether.ai.behavior.actions.LookAtTargetAction;
import de.erethon.aether.ai.behavior.actions.MoveAwayFromAttackerAction;
import de.erethon.aether.ai.behavior.actions.MoveToTargetAction;
import de.erethon.aether.ai.behavior.actions.ResetDefaultGoalsAction;
import de.erethon.aether.ai.behavior.actions.SetCooldownAction;
import de.erethon.aether.ai.behavior.actions.SetTargetLastAttackerAction;
import de.erethon.aether.ai.behavior.actions.SetTargetNearestPlayerAction;
import de.erethon.aether.ai.behavior.conditions.AlwaysCondition;
import de.erethon.aether.ai.behavior.conditions.AttackerDistanceGtCondition;
import de.erethon.aether.ai.behavior.conditions.AttackerDistanceLtCondition;
import de.erethon.aether.ai.behavior.conditions.CooldownReadyCondition;
import de.erethon.aether.ai.behavior.conditions.HasTargetCondition;
import de.erethon.aether.ai.behavior.conditions.HealthAbovePercentCondition;
import de.erethon.aether.ai.behavior.conditions.HealthBelowPercentCondition;
import de.erethon.aether.ai.behavior.conditions.QxlSpawnCondition;
import de.erethon.aether.ai.behavior.conditions.QxlVisibilityCondition;
import de.erethon.aether.ai.behavior.conditions.RandomChanceCondition;
import de.erethon.aether.ai.behavior.conditions.StateTimeAtLeastCondition;
import de.erethon.aether.ai.behavior.conditions.TargetDistanceGtCondition;
import de.erethon.aether.ai.behavior.conditions.TargetDistanceLtCondition;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BehaviorLoader {

    private BehaviorLoader() {
    }

    public static BehaviorDefinition load(String mobId, ConfigurationSection behaviorSection) {
        if (behaviorSection == null) {
            return null;
        }

        ConfigurationSection statesSection = behaviorSection.getConfigurationSection("states");
        if (statesSection == null || statesSection.getKeys(false).isEmpty()) {
            Aether.addException(mobId, "ai.behavior.states is missing or empty", "Add at least one behavior state", null);
            return null;
        }

        Map<String, BehaviorStateDefinition> states = new LinkedHashMap<>();
        for (String stateId : statesSection.getKeys(false)) {
            ConfigurationSection stateSection = statesSection.getConfigurationSection(stateId);
            if (stateSection == null) {
                continue;
            }
            List<BehaviorTransitionDefinition> transitions = loadTransitions(mobId, stateId, stateSection.getConfigurationSection("transitions"));
            BehaviorStateDefinition definition = new BehaviorStateDefinition(
                    stateId,
                    parseActions(mobId, stateId, "onEnter", stateSection.getStringList("onEnter")),
                    parseActions(mobId, stateId, "onExit", stateSection.getStringList("onExit")),
                    parseActions(mobId, stateId, "onTick", stateSection.getStringList("onTick")),
                    parseActions(mobId, stateId, "onDamaged", stateSection.getStringList("onDamaged")),
                    parseActions(mobId, stateId, "onAttack", stateSection.getStringList("onAttack")),
                    parseActions(mobId, stateId, "onTarget", stateSection.getStringList("onTarget")),
                    parseActions(mobId, stateId, "onDeath", stateSection.getStringList("onDeath")),
                    transitions
            );
            states.put(stateId, definition);
        }

        if (states.isEmpty()) {
            Aether.addException(mobId, "No valid states found in ai.behavior", "Ensure ai.behavior.states contains valid state sections", null);
            return null;
        }

        String firstState = states.keySet().iterator().next();
        String initialState = behaviorSection.getString("initialState", firstState);
        if (!states.containsKey(initialState)) {
            Aether.addException(mobId, "Invalid ai.behavior.initialState: " + initialState, "Use one of the configured state IDs", null);
            initialState = firstState;
        }

        int tickInterval = Math.max(1, behaviorSection.getInt("tickInterval", 2));
        return new BehaviorDefinition(tickInterval, initialState, states);
    }

    private static List<BehaviorTransitionDefinition> loadTransitions(String mobId, String stateId, ConfigurationSection transitionsSection) {
        List<BehaviorTransitionDefinition> transitions = new ArrayList<>();
        if (transitionsSection == null) {
            return transitions;
        }

        for (String key : transitionsSection.getKeys(false)) {
            ConfigurationSection section = transitionsSection.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            String to = section.getString("to");
            if (to == null || to.isBlank()) {
                Aether.addException(mobId, "Missing transition target in state " + stateId + ": " + key, "Set transitions.<name>.to", null);
                continue;
            }
            int priority = section.getInt("priority", 0);
            int minStateTicks = Math.max(0, section.getInt("minStateTicks", 0));
            List<AetherCondition> conditions = parseConditions(mobId, stateId, key, section.getStringList("conditions"));
            transitions.add(new BehaviorTransitionDefinition(to, priority, minStateTicks, conditions));
        }
        return transitions;
    }

    private static List<AetherCondition> parseConditions(String mobId, String stateId, String transitionKey, List<String> rawConditions) {
        List<AetherCondition> conditions = new ArrayList<>();
        for (String raw : rawConditions) {
            try {
                AetherCondition condition = parseCondition(raw);
                if (condition == null) {
                    Aether.addException(mobId, "Invalid behavior condition in state " + stateId + ", transition " + transitionKey + ": " + raw, "Check ai.behavior condition syntax", null);
                    continue;
                }
                conditions.add(condition);
            } catch (Exception e) {
                Aether.addException(mobId, "Invalid behavior condition in state " + stateId + ", transition " + transitionKey + ": " + raw, "Check ai.behavior condition syntax", null);
            }
        }
        return conditions;
    }

    private static List<AetherAction> parseActions(String mobId, String stateId, String actionListName, List<String> rawActions) {
        List<AetherAction> actions = new ArrayList<>();
        for (String raw : rawActions) {
            try {
                AetherAction action = parseAction(raw);
                if (action == null) {
                    Aether.addException(mobId, "Invalid behavior action in state " + stateId + " (" + actionListName + "): " + raw, "Check ai.behavior action syntax", null);
                    continue;
                }
                actions.add(action);
            } catch (Exception e) {
                Aether.addException(mobId, "Invalid behavior action in state " + stateId + " (" + actionListName + "): " + raw, "Check ai.behavior action syntax", null);
            }
        }
        return actions;
    }

    private static AetherCondition parseCondition(String raw) {
        String[] args = raw.split(";");
        String type = args[0].toLowerCase(Locale.ROOT);
        return switch (type) {
            case "always" -> new AlwaysCondition();
            case "has_target" -> new HasTargetCondition();
            case "health_below_pct" -> new HealthBelowPercentCondition(parseDouble(args, 1, 100));
            case "health_above_pct" -> new HealthAbovePercentCondition(parseDouble(args, 1, 0));
            case "target_distance_lt" -> new TargetDistanceLtCondition(parseDouble(args, 1, 8));
            case "target_distance_gt" -> new TargetDistanceGtCondition(parseDouble(args, 1, 8));
            case "attacker_distance_lt" -> new AttackerDistanceLtCondition(parseDouble(args, 1, 8));
            case "attacker_distance_gt" -> new AttackerDistanceGtCondition(parseDouble(args, 1, 8));
            case "state_time_at_least" -> new StateTimeAtLeastCondition(parseInt(args, 1, 20));
            case "cooldown_ready" -> new CooldownReadyCondition(args.length > 1 ? args[1] : "default");
            case "random_chance" -> new RandomChanceCondition(parseDouble(args, 1, 100));
            case "qxl_visibility" -> new QxlVisibilityCondition();
            case "qxl_spawn" -> new QxlSpawnCondition();
            default -> null;
        };
    }

    private static AetherAction parseAction(String raw) {
        String[] args = raw.split(";");
        String type = args[0].toLowerCase(Locale.ROOT);
        return switch (type) {
            case "set_cooldown" -> new SetCooldownAction(args.length > 1 ? args[1] : "default", parseInt(args, 2, 20));
            case "cast_spell" -> {
                if (args.length < 2 || args[1].isBlank()) {
                    throw new IllegalArgumentException("cast_spell requires a spell id");
                }
                yield new CastSpellAction(
                        args[1],
                        SpellTargetMode.valueOf((args.length > 2 ? args[2] : "TARGET").toUpperCase(Locale.ROOT)),
                        parseInt(args, 3, 40),
                        parseDouble(args, 4, 16)
                );
            }
            case "move_to_target" -> new MoveToTargetAction(parseDouble(args, 1, 1.0));
            case "move_away_from_attacker" -> new MoveAwayFromAttackerAction(parseDouble(args, 1, 8), parseDouble(args, 2, 1.0));
            case "clear_target" -> new ClearTargetAction();
            case "set_target_last_attacker" -> new SetTargetLastAttackerAction();
            case "set_target_nearest_player" -> new SetTargetNearestPlayerAction(parseDouble(args, 1, 16));
            case "look_at_target" -> new LookAtTargetAction((float) parseDouble(args, 1, 180), (float) parseDouble(args, 2, 180));
            case "clear_goals" -> new ClearMobGoalsAction();
            case "reset_default_goals" -> new ResetDefaultGoalsAction();
            case "apply_goal_profile" -> {
                if (args.length < 2 || args[1].isBlank()) {
                    throw new IllegalArgumentException("apply_goal_profile requires a profile id");
                }
                yield new ApplyGoalProfileAction(args[1]);
            }
            default -> null;
        };
    }

    private static int parseInt(String[] args, int index, int defaultValue) {
        if (args.length <= index) {
            return defaultValue;
        }
        return Integer.parseInt(args[index]);
    }

    private static double parseDouble(String[] args, int index, double defaultValue) {
        if (args.length <= index) {
            return defaultValue;
        }
        return Double.parseDouble(args[index]);
    }
}

