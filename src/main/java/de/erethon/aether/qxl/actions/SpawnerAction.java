package de.erethon.aether.qxl.actions;

import de.erethon.aether.Aether;
import de.erethon.aether.spawning.SpawnerManager;
import de.erethon.questsxl.action.QBaseAction;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;

@QLoadableDoc(
        value = "spawner",
        description = "Triggers a spawner to spawn its mobs.",
        shortExample = "spawner: spawner=bandit_spawner_1",
        longExample = {
                "spawner:",
                "  spawner: bandit_spawner_1",
        }
)
public class SpawnerAction extends QBaseAction {

    private final SpawnerManager spawnerManager = Aether.getInstance().getSpawnerManager();

    @QParamDoc(name = "spawner", description = "The ID of the spawner to trigger", required = true)
    private String spawnerID;

    @Override
    public void play(Quester quester) {
        if (!conditions(quester)) return;
        spawnerManager.triggerSpawner(spawnerID);
        onFinish(quester);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        spawnerID = cfg.getString("spawner");
    }
}
