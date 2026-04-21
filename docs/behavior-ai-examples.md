# Behavior AI Examples

These examples are valid for the current `ai.behavior` implementation in Aether.
They only use conditions/actions that are currently parsed by `BehaviorLoader`.

## Supported Conditions

- `always`
- `has_target`
- `health_below_pct;<percent>`
- `health_above_pct;<percent>`
- `target_distance_lt;<blocks>`
- `target_distance_gt;<blocks>`
- `attacker_distance_lt;<blocks>`
- `attacker_distance_gt;<blocks>`
- `state_time_at_least;<ticks>`
- `cooldown_ready;<key>`
- `random_chance;<0-100>`
- `qxl_visibility`
- `qxl_spawn`

## Supported Actions

- `set_cooldown;<key>;<ticks>`
- `cast_spell;<spellId>;<mode>;<cooldownTicks>;<range>`
- `move_to_target;<speed>`
- `move_away_from_attacker;<distance>;<speed>`
- `clear_target`
- `set_target_last_attacker`
- `set_target_nearest_player;<range>`
- `look_at_target;<maxYaw>;<maxPitch>`
- `clear_goals`
- `reset_default_goals`
- `apply_goal_profile;<profileId>`

`mode` for `cast_spellbook`: `TARGET`, `SELF`, `NEAREST`, `LAST_DAMAGE`

---

## Example 1: Elite Duelist (Retreat + Re-engage)

```yaml
class: de.erethon.aether.creature.AetherBaseMob
displayType: zombie
version: 1

ai:
  goals:
    - "0;float"
    - "1;melee_attack;1.2;true"
    - "2;random_stroll;0.2;80;false"
    - "3;look_at_players;8;100"

  targets:
    - "0;hurt_target"
    - "1;nearest_attackable;player;true"

  behavior:
    tickInterval: 2
    initialState: combat
    states:
      combat:
        onEnter:
          - "set_cooldown;phase_swap;20"
        onTick:
          - "move_to_target;1.2"
          - "cast_spell;DaggerThrow;TARGET;30;18"
        onExit:
          - "clear_target"
        transitions:
          retreat_now:
            to: retreat
            priority: 10
            minStateTicks: 20
            conditions:
              - "health_below_pct;35"
              - "cooldown_ready;phase_swap"

      retreat:
        onEnter:
          - "set_cooldown;phase_swap;40"
        onTick:
          - "move_away_from_attacker;8;1.3"
        onExit:
          - "set_cooldown;phase_swap;20"
        transitions:
          reengage:
            to: combat
            priority: 10
            conditions:
              - "health_above_pct;50"
              - "state_time_at_least;40"
```

---

## Example 2: Ranged Skirmisher (Kite if too close)

```yaml
class: de.erethon.aether.creature.AetherBaseMob
displayType: skeleton
version: 1

ai:
  goals:
    - "0;float"
    - "1;ranged_bow_attack;0.25;20;16"
    - "3;look_at_players;10;100"

  targets:
    - "0;hurt_target"
    - "1;nearest_attackable;player;true"

  behavior:
    tickInterval: 2
    initialState: pressure
    states:
      pressure:
        onTick:
          - "cast_spell;PoisonArrow;TARGET;40;20"
        transitions:
          kite:
            to: kite
            priority: 20
            conditions:
              - "target_distance_lt;6"

      kite:
        onEnter:
          - "set_cooldown;kite_lock;30"
        onTick:
          - "move_away_from_attacker;10;1.2"
          - "cast_spell;DashBack;SELF;60;0"
        onExit:
          - "set_cooldown;kite_lock;20"
        transitions:
          pressure_again:
            to: pressure
            priority: 20
            conditions:
              - "target_distance_gt;10"
              - "state_time_at_least;20"
```

---

## Example 3: QXL-aware Guard

Uses QXL checks as transition conditions.

```yaml
class: de.erethon.aether.creature.AetherBaseMob
displayType: villager
version: 1

qxl:
  visibilityConditions:
    - "has_quest: quest=guard_test"

ai:
  behavior:
    tickInterval: 2
    initialState: idle
    states:
      idle:
        transitions:
          alert:
            to: alert
            conditions:
              - "qxl_visibility"
              - "has_target"

      alert:
        onTick:
          - "move_to_target;1.0"
          - "cast_spell;WarCry;TARGET;80;12"
        onExit:
          - "clear_target"
        transitions:
          idle_again:
            to: idle
            conditions:
              - "target_distance_gt;16"
```

---

## Example 4: Goal Profile Switching (Berserk Phase)

```yaml
class: de.erethon.aether.creature.AetherBaseMob
displayType: zombie
version: 1

ai:
  behavior:
    tickInterval: 2
    goalProfiles:
      normal:
        goals:
          - "0;float"
          - "1;melee_attack;1.0;true"
          - "3;look_at_players;8;100"
        targets:
          - "0;hurt_target"
          - "1;nearest_attackable;player;true"

      berserk:
        goals:
          - "0;float"
          - "1;melee_attack;1.5;true"
          - "2;random_stroll;0.25;40;false"
        targets:
          - "0;hurt_target"
          - "1;nearest_attackable;player;true"

    initialState: normal
    states:
      normal:
        onEnter:
          - "apply_goal_profile;normal"
        onTick:
          - "set_target_nearest_player;18"
          - "look_at_target;180;180"
        transitions:
          enrage:
            to: berserk
            priority: 10
            conditions:
              - "health_below_pct;30"

      berserk:
        onEnter:
          - "apply_goal_profile;berserk"
          - "set_cooldown;berserk_time;100"
        onTick:
          - "cast_spell;WarCry;SELF;80;0"
        onExit:
          - "reset_default_goals"
        transitions:
          calm_down:
            to: normal
            conditions:
              - "cooldown_ready;berserk_time"
              - "health_above_pct;45"
```

