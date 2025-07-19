package de.erethon.aether.qxl.objectives;

import de.erethon.aether.creature.NPCData;
import de.erethon.aether.events.CreatureDeathEvent;
import de.erethon.aether.events.InstancedCreatureDeathEvent;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.objective.ActiveObjective;
import de.erethon.questsxl.objective.QBaseObjective;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDeathEvent;

public class KillMobObjective extends QBaseObjective<CreatureDeathEvent> {

    private String mob;
    private int radius;

    @Override
    public void check(ActiveObjective active, CreatureDeathEvent event) {
        if (radius > 0) {
            if (active.getHolder() instanceof QEvent qEvent) {
                if (event.getKiller().getLocation().distance(qEvent.getLocation()) <= radius) {
                    return;
                }
            }
            return;
        }
        check(event.getNpc(), event.getKiller(), active);
    }

    private void check(NPCData npc, Player player, ActiveObjective active) {
        if (npc.getID().equals(mob)) {
            checkCompletion(active, this, plugin.getPlayerCache().getByPlayer(player));
        }
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        mob = cfg.getString("mob");
        if (mob == null) { // Legacy
            mob = cfg.getString("id");
        }
        radius = cfg.getInt("radius", -1);
    }

    @Override
    public Class<CreatureDeathEvent> getEventType() {
        return CreatureDeathEvent.class;
    }
}
