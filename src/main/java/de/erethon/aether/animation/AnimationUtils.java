package de.erethon.aether.animation;

import io.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.packetwrappers.play.out.animation.WrappedPacketOutAnimation;
import io.github.retrooper.packetevents.packetwrappers.play.out.entitystatus.WrappedPacketOutEntityStatus;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftLivingEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class AnimationUtils {

    public static void lookAt(Entity entity, Location target) {
        LivingEntity livingEntity = (LivingEntity) entity;
        CraftLivingEntity ce = (CraftLivingEntity) livingEntity;
        net.minecraft.world.entity.LivingEntity insentient = (net.minecraft.world.entity.LivingEntity) ce.getHandle();
    }

    public static void nod(Entity entity) {
        Mob mob = (Mob) entity;
        Bukkit.getMobGoals().removeAllGoals(mob);
        Vector down = entity.getLocation().getDirection();
        down.multiply(3);
        down.add(entity.getLocation().toVector());
        down.setY(down.getY() - 5);
        down.toLocation(entity.getWorld()).getBlock().setType(Material.REDSTONE_BLOCK);
        lookAt(entity, down.toLocation(entity.getWorld()));

    }

    public static void sendAnimation(Entity entity, Player player, EntityAnimation anim) {
        WrappedPacketOutAnimation.EntityAnimationType animation = null;
        switch (anim) {
            case SWAP_HANDS:
                sendStatus(entity, player, Integer.valueOf(55).byteValue());
                return;
            case SWING_MAIN_HAND:
                animation = WrappedPacketOutAnimation.EntityAnimationType.SWING_MAIN_ARM;
                break;
            case SWING_OFF_HAND:
                animation = WrappedPacketOutAnimation.EntityAnimationType.SWING_OFFHAND;
                break;
            case HURT:
                animation = WrappedPacketOutAnimation.EntityAnimationType.TAKE_DAMAGE;
                break;
            case CHORUS:
                break;
            case CRITICAL:
                animation = WrappedPacketOutAnimation.EntityAnimationType.CRITICAL_EFFECT;
                break;
            case LEAVE_BED:
                animation = WrappedPacketOutAnimation.EntityAnimationType.LEAVE_BED;
                break;
            case MAGIC_CRITICAL:
                animation = WrappedPacketOutAnimation.EntityAnimationType.MAGIC_CRITICAL_EFFECT;
                break;
            case DEATH:
                sendStatus(entity, player, Integer.valueOf(3).byteValue());
                return;
            case TOTEM:
                sendStatus(entity, player, Integer.valueOf(35).byteValue());
                return;
            case EAT_GRASS:
                sendStatus(entity, player, Integer.valueOf(10).byteValue());
                return;
            case WOLF_SHAKING:
                sendStatus(entity, player, Integer.valueOf(8).byteValue());
                return;
            case EQUIPMENT_BREAK:
                sendStatus(entity, player, Integer.valueOf(47).byteValue());
                return;
            case HONEY:
                sendStatus(entity, player, Integer.valueOf(54).byteValue());
                return;
            case HURT_BERRY:
                sendStatus(entity, player, Integer.valueOf(44).byteValue());
                return;
            case HURT_BURN:
                sendStatus(entity, player, Integer.valueOf(37).byteValue());
                return;
            case HURT_DROWN:
                sendStatus(entity, player, Integer.valueOf(36).byteValue());
                return;
            case HURT_THORNS:
                sendStatus(entity, player, Integer.valueOf(33).byteValue());
                return;
            case SHIELD_BLOCK:
                sendStatus(entity, player, Integer.valueOf(29).byteValue());
                return;
            case SHIELD_BREAK:
                sendStatus(entity, player, Integer.valueOf(30).byteValue());
                return;
        }
        WrappedPacketOutAnimation packet = new WrappedPacketOutAnimation(entity, animation);
        PacketEvents.get().getPlayerUtils().sendPacket(player, packet);



    }

    // Some animations are a status for whatever reason, so they use a different packet
    private static void sendStatus(Entity entity, Player player, byte id) {
        WrappedPacketOutEntityStatus status = new WrappedPacketOutEntityStatus(entity, id);
        PacketEvents.get().getPlayerUtils().sendPacket(player, status);
    }
}
