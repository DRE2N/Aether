package de.erethon.aether.tools.hitbox;

import net.minecraft.server.v1_16_R3.*;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_16_R3.util.CraftNamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.util.BoundingBox;

public class RayTraceUtils {

    public static BoundingBox getHitboxFromEntity(Location location, EntityType type, EntityPose pose) {
        BoundingBox box = null;
        EntityTypes<?> entity = IRegistry.ENTITY_TYPE.get(CraftNamespacedKey.toMinecraft(type.getKey()));
        EntitySize entitysize = entity.l();
        float f = entitysize.width / 2.0F;
        Vec3D vec3d = new Vec3D(location.getX() - (double) f, location.getY(), location.getZ() - (double) f);
        Vec3D vec3d1 = new Vec3D(location.getX() + (double)f, location.getY() + (double)entitysize.height, location.getZ() + (double)f);
        return toBoundingBox(new AxisAlignedBB(vec3d, vec3d1));
    }

    public static AxisAlignedBB toAAAB(BoundingBox box) {
        Vec3D maxVector = new Vec3D(box.getMaxX(), box.getMaxY(), box.getMaxZ());
        Vec3D minVector = new Vec3D(box.getMinX(), box.getMinY(), box.getMinZ());
        return new AxisAlignedBB(maxVector, minVector);
    }

    public static BoundingBox toBoundingBox(AxisAlignedBB box) {
        return new BoundingBox(box.maxX, box.maxY, box.maxZ, box.minX, box.minY, box.minZ);
    }

}
