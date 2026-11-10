package de.erethon.aether.qxl;

import de.erethon.aether.creature.AetherBaseMob;
import de.erethon.aether.creature.NPCData;
import de.erethon.aether.spawning.EncounterSpawnPlacement;
import de.erethon.questsxl.livingworld.QEvent;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.Location;
import org.bukkit.World;

public final class QxlMobSpawner {
    private QxlMobSpawner() {}

    public static AetherBaseMob create(NPCData data, Location location, int level) throws ReflectiveOperationException {
        Class<? extends AetherBaseMob> type = data.getEntityClass();
        AetherBaseMob mob = level < 0
                ? type.getConstructor(NPCData.class, World.class).newInstance(data, location.getWorld())
                : type.getConstructor(NPCData.class, World.class, Integer.class).newInstance(data, location.getWorld(), level);
        mob.setPos(location.getX(), location.getY(), location.getZ());
        return mob;
    }

    public static AetherBaseMob spawnDefenseMob(NPCData data, Location location, int level, QEvent event)
            throws ReflectiveOperationException {
        AetherBaseMob mob = create(data, location, level);
        try {
            Location center = event.getLocation();
            double rangeSquared = (double) event.getRange() * event.getRange();
            if (!EncounterSpawnPlacement.placeAtAuthoredPoint((ServerLevel) mob.level(), mob, location,
                    point -> point.getWorld().equals(center.getWorld()) && point.distanceSquared(center) <= rangeSquared)) {
                throw new IllegalStateException("No safe, ticking spawn position within three blocks of the approved point.");
            }
            if (!mob.attachToEvent(event)) throw new IllegalStateException("Event is no longer active.");
            mob.addToWorld();
            if (!mob.getBukkitEntity().isValid()) throw new IllegalStateException("The server rejected spawn of " + data.getID());
            return mob;
        } catch (RuntimeException exception) {
            event.forgetMob(mob.getUUID());
            mob.remove(RemovalReason.DISCARDED);
            throw exception;
        }
    }
}
