package de.erethon.aether.qxl.objectives;

import de.erethon.aether.creature.NPCData;
import de.erethon.aether.events.CreatureDeathEvent;
import de.erethon.aether.events.CreatureInteractEvent;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.QTranslatable;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.objective.ActiveObjective;
import de.erethon.questsxl.objective.QBaseObjective;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@QLoadableDoc(
        value = "interact_mob",
        description = "Objective that requires the player to interact with a specific mob. Currently only right-clicking is supported.",
        shortExample = "interact_mob: mob=bandit",
        longExample = {
                "interact_mob:",
                "  mob: bandit",
                "  tag: quest_bandit_1"
        }
)
public class InteractMobObjective extends QBaseObjective<CreatureInteractEvent> {

    @QParamDoc(name = "mos", description = "The ID of the mob that counts towards this objective", required = true)
    private String mob;
    @QParamDoc(name = "tag", description = "If set, only the mob with the specified tag will count towards this objective", required = false)
    private String tag;

    @Override
    public void check(ActiveObjective active, CreatureInteractEvent event) {
        NPCData data = event.getData();
        if (data == null) return;
        if (!data.getID().equalsIgnoreCase(mob)) return;
        if (tag != null) {
            String eventTag = event.getMob().getMobTag();
            if (eventTag == null || !eventTag.equalsIgnoreCase(tag)) return;
        }
        checkCompletion(active, this, plugin.getDatabaseManager().getCurrentPlayer(event.getPlayer()));
    }

    @Override
    protected QTranslatable getDefaultDisplayText(Player player) {
        return QTranslatable.fromString("de=Interagiere mit " + (mob != null ? mob : "einem Mob") + "; en=Interact with " + (mob != null ? mob : "a mob"));
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        mob = cfg.getString("mob", null);
        tag = cfg.getString("tag", null);
        if (mob == null) {
            throw new IllegalArgumentException("mob is required");
        }
    }

    @Override
    public Class<CreatureInteractEvent> getEventType() {
        return CreatureInteractEvent.class;
    }
}
