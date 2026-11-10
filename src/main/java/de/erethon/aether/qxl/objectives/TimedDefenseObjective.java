package de.erethon.aether.qxl.objectives;

import de.erethon.aether.Aether;
import de.erethon.aether.creature.NPCData;
import de.erethon.aether.events.CreatureDeathEvent;
import de.erethon.aether.qxl.QxlDeathCredit;
import de.erethon.aether.qxl.QxlMobSpawner;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.script.QTranslatable;
import de.erethon.questsxl.component.objective.ActiveObjective;
import de.erethon.questsxl.component.objective.QBaseObjective;
import de.erethon.questsxl.content.ComponentValidationContributor;
import de.erethon.questsxl.content.ComponentNode;
import de.erethon.questsxl.content.ContentDiagnostic;
import de.erethon.questsxl.content.ContentKind;
import de.erethon.questsxl.content.ContentValidator;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.livingworld.defense.DefensePlan;
import de.erethon.questsxl.livingworld.defense.DefenseSite;
import de.erethon.questsxl.location.NamedLocation;
import de.erethon.questsxl.runtime.RuntimeStateSpec;
import net.minecraft.core.BlockPos;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

@QLoadableDoc(value = "timed_defense",
        description = "Defeat the scheduled attackers before the deadline while keeping losses below the defender limit. Use the Hermes timed defense template to configure it.",
        shortExample = "<YAML section required>",
        longExample = {"defense:", "  type: timed_defense", "  attackers: [Bandit]", "  approaches: [village.east]",
                "  defenders: [Villager]", "  defensePoints: [village.square]"})
public final class TimedDefenseObjective extends QBaseObjective<CreatureDeathEvent> implements ComponentValidationContributor {
    private static final RuntimeStateSpec<DefenseRun> STATE =
            RuntimeStateSpec.activation("timed-defense", ignored -> new DefenseRun());

    private DefensePlan.Definition definition;

    @Override
    public void load(QConfig config) {
        super.load(config);
        definition = DefensePlan.read(config instanceof ConfigurationSection section ? section : null);
        if (!(findTopParent() instanceof QEvent event) || event.isIncident()) {
            throw new IllegalArgumentException("Timed defense belongs to an authored event with named locations.");
        }
        var locations = plugin.getNamedLocationRegistry();
        definition.approaches().forEach(locations::registerReference);
        definition.defensePoints().forEach(locations::registerReference);
        if (definition.rallyPoint() != null) locations.registerReference(definition.rallyPoint());
    }

    @Override
    public List<ContentValidator<ComponentNode>> contentValidators() {
        return List.of((node, context, diagnostics) -> validate(node, diagnostics));
    }

    private void validate(ComponentNode node, Consumer<ContentDiagnostic> diagnostics) {
        if (node.source().kind() != ContentKind.EVENT) {
            diagnostics.accept(ContentDiagnostic.error("QXL-DEFENSE-SCOPE", "Timed defense belongs to a public event.",
                    node.source(), node.path()));
            return;
        }
        try {
            var plan = DefensePlan.read(node.configuration());
            validateRoster(plan.attackers(), "attackers", true, node, diagnostics);
            validateRoster(plan.defenders(), "defenders", false, node, diagnostics);
            var locations = plugin.getNamedLocationRegistry();
            if (locations != null && node.configuration().getRoot() != null) {
                var root = node.configuration().getRoot();
                var center = DefenseSite.authoredCenter(node.source().id(), root, locations);
                plan.validateSite(center, root.getInt("range", 32), locations::resolve);
            }
        } catch (DefensePlan.InvalidDefinition exception) {
            String path = exception.field().isEmpty() ? node.path() : node.path() + "." + exception.field();
            diagnostics.accept(ContentDiagnostic.error("QXL-DEFENSE", exception.getMessage(), node.source(), path));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            diagnostics.accept(ContentDiagnostic.error("QXL-DEFENSE-SITE", exception.getMessage(), node.source(), "startLocation"));
        }
    }

    private void validateRoster(List<String> ids, String field, boolean attackers, ComponentNode node,
                                Consumer<ContentDiagnostic> diagnostics) {
        var creatures = Aether.getInstance().getCreatureManager();
        for (String id : ids) {
            NPCData npc = creatures.getByID(id);
            if (npc == null || npc.isInvulnerable() || (attackers && npc.isInvulnerableToPlayers())) {
                diagnostics.accept(ContentDiagnostic.error("QXL-DEFENSE-ROSTER",
                        npc == null ? "Unknown Aether mob " + id + "." : id + " cannot take the required damage.",
                        node.source(), node.path() + "." + field));
            }
        }
    }

    @Override
    public Optional<RuntimeStateSpec<DefenseRun>> runtimeStateSpec() {
        return Optional.of(STATE);
    }

    @Override
    public void onStart(ActiveObjective active) {
        if (!(active.getHolder() instanceof QEvent event)) throw new IllegalArgumentException("Timed defense requires an event.");
        if (event.getCurrentObjectives().stream().anyMatch(other -> other != active && other.getObjective() instanceof TimedDefenseObjective)) {
            throw new IllegalArgumentException("A stage can run one timed defense at a time.");
        }
        DefenseRun run = active.state(STATE);
        int seed = definition.seed() == null ? ThreadLocalRandom.current().nextInt() : definition.seed();
        run.plan = definition.plan(seed);
        active.setProgressGoal(run.plan.killGoal());
        run.startTick = Bukkit.getCurrentTick();
        var locations = plugin.getNamedLocationRegistry();
        try {
            definition.validateSite(NamedLocation.fromBukkit(NamedLocation.eventId(event.getId(), "center"),
                    event.getLocation(), "", ""), event.getRange(), locations::resolve);
            for (String point : definition.approaches()) run.points.put(point, locations.resolve(point).toBukkitLocation());
            for (String point : definition.defensePoints()) run.points.put(point, locations.resolve(point).toBukkitLocation());
            if (definition.rallyPoint() != null) run.rally = locations.resolve(definition.rallyPoint()).toBukkitLocation();
        } catch (RuntimeException exception) {
            event.getTrace().add("error", "Invalid defense site: " + exception.getMessage());
            throw exception;
        }
        var tasks = active.getRuntimeScope();
        tasks.later(() -> spawn(active, event, run, run.plan.defenders(), false), 0);
        for (DefensePlan.Wave wave : run.plan.waves()) {
            tasks.later(() -> {
                if (spawn(active, event, run, wave.attackers(), true)) {
                    run.wavesSpawned++;
                    event.getTrace().add("wave", "Wave " + run.wavesSpawned + " spawned " + wave.attackers().size()
                            + " attackers at " + wave.attackers().getFirst().point());
                    updateTrace(active, event, run);
                }
            }, wave.atSeconds() * 20L);
        }
        tasks.later(() -> fail(event, "Deadline expired"), run.plan.deadlineSeconds() * 20L);
        tasks.repeating(() -> updateTrace(active, event, run), 20, 20);
        event.getTrace().add("plan", "Seed " + seed + " · " + run.plan.waves().size()
                + " waves · " + run.plan.killGoal() + " required kills");
        updateTrace(active, event, run);
    }

    private boolean spawn(ActiveObjective active, QEvent event, DefenseRun run, List<DefensePlan.Spawn> spawns, boolean attacker) {
        for (DefensePlan.Spawn spawn : spawns) {
            if (!active.isRunning() || !event.isCurrentStage(active.getStage())) return false;
            try {
                NPCData npc = Aether.getInstance().getCreatureManager().getByID(spawn.mob());
                if (npc == null || npc.isInvulnerable() || (attacker && npc.isInvulnerableToPlayers())) {
                    throw new IllegalArgumentException("Invalid defense mob " + spawn.mob());
                }
                Location point = run.points.get(spawn.point());
                var mob = QxlMobSpawner.spawnDefenseMob(npc, point, spawn.level(), event);
                run.actors.put(mob.getUUID(), attacker);
                Location home = attacker && run.rally != null ? run.rally : point;
                int homeRange = attacker && run.rally == null ? event.getRange() : 8;
                mob.setHomeTo(new BlockPos(home.getBlockX(), home.getBlockY(), home.getBlockZ()), homeRange);
            } catch (ReflectiveOperationException | RuntimeException exception) {
                fail(event, "Spawn failed for " + spawn.mob() + " at " + spawn.point() + " (" + exception.getMessage() + ")");
                return false;
            }
        }
        return true;
    }

    @Override
    public void check(ActiveObjective active, CreatureDeathEvent death) {
        if (!(active.getHolder() instanceof QEvent event) || !event.isCurrentStage(active.getStage())) return;
        DefenseRun run = active.state(STATE);
        var mob = death.getMob();
        String id = death.getNpc().getID();
        if (event.getRunId() == null || !event.getRunId().equals(mob.getEventRunId())) {
            if (definition.attackers().contains(id) || definition.defenders().contains(id)) {
                var location = mob.getBukkitEntity().getLocation();
                var center = event.getLocation();
                if (location.getWorld().equals(center.getWorld())
                        && location.distanceSquared(center) <= (double) event.getRange() * event.getRange()) {
                    event.getTrace().add("rejected", id + " was outside this defense run");
                }
            }
            return;
        }

        if (Bukkit.getCurrentTick() - run.startTick >= run.plan.deadlineSeconds() * 20) {
            fail(event, "Deadline expired");
            return;
        }
        Boolean attacker = run.actors.remove(mob.getUUID());
        if (attacker == null) {
            event.getTrace().add("rejected", id + " was outside this defense objective or already counted");
            return;
        }
        if (!attacker) {
            run.defendersLost++;
            event.getTrace().add("death", id + " counted as a defender loss");
            updateTrace(active, event, run);
            if (run.defendersLost >= run.plan.lossLimit()) fail(event, "Defender loss limit reached");
            return;
        }

        var contributors = QxlDeathCredit.contributors(death, event);
        var killer = death.getKiller() instanceof Player player ? getRelevantPlayer(active, player) : null;
        var instigator = killer == null ? event : killer;
        event.withObjectiveContributors(contributors, () -> {
            event.getTrace().add("death", id + " counted as an attacker death");
            contributors.forEach(player -> event.participate(player, 1));
            checkCompletion(active, this, instigator);
            if (active.isRunning()) updateTrace(active, event, run);
        });
    }

    private void fail(QEvent event, String reason) {
        event.getTrace().add("failure", reason);
        event.stop(true);
    }

    private void updateTrace(ActiveObjective active, QEvent event, DefenseRun run) {
        if (run.plan == null) return;
        int elapsed = (Bukkit.getCurrentTick() - run.startTick) / 20;
        event.getTrace().defense(run.plan, run.wavesSpawned, active.getProgress(), run.defendersLost,
                Math.max(0, run.plan.deadlineSeconds() - elapsed));
    }

    @Override
    public void onStop(ActiveObjective active) {
        if (active.getHolder() instanceof QEvent event) updateTrace(active, event, active.state(STATE));
    }

    @Override
    protected QTranslatable getDefaultDisplayText(Player player) {
        var qPlayer = plugin.getDatabaseManager().getCurrentPlayer(player);
        QEvent event = qPlayer == null ? null : qPlayer.getTrackedEvent();
        var active = event == null ? null : event.findObjective(this);
        if (active != null && active.isRunning()) {
            var run = active.state(STATE);
            if (run.plan != null) {
                int seconds = Math.max(0, run.plan.deadlineSeconds() - (Bukkit.getCurrentTick() - run.startTick) / 20);
                return QTranslatable.fromString("en=Defend · " + seconds + "s · " + run.defendersLost + "/" + run.plan.lossLimit()
                        + " lost;de=Verteidigen · " + seconds + "s · " + run.defendersLost + "/" + run.plan.lossLimit() + " verloren");
            }
        }
        return QTranslatable.fromString("en=Defend the position;de=Verteidige die Stellung");
    }

    @Override
    public Class<CreatureDeathEvent> getEventType() {
        return CreatureDeathEvent.class;
    }

    private static final class DefenseRun {
        private DefensePlan plan;
        private int startTick;
        private int wavesSpawned;
        private int defendersLost;
        private Location rally;
        private final Map<String, Location> points = new HashMap<>();
        private final Map<UUID, Boolean> actors = new HashMap<>();
    }
}
