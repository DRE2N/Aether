package de.erethon.aether.tools.hitbox;

import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.util.BoundingBox;

public class RayTraceUtils {

    public static BoundingBox getHitboxFromEntity(Location location, EntityType type, Pose pose) {
        return null;
    }

    public static AABB toAAAB(BoundingBox box) {
        Vec3 maxVector = new Vec3(box.getMaxX(), box.getMaxY(), box.getMaxZ());
        Vec3 minVector = new Vec3(box.getMinX(), box.getMinY(), box.getMinZ());
        return new AABB(maxVector, minVector);
    }

    public static BoundingBox toBoundingBox(AABB box) {
        return new BoundingBox(box.maxX, box.maxY, box.maxZ, box.minX, box.minY, box.minZ);
    }

}
