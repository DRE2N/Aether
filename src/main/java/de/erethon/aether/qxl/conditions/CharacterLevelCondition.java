package de.erethon.aether.qxl.conditions;

import de.erethon.hecate.Hecate;
import de.erethon.hecate.data.DatabaseManager;
import de.erethon.hecate.data.HPlayer;
import de.erethon.hecate.progression.LevelUtil;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.condition.QBaseCondition;
import de.erethon.questsxl.player.QPlayer;

import java.util.concurrent.CompletableFuture;

@QLoadableDoc(
        value = "character_level",
        description = "Checks if the player's selected character's level is within the specified range.",
        shortExample = "character_level: minLevel=5; maxLevel=10",
        longExample = {
                "character_level:",
                "  minLevel: 5",
                "  maxLevel: 10",
        }
)
public class CharacterLevelCondition extends QBaseCondition {

    private final Hecate hecate = Hecate.getInstance();
    private final DatabaseManager databaseManager = hecate.getDatabaseManager();

    @QParamDoc(name = "minLevel", description = "The minimum character level (inclusive).", def = "0")
    private int minLevel;
    @QParamDoc(name = "maxLevel", description = "The maximum character level (inclusive).", def = "a lot")
    private int maxLevel;

    @Override
    public boolean check(Quester quester) {
        if (!(quester instanceof QPlayer player)) {
            return fail(quester);
        }
        HPlayer hPlayer = databaseManager.getHPlayer(player.getPlayer());
        if (hPlayer == null || hPlayer.getSelectedCharacter() == null) {
            return fail(quester);
        }
        CompletableFuture<Integer> levelFuture = LevelUtil.getCharacterLevel(hPlayer.getSelectedCharacter());
        int level = levelFuture.join();
        if (level >= minLevel && level <= maxLevel) {
            return success(quester);
        }
        return fail(quester);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        minLevel = cfg.getInt("minLevel", 0);
        maxLevel = cfg.getInt("maxLevel", Integer.MAX_VALUE);
    }
}
