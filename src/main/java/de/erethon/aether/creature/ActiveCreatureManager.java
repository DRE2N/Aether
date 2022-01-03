package de.erethon.aether.creature;

import de.erethon.aether.Aether;
import de.erethon.aether.tools.NMSUtils;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ActiveCreatureManager {

    private static final char healthBar = '|';

    HashMap<UUID, ActiveNPC> globalNPCs = new HashMap<>(16);
    HashMap<Player, HashMap<String, InstancedNPC>> instancedNPCs = new HashMap<>();
    HashMap<Entity, Entity> healthBars = new HashMap<>(16);
    HashMap<Entity, List<Entity>> textBars = new HashMap<>(8);

    public ActiveCreatureManager() {
        ambientSoundTask.runTaskTimer(Aether.getInstance(), 20, 40);
    }

    BukkitRunnable ambientSoundTask = new BukkitRunnable() {
        @Override
        public void run() {
            for (ActiveNPC npc : globalNPCs.values())
                npc.playAmbientSound();
            }
        };

    public ActiveNPC get(UUID uuid) {
        return globalNPCs.get(uuid);
    }

    public void remove(UUID uuid) {
        globalNPCs.remove(uuid);
    }


    public InstancedNPC getInstanced(Player player, String id) {
        return instancedNPCs.get(player).get(id);
    }

    public void addActive(Entity entity, ActiveNPC npc) {
        globalNPCs.put(entity.getUniqueId(), npc);
    }

    public String getHealthMessage(double health, double max) { // maybe with color based on damage type?
        double ratio = health / max;
        double missing = 1 - ratio;
        int missingBars = (int) Math.round(missing * 20);
        int healthBars = (int) Math.round(ratio * 20);
        String missingMsg = "";
        String healthMsg = "";
        while (missingBars != 0) {
            missingMsg = missingMsg + "§8" + healthBar;
            missingBars--;
        }
        while (healthBars != 0) {
            healthMsg = healthMsg + "§4" + healthBar;
            healthBars--;
        }
        ChatColor healthColor;
        if (ratio >= 0.5) {
            healthColor = ChatColor.DARK_RED;
        } else {
            healthColor = ChatColor.DARK_GRAY;
        }
        String msg =  healthMsg + missingMsg;
        String begin = msg.substring(0, msg.length() / 2);
        String end = msg.substring(msg.length() / 2);
        return  begin + healthColor + Math.toIntExact(Math.round(health)) + end;
    }

    public void showHealth(Entity entity) {
        LivingEntity livingEntity = (LivingEntity)  entity;
        if (healthBars.get(entity) == null) {
            ArmorStand stand = NMSUtils.spawnInvisibleArmorstand(entity.getLocation().clone().add(0, 1.68, 0), true, true, true, getHealthMessage(((LivingEntity) entity).getHealth(), ((LivingEntity) entity).getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()));
            healthBars.put(entity, stand.getBukkitEntity());
            return;
        }
        healthBars.get(livingEntity).setCustomName(getHealthMessage(((LivingEntity) entity).getHealth(), ((LivingEntity) entity).getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()));
    }

    public void removeHealthbar(Entity entity) {
        if (!healthBars.containsKey(entity)) {
            return;
        }
        healthBars.get(entity).remove();
        healthBars.remove(entity);
    }

    public void clearHealthBars() {
        for (Map.Entry<Entity, Entity> entity : healthBars.entrySet()) {
            entity.getValue().remove();
        }
    }

    public void updateHealthbarPosition(Entity entity) {
        if (healthBars.containsKey(entity)) {
            healthBars.get(entity).teleport(entity.getLocation().clone().add(0, 1.68, 0));
        }
    }

    public void updateTextPosition(Entity entity) {
        if (textBars.containsKey(entity)) {
            for (Entity e : textBars.get(entity)) {
            }
        }
    }

    public HashMap<UUID, ActiveNPC> getGlobalNPCs() {
        return globalNPCs;
    }
}
