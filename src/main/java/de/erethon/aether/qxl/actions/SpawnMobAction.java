package de.erethon.aether.qxl.actions;

import de.erethon.aether.Aether;
import de.erethon.aether.creature.AetherBaseMob;
import de.erethon.aether.creature.CreatureManager;
import de.erethon.aether.creature.NPCData;
import de.erethon.aether.qxl.QxlInstanceScope;
import de.erethon.aether.qxl.QxlMobSpawner;
import de.erethon.aether.spawning.EncounterSpawnPlacement;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.doc.QParamDoc;
import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.script.QLocation;
import de.erethon.questsxl.component.action.QBaseAction;
import de.erethon.questsxl.content.ComponentValidationContributor;
import de.erethon.questsxl.error.FriendlyError;
import de.erethon.questsxl.livingworld.QEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.Location;

import java.util.List;
import java.util.Set;

@QLoadableDoc(
        value = "spawn_mob",
        description = "Spawns a mob at a specified location.",
        shortExample = "spawn_mob: mob=bandit; x=~30; y=64; z=~-15; level=5",
        longExample = {
                "spawn_mob:",
                "  mob: bandit",
                "  tag: guard_leader",
                "  level: 69",
                "  homeRange: 32",
                "  location:",
                "    x: 1",
                "    y: 2",
                "    z: 3",
        }
)
public class SpawnMobAction extends QBaseAction implements ComponentValidationContributor {

    Aether aether = Aether.getInstance();
    CreatureManager creatureManager = aether.getCreatureManager();

    @QParamDoc(name ="mob", description = "The ID of the mob to spawn", required = true)
    NPCData npcData = null;
    @QParamDoc(name = "location", description = "The QLocation to spawn the mob at", required = true)
    QLocation location = null;
    @QParamDoc(name = "homeRange", description = "Set the home range around the spawn point. Defaults to the event range for event mobs. Use -1 for no home.")
    Integer homeRange;
    @QParamDoc(name = "level", description = "Override the mob's level. Default: -1 (no override)")
    int overrideLevel = -1;
    @QParamDoc(name ="amount", description = "The amount of mobs to spawn", def="1")
    int amount = 1;
    @QParamDoc(name = "tag" , description = "Tag the spawned mob with an ID to be able to reference it later")
    String tag = null;

    @Override
    public List<List<String>> requiredParameterGroups() {
        return List.of(List.of("mob", "id"));
    }

    @Override
    public void playInternal(Quester quester) {
        if (!conditions(quester)) return;
        QEvent event = eventContext(quester);
        for (int i = 0; i < amount; i++) {
            Location spawnLocation = location.get(quester.getLocation());
            if (event != null && event.isIncident()) {
                if (!spawnMob(spawnLocation, null, event)) {
                    event.stop(true);
                    return;
                }
                continue;
            }
            Set<String> scopes = QxlInstanceScope.getSpawnScopes(quester, spawnLocation);
            if (scopes.isEmpty()) {
                spawnMob(spawnLocation, null, event);
            } else {
                for (String scope : scopes) spawnMob(spawnLocation, scope, event);
            }
        }
        onFinish(quester);
    }

    private boolean spawnMob(Location spawnLocation, String instanceScope, QEvent event) {
        try {
            AetherBaseMob activeNPC = QxlMobSpawner.create(npcData, spawnLocation, overrideLevel);
            if (event != null && event.isIncident() && !EncounterSpawnPlacement.placeIncidentMob(
                    (ServerLevel) activeNPC.level(), activeNPC, spawnLocation)) {
                activeNPC.discard();
                Aether.log("Could not place " + npcData.getID() + " in incident " + event.getId());
                return false;
            }
            activeNPC.setInstanceScope(instanceScope);
            if (event != null && !activeNPC.attachToEvent(event)) return false;
            activeNPC.addToWorld();
            if (activeNPC.isRemoved()) return false;
            if (tag != null) {
                activeNPC.setMobTag(tag);
            }
            int effectiveHomeRange = homeRange != null ? homeRange : event == null ? -1 : event.getRange();
            if (effectiveHomeRange != -1) {
                activeNPC.setHomeTo(new BlockPos((int) activeNPC.getX(), (int) activeNPC.getY(),
                        (int) activeNPC.getZ()), effectiveHomeRange);
            }
            return true;
        } catch (Exception e) {
            FriendlyError error = new FriendlyError(id, "Failed to spawn mob", e.getMessage(),
                    "Mob ID: " + npcData.getID()).addStacktrace(e.getStackTrace());
            QuestsXL.get().addRuntimeError(error);
            return false;
        }
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        location = cfg.getQLocation("location");
        npcData = creatureManager.getByID(cfg.getString("mob"));
        overrideLevel = cfg.getInt("level", -1);
        tag = cfg.getString("tag", null);
        homeRange = cfg.contains("homeRange") ? cfg.getInt("homeRange") : null;
        amount = cfg.getInt("amount", 1);
        if (npcData == null) { // Legacy support
            npcData = creatureManager.getByID(cfg.getString("id"));
        }
        if (npcData == null) {
            throw new IllegalArgumentException("NPCData with id " + cfg.getString("mob") + " not found.");
        }
    }
}
