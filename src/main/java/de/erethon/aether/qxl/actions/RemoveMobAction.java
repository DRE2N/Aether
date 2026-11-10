package de.erethon.aether.qxl.actions;

import de.erethon.aether.creature.AetherBaseMob;
import de.erethon.aether.qxl.QxlInstanceScope;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.doc.QParamDoc;
import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.component.action.QBaseAction;
import de.erethon.questsxl.livingworld.QEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import java.util.Set;

@QLoadableDoc(
        value = "remove_mob",
        description = "Removes matching mobs owned by this event, or nearby unowned mobs when used outside an event.",
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

    @QParamDoc(name = "mobs", description = "Mob IDs or tags to remove", required = true)
    private Set<String> mobs;
    @QParamDoc(name = "range", description = "Distance from the event center or player. Defaults to all owned event mobs or 32 blocks around a player.")
    private Double range;

    @Override
    public void playInternal(Quester quester) {
        if (!conditions(quester)) return;
        QEvent event = eventContext(quester);
        if (event != null) {
            for (var mobId : event.getOwnedMobIds()) {
                Entity entity = Bukkit.getEntity(mobId);
                AetherBaseMob mob = QxlInstanceScope.getAetherMob(entity);
                if (mob != null && matches(mob) && withinEventRange(event, entity)) mob.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            }
        } else {
            double distance = range == null ? 32 : range;
            execute(quester, player -> {
                for (Entity entity : player.getPlayer().getNearbyEntities(distance, distance, distance)) {
                    AetherBaseMob mob = QxlInstanceScope.getAetherMob(entity);
                    if (mob != null && mob.getEventRunId() == null && matches(mob)
                            && QxlInstanceScope.canInteract(mob, player.getPlayer())) {
                        mob.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                    }
                }
            });
        }
        onFinish(quester);
    }

    private boolean withinEventRange(QEvent event, Entity entity) {
        if (range == null) return true;
        var center = event.getLocation();
        return center.getWorld().equals(entity.getWorld()) && center.distanceSquared(entity.getLocation()) <= range * range;
    }

    private boolean matches(AetherBaseMob mob) {
        return (mob.getData() != null && mobs.contains(mob.getData().getID()))
                || (mob.getMobTag() != null && mobs.contains(mob.getMobTag()));
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        String[] mobArray = cfg.getStringArray("mobs");
        if (mobArray == null || mobArray.length == 0) {
            throw new RuntimeException("No mobs specified for RemoveMobAction.");
        }
        mobs = Set.of(mobArray);
        if (cfg.contains("range")) range = cfg.getDouble("range");
        else if (cfg.contains("radius")) range = cfg.getDouble("radius");
    }
}
