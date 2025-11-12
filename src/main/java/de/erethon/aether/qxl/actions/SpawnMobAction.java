package de.erethon.aether.qxl.actions;

import de.erethon.aether.Aether;
import de.erethon.aether.creature.AetherBaseMob;
import de.erethon.aether.creature.CreatureManager;
import de.erethon.aether.creature.NPCData;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.action.QBaseAction;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QLocation;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.error.FriendlyError;
import de.erethon.questsxl.livingworld.QEvent;
import net.minecraft.core.BlockPos;
import org.bukkit.Location;
import org.bukkit.World;

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
public class SpawnMobAction extends QBaseAction {

    Aether aether = Aether.getInstance();
    CreatureManager creatureManager = aether.getCreatureManager();

    @QParamDoc(name ="mob", description = "The ID of the mob to spawn", required = true)
    NPCData npcData = null;
    @QParamDoc(name = "location", description = "The QLocation to spawn the mob at", required = true)
    QLocation location = null;
    @QParamDoc(name = "homeRange", description = "Set the home range of the mob. The mob will try to stay within this range of the spawn location. Default: -1 (no home)")
    int homeRange = -1;
    @QParamDoc(name = "level", description = "Override the mob's level. Default: -1 (no override)")
    int overrideLevel = -1;
    @QParamDoc(name ="amount", description = "The amount of mobs to spawn", def="1")
    int amount = 1;
    @QParamDoc(name = "tag" , description = "Tag the spawned mob with an ID to be able to reference it later")
    String tag = null;

    @Override
    public void play(Quester quester) {
        if (!conditions(quester)) return;
        Location pLocation = quester.getLocation();
        for (int i = 0; i < amount; i++) {
            try {
                Class<? extends AetherBaseMob> toSpawn = npcData.getEntityClass();
                AetherBaseMob activeNPC;
                if (overrideLevel == -1) {
                    activeNPC = toSpawn.getConstructor(NPCData.class, World.class).newInstance(npcData, pLocation.getWorld());
                } else {
                    activeNPC = toSpawn.getConstructor(NPCData.class, World.class, Integer.class).newInstance(npcData, pLocation.getWorld(), overrideLevel);
                }
                activeNPC.setPos(location.getX(pLocation), location.getY(pLocation), location.getZ(pLocation));
                activeNPC.addToWorld();
                if (tag != null) {
                    activeNPC.setMobTag(tag);
                }
                if (quester instanceof QEvent event) {
                    homeRange = event.getRange();
                }
                if (homeRange != -1) {
                    activeNPC.setHomeTo(new BlockPos((int) activeNPC.getX(), (int) activeNPC.getY(), (int) activeNPC.getZ()), homeRange);
                }
            }
            catch (Exception e) {
                FriendlyError error = new FriendlyError(id, "Failed to spawn mob", e.getMessage(), "Mob ID: " + npcData.getID()).addStacktrace(e.getStackTrace());
                QuestsXL.get().addRuntimeError(error);
            }
        }
        onFinish(quester);
    }


    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        location = cfg.getQLocation("location");
        npcData = creatureManager.getByID(cfg.getString("mob"));
        overrideLevel = cfg.getInt("level", -1);
        tag = cfg.getString("tag", null);
        homeRange = cfg.getInt("homeRange", -1);
        amount = cfg.getInt("amount", 1);
        if (npcData == null) { // Legacy support
            npcData = creatureManager.getByID(cfg.getString("id"));
        }
        if (npcData == null) {
            throw new IllegalArgumentException("NPCData with id " + cfg.getString("mob") + " not found.");
        }
    }
}
