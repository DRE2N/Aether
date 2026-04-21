
## Basic Configuration

### Required Fields

```yaml
class: de.erethon.aether.creature.AetherBaseMob
# The Java class that handles this NPC's behavior
# Common options:
#   - de.erethon.aether.creature.AetherBaseMob (basic mob)
#   - de.erethon.aether.creature.AetherPlayer (player-like NPC)
#   - de.erethon.aether.creature.ModelledMob (uses Daedalus)

displayType: pig
# The base Minecraft entity type to use for hitbox and basic behavior
# Examples: pig, zombie, skeleton, player, villager, etc.

version: 1
# Configuration version number. Mobs will get updated if their version is below this number.
```

### Display & Identity

```yaml
name:
  en: "My NPC"  # English name
  de: "Mein NPC"  # German name

model: "my_model_id"
# Optional. Daedalus model ID to use for this NPC. Only works with ModelledMob class.

mobCategory: MONSTER
# Optional. Override the mob category, used for spawning limits
# Options: MONSTER, CREATURE, AMBIENT, WATER_CREATURE, WATER_AMBIENT, MISC
# Default: MONSTER
```

### General Settings

```yaml
instancable: false
# If true, each player sees their own instance of this NPC
# Useful for quest NPCs or personal interactions
# Default: false

config:
  collision: true
  # If true, the NPC has collision with players and entities
  # Default: true
  
  persistent: false
  # If true, the NPC persists across chunk unloads. For example in vanilla, most monsters are not persistent.
  # Default: false
  
  invulnerable: false
  # If true, the NPC cannot take any damage
  # Default: false
  
  invulnerableToPlayers: false
  # If true, the NPC cannot take damage from players specifically but can still take damage from other mobs or environmental sources
  # Default: false
  
  glowing: false
  # If true, the NPC will have the glowing effect
  # Default: false
  
  gravity: true
  # If true, the NPC is affected by gravity
  # Default: true
  
  nametagVisible: false
  # If true, the NPC's nameplate is visible
  # Default: false
  
  noDamageTicks: 20
  # Number of ticks the NPC is invulnerable after taking damage
  # Default: 20 (1 second)
  
  maximumAir: 20
  # Maximum air the NPC can have underwater (in ticks)
  # Default: 20
```

### Player Skin (only for AetherPlayer)

```yaml
skin: "https://minesk.in/abc123def456"
# Mineskin URL for player skin
# Get skins from https://mineskin.org

skins:
  - "https://minesk.in/abc123def456"
  - "https://minesk.in/xyz789ghi012"
# Optional. List of multiple skins - one will be randomly selected per spawn
# If specified, overrides the single 'skin' value
```

---

## Spells

Spells are cast at specific triggers. Requires Spellbook plugin.

```yaml
spells:
  onAttack:
    # Spells cast when the NPC attacks
    Cleave:
      chance: 20  # 20% chance to cast
    DaggerThrow:
      chance: 15  # 15% chance to cast
  
  onDamaged:
    # Spells cast when the NPC takes damage
    DashBack:
      chance: 10  # 10% chance to cast
    Heal:
      chance: 5
  
  onTimer:
    # Spells cast on a timer (periodically)
    Buff:
      chance: 100  # Cast every timer tick
  
  onDeath:
    # Spells cast when the NPC dies
    Explosion:
      chance: 100  # Always cast on death
  
  onTarget:
    # Spells cast when the NPC targets an entity
    WarCry:
      chance: 50  # 50% chance when targeting
```

Spell names must match spell IDs defined in Spellbook configuration.

---

## Leveling System

The leveling system allows NPCs to have different stats and loot based on their level. When an NPC spawns, a level is randomly selected based on weights.
Alternatively , you can set a fixed level for the NPC using Spawners or QuestsXL.

```yaml
leveling:
  1:  # Level 1 configuration
    weight: 100
    # Weight for random selection. Higher = more common.
    # Example: If level 1 has weight 100 and level 2 has weight 50,
    # level 1 will spawn 66% of the time, level 2 33% of the time.
    
    attributes:
      # Attribute bonuses for this level
      0:
        id: "max_health"
        value: 100  # Exact value
      1:
        id: "attack_damage"
        min: 10  # Random value between min and max
        max: 15
    
    loot:
      # Level-specific loot (overrides base loot)
      xp:
        min: 10
        max: 20
        # Random XP between 10 and 20
      
      items:
        - "minecraft:iron_sword;100"
        - "minecraft:iron_ingot;50"
  
  5:  # Level 5 configuration
    weight: 50
    # Less common than level 1
    
    attributes:
      0:
        id: "max_health"
        min: 200
        max: 250
      1:
        id: "attack_damage"
        value: 25
      2:
        id: "movement_speed"
        value: 0.3
    
    loot:
      xp:
        value: 100
        # Fixed XP value
      
      items:
        - "minecraft:diamond_sword;100"
        - "minecraft:diamond;75"
        - "erethon:legendary_item;10"
  
  10:  # Level 10 configuration (boss level)
    weight: 10
    # Very rare spawn
    
    attributes:
      0:
        id: "max_health"
        value: 1000
      1:
        id: "attack_damage"
        value: 50
      2:
        id: "armor"
        value: 20
      3:
        id: "knockback_resistance"
        value: 1.0
    
    loot:
      xp:
        min: 500
        max: 1000
      
      items:
        - "erethon:boss_drop;100"
        - "minecraft:nether_star;50"
```

### How Leveling Works

1. **Random Selection**: When an NPC spawns, the system randomly selects a level based on weights
2. **Attribute Inheritance**: If an attribute is not defined at a specific level, it falls back to the highest configured level below it
3. **Loot Inheritance**: Loot uses the highest configured level at or below the target level

Example:
- Level 3 spawns but only levels 1, 5, and 10 are configured
- Health comes from level 1 (highest below 3 with health defined)
- Attack damage comes from level 1
- Loot comes from level 1

---

## Interaction

### Messages

```yaml
interaction:
  messages:
    # Messages displayed above the NPC
    - "Hello there!"
    - "How can I help you?"
    - "Welcome, traveler!"
  
  randomTalker: true
  # If true, randomly says one of the messages when players are nearby
  # Default: false
```

### Sounds

```yaml
interaction:
  sounds:
    attack: "minecraft:entity_wolf_howl"
    # Sound played when attacking
    
    ambient: "minecraft:entity_villager_ambient"
    # Sound played periodically
    
    shoot: "minecraft:entity_skeleton_shoot"
    # Sound played when shooting projectiles
    
    death: "minecraft:entity_zombie_death"
    # Sound played on death
    
    hurt: "minecraft:entity_player_hurt"
    # Sound played when taking damage
```

All sounds are optional. Use Minecraft sound IDs from the Bukkit Sound enum.

---

## Combat

### Team/Faction

```yaml
team: "Brotherhood"
# Optional. Faction name for team-based combat
# NPCs in the same team won't attack each other
```

### Home Location

```yaml
homeLocation:
  x: 100
  y: 64
  z: 200
  range: 32
  # NPC will try to stay within this range of the home location
  # Useful for guards or stationary NPCs.
  # NPCs spawned from a Spawner or QXL event will automatically set their home location to their spawn point.
```

---

## QXL Integration

Aether supports QuestsXL integration through the `qxl` section. This allows you to add QuestsXL actions and conditions to your NPCs.

```yaml
qxl:
  # Standard QuestsXL action/condition format
  # See QuestsXL documentation for available actions and conditions
  # https://erethon.de/docs/questsxl
  
  # Example actions:
  onDeathActions:
    - "give_item: item=diamond; amount=5"
  visibilityConditions:
    - "has_quest: quest=my_quest"
```
---

## AI Configuration

AI controls the NPC's behavior through goals and targets.

```yaml
ai:
  goals:
    # Goals define what actions the NPC takes
    # Format: <priority>;<goal_type>;<parameters>
    - "0;float"
    - "1;melee_attack;0.25;true"
    - "2;random_stroll;0.2;80;false"
    - "3;look_at_players;8;100"
    - "4;random_look_around"
  
  targets:
    # Targets define what the NPC attacks
    # Format: <priority>;<target_type>;<parameters>
    - "0;hurt_target"
    - "1;nearest_attackable;player;true"
```

### Available Goals

#### Movement Goals

**FLOAT**
```yaml
- "0;float"
```
Makes the NPC float in water.

**RANDOM_STROLL**
```yaml
- "2;random_stroll;<speed>;<interval>;<checkNoActionTime>"
```
- `speed` - Movement speed multiplier (e.g., 0.2)
- `interval` - Ticks between movement attempts (e.g., 80)
- `checkNoActionTime` - Whether to check if the mob is doing nothing (true/false)

**RANDOM_SWIM**
```yaml
- "2;random_swim;<speed>;<interval>;<checkNoActionTime>"
```
Same parameters as RANDOM_STROLL but for swimming.

**AVOID_WATER**
```yaml
- "1;avoid_water"
```
Makes the NPC avoid walking into water.

**FLEE_SUN**
```yaml
- "1;flee_sun;<speedModifier>"
```
- `speedModifier` - Speed multiplier when fleeing sun (e.g., 0.25)

**RESTRICT_SUN**
```yaml
- "1;restrict_sun"
```
Prevents the NPC from moving into sunlight.

**PANIC**
```yaml
- "1;panic;<speedModifier>"
```
- `speedModifier` - Speed multiplier when panicking (e.g., 0.3)

**DOOR_INTERACT**
```yaml
- "1;door_interact"
```
Allows the NPC to open doors.

#### Combat Goals

**MELEE_ATTACK**
```yaml
- "1;melee_attack;<speedModifier>;<followTargetIfNotSeen>"
```
- `speedModifier` - Speed multiplier when chasing target (e.g., 0.25)
- `followTargetIfNotSeen` - Whether to follow target through blocks (true/false)

**RANGED_BOW_ATTACK**
```yaml
- "0;ranged_bow_attack;<speedModifier>;<minDistance>;<maxDistance>"
```
- `speedModifier` - Movement speed during combat (e.g., 0.2)
- `minDistance` - Minimum attack distance in blocks (e.g., 4)
- `maxDistance` - Maximum attack distance in blocks (e.g., 20)

**RANGED_CROSSBOW_ATTACK**
```yaml
- "0;ranged_crossbow_attack;<minDistance>;<maxDistance>"
```
- `minDistance` - Minimum attack distance in blocks (e.g., 4)
- `maxDistance` - Maximum attack distance in blocks (e.g., 20)

**LEAP_AT_TARGET**
```yaml
- "1;leap_at;<leapMotion>"
```
- `leapMotion` - Leap height/power (e.g., 0.4)

#### Interaction Goals

**LOOK_AT_PLAYERS**
```yaml
- "3;look_at_players;<lookDistance>;<probability>"
```
- `lookDistance` - Distance to look at players (e.g., 8)
- `probability` - Chance per tick to look (e.g., 100)

**RANDOM_LOOK_AROUND**
```yaml
- "4;random_look_around"
```
Makes the NPC randomly look around.

### Available Targets

**HURT_TARGET**
```yaml
- "0;hurt_target"
```
Targets entities that hurt this NPC.

**NEAREST_ATTACKABLE**
```yaml
- "1;nearest_attackable;<entityType>;<checkVisibility>"
```
- `entityType` - Entity type to target (e.g., player, zombie, skeleton)
- `checkVisibility` - Whether to require line of sight (true/false)

**AVOID_TARGET**
```yaml
- "1;avoid_target;<entityType>;<maxDistance>;<walkSpeedModifier>;<sprintSpeedModifier>"
```
- `entityType` - Entity type to avoid
- `maxDistance` - Distance to start avoiding (e.g., 10)
- `walkSpeedModifier` - Walking speed when avoiding (e.g., 0.2)
- `sprintSpeedModifier` - Sprint speed when avoiding (e.g., 0.3)

---

## Advanced Behavior AI (Optional)

`ai.behavior` is an optional state-machine based AI layer for more advanced mobs.

- If `ai.behavior` is **not** set, the mob behaves exactly like before (normal goals + spell triggers + qxl section).
- If `ai.behavior` is set, behavior states run in parallel and can drive advanced decisions.
- `tickInterval` is configurable per mob and defaults to `2` ticks.

```yaml
ai:
  behavior:
    tickInterval: 2
    initialState: combat
    states:
      combat:
        onTick:
          - "cast_spell;FlameBolt;TARGET;40;16"
          - "move_to_target;1.15"
        transitions:
          low_health:
            to: retreat
            priority: 10
            minStateTicks: 20
            conditions:
              - "health_below_pct;35"

      retreat:
        onTick:
          - "move_away_from_attacker;10;1.3"
        transitions:
          recovered:
            to: combat
            conditions:
              - "health_above_pct;55"
              - "state_time_at_least;40"
```

### Behavior State Fields

- `onEnter` - Runs once when entering state
- `onExit` - Runs once when leaving state
- `onTick` - Runs every `tickInterval` while in state
- `onDamaged` - Runs when mob is damaged
- `onAttack` - Runs when mob lands an attack
- `onTarget` - Runs when mob gets a new target
- `onDeath` - Runs on death
- `transitions` - Named transition list with `to`, optional `priority`, optional `minStateTicks`, and `conditions`

### Built-in Conditions (`transitions.*.conditions`)

- `always` - Always returns true. Useful as a fallback transition if no other condition matches.
- `has_target` - True when the mob currently has an attack target. Use this to gate combat-only states.
- `health_below_pct;<percent>` - True when current health is at or below the given percent. Common for enrage or retreat triggers.
- `health_above_pct;<percent>` - True when current health is at or above the given percent. Useful for returning from defensive states.
- `target_distance_lt;<blocks>` - True when the current target is closer than the distance. Good for kiting or panic switches.
- `target_distance_gt;<blocks>` - True when the target is farther away than the distance (or no target exists). Useful to re-enter pressure states.
- `attacker_distance_lt;<blocks>` - True when the last attacking player is within range. Useful for reactive behavior after getting hit.
- `attacker_distance_gt;<blocks>` - True when the last attacking player is outside range (or missing). Useful for disengage logic.
- `state_time_at_least;<ticks>` - True after staying in the current state for at least the given ticks. Prevents rapid state thrashing.
- `cooldown_ready;<key>` - True when the named behavior cooldown has expired. Use together with `set_cooldown` actions.
- `random_chance;<0-100>` - Rolls chance each evaluation cycle and succeeds on hit. Good for small behavior variation.
- `qxl_visibility` - Checks QXL visibility conditions using the last attacking player as context. Useful when AI should respect quest visibility.
- `qxl_spawn` - Checks QXL spawn conditions from the holder. Useful for quest-driven activation/deactivation.

### Built-in Actions (`on*` lists)

- `set_cooldown;<key>;<ticks>`
- `cast_spell;<spellId>;<mode>;<cooldownTicks>;<range>`
  - `mode`: `TARGET`, `SELF`, `NEAREST`, `LAST_DAMAGE`
- `move_to_target;<speed>`
- `move_away_from_attacker;<distance>;<speed>`
- `clear_target`
- `set_target_last_attacker`
- `set_target_nearest_player;<range>`
- `look_at_target;<maxYaw>;<maxPitch>`
- `clear_goals`
- `reset_default_goals`
- `apply_goal_profile;<profileId>`

### Goal-Related Actions

- `clear_goals` - Removes all currently active goal and target goals from the mob. Use this when a state should fully pause movement/target AI.
- `reset_default_goals` - Rebuilds the mob's original goals and targets from config (`ai.goals`/`ai.targets`). Use this to cleanly leave scripted phases.
- `apply_goal_profile;<profileId>` - Replaces current goals with a configured profile from `ai.behavior.goalProfiles`. This is the main way to switch combat style between states.

### Goal Profile Switching

You can define alternate goal/target sets and switch to them from behavior actions.

```yaml
ai:
  behavior:
    goalProfiles:
      aggressive:
        goals:
          - "0;float"
          - "1;melee_attack;1.3;true"
        targets:
          - "0;hurt_target"
          - "1;nearest_attackable;player;true"

      defensive:
        goals:
          - "0;float"
          - "2;random_stroll;0.2;80;false"
        targets:
          - "0;hurt_target"
```

Then switch inside a state with:

```yaml
onEnter:
  - "apply_goal_profile;defensive"
```

### One Complete YAML Example

```yaml
ai:
  goals:
    - "0;float"
    - "1;melee_attack;1.0;true"
  targets:
    - "0;hurt_target"
    - "1;nearest_attackable;player;true"

  behavior:
    tickInterval: 2
    goalProfiles:
      normal:
        goals:
          - "0;float"
          - "1;melee_attack;1.0;true"
        targets:
          - "0;hurt_target"
          - "1;nearest_attackable;player;true"
      berserk:
        goals:
          - "0;float"
          - "1;melee_attack;1.5;true"
        targets:
          - "0;hurt_target"
          - "1;nearest_attackable;player;true"

    initialState: normal
    states:
      normal:
        onEnter:
          - "apply_goal_profile;normal"
        transitions:
          enrage:
            to: berserk
            conditions:
              - "health_below_pct;30"

      berserk:
        onEnter:
          - "apply_goal_profile;berserk"
          - "set_cooldown;berserk_lock;80"
        onExit:
          - "reset_default_goals"
        transitions:
          calm:
            to: normal
            conditions:
              - "cooldown_ready;berserk_lock"
              - "health_above_pct;45"
```

For full ready-to-copy behavior examples, see `docs/behavior-ai-examples.md`.



