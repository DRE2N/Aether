package de.erethon.aether.qxl.objectives;

import de.erethon.aether.creature.NPCData;
import de.erethon.aether.events.CreatureDeathEvent;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.objective.ActiveObjective;
import de.erethon.questsxl.objective.QBaseObjective;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@QLoadableDoc(
        value = "kill_mob",
        description = "Fulfilled when the player kills a specific mob. Allows defining multiple mobs.",
        shortExample = "kill_mob: mob=bandit,goblin; radius=3",
        longExample = {
                "kill_mob:",
                "  mobs:",
                "    - bandit",
                "    - goblin",
        }
)
public class KillMobObjective extends QBaseObjective<CreatureDeathEvent> {

    @QParamDoc(name = "mobs", description = "The IDs of the mobs that count towards this objective", required = true)
    private Set<String> mobs;
    @QParamDoc(name = "radius", description = "If set and on an event, the mob must be within the specified radius of the event  to count. Default: -1 (no radius check)")
    private int radius;

    @Override
    public void check(ActiveObjective active, CreatureDeathEvent event) {
        if (radius > 0) {
            if (active.getHolder() instanceof QEvent qEvent) {
                if (event.getKiller().getLocation().distance(qEvent.getLocation()) >= radius) {
                    return;
                }
            }
            return;
        }
        check(event.getNpc(), event.getKiller(), active);
    }

    private void check(NPCData npc, Player player, ActiveObjective active) {
        if (mobs.contains(npc.getID())) {
            checkCompletion(active, this, plugin.getDatabaseManager().getCurrentPlayer(player));
        }
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        String[] npcIds = cfg.getStringArray("mobs", new String[]{});
        mobs = new HashSet<>();
        Collections.addAll(mobs, npcIds);
        radius = cfg.getInt("radius", -1);
    }

    @Override
    public Class<CreatureDeathEvent> getEventType() {
        return CreatureDeathEvent.class;
    }
}
