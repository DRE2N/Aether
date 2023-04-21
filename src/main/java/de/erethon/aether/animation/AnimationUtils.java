package de.erethon.aether.animation;

import net.minecraft.world.entity.LivingEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftLivingEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class AnimationUtils {

    public static void nod(Entity entity) {
        Mob mob = (Mob) entity;
        Bukkit.getMobGoals().removeAllGoals(mob);
        Vector down = entity.getLocation().getDirection();
        down.multiply(3);
        down.add(entity.getLocation().toVector());
        down.setY(down.getY() - 5);
        down.toLocation(entity.getWorld()).getBlock().setType(Material.REDSTONE_BLOCK);

    }

    public static void sendAnimation(Entity entity, Player player, EntityAnimation anim) {
        //WrapperPlayServerAnimation animation = new WrapperPlayServerAnimation();
        //animation.setEntityID(entity.getEntityId());
        int id = -1;
        switch (anim) {
            case SWAP_HANDS:
                sendStatus(entity, player, Integer.valueOf(55).byteValue());
                return;
            case SWING_MAIN_HAND:
                id = 0;
                break;
            case SWING_OFF_HAND:
                id = 3;
                break;
            case HURT:
                id = 1;
                break;
            case CHORUS:
                break;
            case CRITICAL:
                id = 4;
                break;
            case LEAVE_BED:
                id = 2;
                break;
            case MAGIC_CRITICAL:
                id = 5;
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
        if (id != -1) {
        }


    }

    // Some animations are a status for whatever reason, so they use a different packet
    private static void sendStatus(Entity entity, Player player, byte id) {

    }
}