package de.erethon.aether.qxl.actions;

import de.erethon.aether.Aether;
import de.erethon.aether.creature.AetherBaseMob;
import de.erethon.questsxl.action.QBaseAction;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.entity.Entity;

import java.util.Set;

@QLoadableDoc(
        value = "remove_mob",
        description = "Removes a mob with the specified tag from the world.",
        shortExample = "hide_mob: mobs=bandit; range=30",
        longExample = {
                "remove_mob:",
                "  mobs:",
                "    - bandit",
                "    - goblin",
                "  range: 50"
        }
)
public class RemoveMobAction extends QBaseAction {

    @QParamDoc(name = "mobs", description = "The tags of the mobs to remove", required = true)
    private Set<String> mobs;
    @QParamDoc(name = "range", description = "The range around the user to remove mobs from", def = "32")
    private double range = 32;

    @Override
    public void play(Quester quester) {
        if (!conditions(quester)) return;
        execute(quester, (player) -> {
            for (Entity entity : player.getPlayer().getNearbyEntities(range, range, range)) {
                if (entity instanceof CraftLivingEntity livingEntity && livingEntity.getHandle() instanceof AetherBaseMob mob) {
                    if (mob.getData().getID() == null) {
                        continue;
                    }
                    if (mobs.contains(mob.getData().getID())) {
                        mob.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                    }
                }
            }
        });
        onFinish(quester);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        String[] mobArray = cfg.getStringArray("mobs");
        if (mobArray == null || mobArray.length == 0) {
            throw new RuntimeException("No mobs specified for RemoveMobAction.");
        }
        mobs = Set.of(mobArray);
        range = cfg.getDouble("range", 32);
    }
}