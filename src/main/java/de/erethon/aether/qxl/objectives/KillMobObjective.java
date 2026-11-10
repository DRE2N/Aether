package de.erethon.aether.qxl.objectives;

import de.erethon.aether.events.CreatureDeathEvent;
import de.erethon.aether.qxl.QxlDeathCredit;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.content.ComponentValidationContributor;
import de.erethon.questsxl.common.doc.QParamDoc;
import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.script.QTranslatable;
import de.erethon.questsxl.component.objective.ActiveObjective;
import de.erethon.questsxl.component.objective.QBaseObjective;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

@QLoadableDoc(
        value = "kill_mob",
        description = "Fulfilled when the player kills a specific mob. Allows defining multiple mobs.",
        shortExample = "kill_mob: mobs=bandit,goblin; radius=3",
        longExample = {
                "kill_mob:",
                "  mobs:",
                "    - bandit",
                "    - goblin",
        }
)
public class KillMobObjective extends QBaseObjective<CreatureDeathEvent> implements ComponentValidationContributor {

    @QParamDoc(name = "mobs", description = "The IDs of the mobs that count towards this objective", required = true)
    private Set<String> mobs;
    @QParamDoc(name = "radius", description = "Distance from the event center. Ambient mobs use the event range by default.")
    private int radius;
    @QParamDoc(name = "ambient", description = "Also count unowned public mobs inside this event's area.", def = "false")
    private boolean ambient;

    @Override
    public List<List<String>> requiredParameterGroups() {
        return List.of(List.of("mobs", "mob"));
    }

    @Override
    public void check(ActiveObjective active, CreatureDeathEvent death) {
        String mobId = death.getNpc().getID();
        if (!mobs.contains(mobId.toLowerCase(Locale.ROOT))) return;
        var mob = death.getMob();
        if (active.getHolder() instanceof QEvent event) {
            UUID runId = event.getRunId();
            if (runId == null || !event.isCurrentStage(active.getStage())) return;
            Location deathLocation = mob.getBukkitEntity().getLocation();
            Location center = event.getLocation();
            if (!deathLocation.getWorld().equals(center.getWorld())) return;
            double distanceSquared = deathLocation.distanceSquared(center);
            boolean owned = runId.equals(mob.getEventRunId());
            if (!owned && (mob.getEventRunId() != null || !ambient || mob.getInstanceScope() != null)) {
                if (distanceSquared <= (double) event.getRange() * event.getRange()) {
                    event.getTrace().add("rejected", mobId + " did not belong to this event run");
                }
                return;
            }
            int distance = radius > 0 ? radius : owned ? 0 : event.getRange();
            if (distance > 0 && distanceSquared > (double) distance * distance) {
                event.getTrace().add("rejected", mobId + " died outside the objective radius");
                return;
            }

            Set<QPlayer> contributors = QxlDeathCredit.contributors(death, event);
            QPlayer killer = death.getKiller() instanceof Player player ? getRelevantPlayer(active, player) : null;
            event.withObjectiveContributors(contributors, () -> {
                var instigator = killer == null ? event : killer;
                if (conditions(event, instigator)) {
                    event.getTrace().add("death", mobId + " counted for " + active.getComponentPath());
                    checkCompletion(active, this, instigator);
                } else {
                    event.getTrace().add("rejected", mobId + " did not meet the objective conditions");
                }
            });
            return;
        }
        if (death.getKiller() instanceof Player player) {
            QPlayer owner = getRelevantPlayer(active, player);
            if (owner != null && conditions(active.getHolder(), owner)) checkCompletion(active, this, owner);
        }
    }

    @Override
    protected QTranslatable getDefaultDisplayText(Player player) {
        return QTranslatable.fromString("de=Töte Mob; en=Kill mob");
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        String[] npcIds = cfg.getStringArray("mobs", cfg.getStringArray("mob", new String[0]));
        mobs = new HashSet<>();
        for (String id : npcIds) {
            if (!id.isBlank()) mobs.add(id.trim().toLowerCase(Locale.ROOT));
        }
        if (mobs.isEmpty()) throw new IllegalArgumentException("No mob IDs specified");
        radius = cfg.getInt("radius", cfg.getInt("range", -1));
        ambient = cfg.getBoolean("ambient", false);
    }


    @Override
    public Class<CreatureDeathEvent> getEventType() {
        return CreatureDeathEvent.class;
    }
}
