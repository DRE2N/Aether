package de.erethon.aether.ai.behavior;

import de.erethon.aether.Aether;
import de.erethon.aether.creature.AetherBaseMob;

import java.util.Comparator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class MobBehaviorController {

    private final AetherBaseMob mob;
    private final BehaviorDefinition behavior;
    private final BehaviorContext context;

    private String currentStateId;
    private int stateTicks;
    private int worldTicks;
    private final Map<String, Integer> cooldownUntil = new HashMap<>();

    public MobBehaviorController(AetherBaseMob mob, BehaviorDefinition behavior) {
        this.mob = mob;
        this.behavior = behavior;
        this.context = new BehaviorContext(this, mob);
    }

    /**
     * Targeting, goal profiles, and approach movement — runs before {@link AetherBaseMob}'s goal tick
     * so melee/ranged goals can attack in the same tick.
     */
    public void tickBeforeGoals() {
        if (behavior == null || mob.isDeadOrDying()) {
            return;
        }
        worldTicks++;
        stateTicks++;

        if (currentStateId == null) {
            switchState(behavior.initialState());
        }

        BehaviorStateDefinition state = getCurrentState();
        if (state == null) {
            return;
        }

        applyTransitions(state);

        if (worldTicks % behavior.tickInterval() != 0) {
            return;
        }

        runActions(state, BehaviorTrigger.TICK, true);
    }

    /**
     * Spells, retreat movement, look — runs after goals so casts do not override melee attacks.
     */
    public void tickAfterGoals() {
        if (behavior == null || mob.isDeadOrDying() || currentStateId == null) {
            return;
        }
        if (worldTicks % behavior.tickInterval() != 0) {
            return;
        }

        BehaviorStateDefinition state = getCurrentState();
        if (state == null) {
            return;
        }

        runActions(state, BehaviorTrigger.TICK, false);
    }

    public void trigger(BehaviorTrigger trigger) {
        if (behavior == null || currentStateId == null) {
            return;
        }
        BehaviorStateDefinition state = getCurrentState();
        if (state == null) {
            return;
        }
        runActions(state, trigger, true);
        runActions(state, trigger, false);
    }

    private void applyTransitions(BehaviorStateDefinition state) {
        List<BehaviorTransitionDefinition> transitions = state.transitions().stream()
                .sorted(Comparator.comparingInt(BehaviorTransitionDefinition::priority).reversed())
                .toList();

        for (BehaviorTransitionDefinition transition : transitions) {
            if (stateTicks < transition.minStateTicks()) {
                continue;
            }
            if (transition.conditions().stream().allMatch(condition -> condition.check(context))) {
                switchState(transition.toState());
                return;
            }
        }
    }

    private void switchState(String toState) {
        if (toState == null || toState.equals(currentStateId)) {
            return;
        }
        BehaviorStateDefinition previous = getCurrentState();
        if (previous != null) {
            runActions(previous, BehaviorTrigger.EXIT, false);
            runActions(previous, BehaviorTrigger.EXIT, true);
        }

        if (!behavior.states().containsKey(toState)) {
            Aether.addException(mob.getData().getID(), "Unknown behavior state: " + toState, "Use a valid ai.behavior.states key", null);
            return;
        }

        currentStateId = toState;
        stateTicks = 0;
        BehaviorStateDefinition current = getCurrentState();
        runActions(current, BehaviorTrigger.ENTER, true);
        runActions(current, BehaviorTrigger.ENTER, false);
    }

    private BehaviorStateDefinition getCurrentState() {
        if (currentStateId == null) {
            return null;
        }
        return behavior.states().get(currentStateId);
    }

    private void runActions(BehaviorStateDefinition state, BehaviorTrigger trigger, boolean beforeGoals) {
        if (state == null) {
            return;
        }
        List<AetherAction> actions = switch (trigger) {
            case ENTER -> state.onEnter();
            case TICK -> state.onTick();
            case DAMAGED -> state.onDamaged();
            case ATTACK -> state.onAttack();
            case TARGET -> state.onTarget();
            case DEATH -> state.onDeath();
            case EXIT -> state.onExit();
        };

        context.setTrigger(trigger);
        for (AetherAction action : actions) {
            if (action.runsBeforeGoals() != beforeGoals) {
                continue;
            }
            try {
                action.execute(context);
            } catch (Exception e) {
                Aether.addException(mob.getData().getID(), "Failed to execute behavior action " + action.getClass().getSimpleName(), "Check ai.behavior action configuration", null);
            }
        }
    }

    boolean isCooldownReady(String key) {
        return worldTicks >= cooldownUntil.getOrDefault(key, 0);
    }

    void setCooldown(String key, int ticks) {
        cooldownUntil.put(key, worldTicks + Math.max(0, ticks));
    }

    int getStateTicks() {
        return stateTicks;
    }

    int getWorldTicks() {
        return worldTicks;
    }
}
