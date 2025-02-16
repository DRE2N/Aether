package de.erethon.aether.qxl.actions;

import de.erethon.aether.Aether;
import de.erethon.aether.creature.AetherBaseMob;
import de.erethon.aether.creature.CreatureManager;
import de.erethon.aether.creature.NPCData;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.action.QBaseAction;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLocation;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.error.FriendlyError;
import org.bukkit.Location;

public class SpawnMobAction extends QBaseAction {

    Aether aether = Aether.getInstance();
    CreatureManager creatureManager = aether.getCreatureManager();

    NPCData npcData = null;
    QLocation location = null;


    @Override
    public void play(Quester quester) {
        if (!conditions(quester)) return;
        Location pLocation = quester.getLocation();
        try {
            AetherBaseMob mob = npcData.spawn(location.get(pLocation));
            mob.setPos(location.getX(pLocation), location.getY(pLocation), location.getZ(pLocation));
            mob.addToWorld();
        }
        catch (Exception e) {
            FriendlyError error = new FriendlyError(id,"Failed to spawn mob", e.getMessage(), "Mob ID: " + npcData.getID()).addStacktrace(e.getStackTrace());
            QuestsXL.getInstance().addRuntimeError(error);
        }
        onFinish(quester);
    }


    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        location = cfg.getQLocation("location");
        npcData = creatureManager.getByID(cfg.getString("mob"));
        if (npcData == null) { // Legacy support
            npcData = creatureManager.getByID(cfg.getString("id"));
        }
        if (npcData == null) {
            throw new IllegalArgumentException("NPCData with id " + cfg.getString("mob") + " not found.");
        }
    }
}
