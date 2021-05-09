package de.erethon.aether.tools;

import com.avaje.ebeaninternal.server.lib.util.MailEvent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import de.erethon.commons.chat.MessageUtil;
import net.minecraft.server.v1_16_R3.*;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.Optional;

public class NMSUtils {

    public static EntityArmorStand spawnInvisibleArmorstand(Location location, boolean invisible, boolean marker, boolean invulnerable, String name) {
        CraftWorld craftWorld = (CraftWorld) location.getWorld();
        World world = craftWorld.getHandle();
        EntityArmorStand stand = new EntityArmorStand(EntityTypes.ARMOR_STAND, world);
        stand.setInvisible(invisible);
        stand.collides = false;
        stand.setMarker(marker);
        stand.setInvulnerable(invulnerable);
        stand.getBukkitEntity().teleport(location);
        stand.getBukkitEntity().setCustomName(name);
        stand.setCustomNameVisible(true);
        world.addEntity(stand, CreatureSpawnEvent.SpawnReason.CUSTOM);
        return stand;
    }

    public static Entity spawnEntityWithoutSending(Location location, EntityType type) {
        CraftWorld craftWorld = (CraftWorld) location.getWorld();
        World world = craftWorld.getHandle();
        Optional<EntityTypes<?>> types = EntityTypes.getByName(type.getKey().asString());
        if (types.isPresent()) {
            EntityTypes<?> entityTypes = types.get();
            return entityTypes.create(world);
        }
        return null;
    }

    public static void addEntity(Entity entity, Location location) {
        entity.getBukkitEntity().teleport(location);
        World world = entity.getWorld();
        world.addEntity(entity);
    }

}

