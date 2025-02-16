package de.erethon.aether.qxl;

import de.erethon.aether.creature.AetherBaseMob;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.action.QAction;
import de.erethon.questsxl.common.QComponent;
import de.erethon.questsxl.common.QConfigLoader;
import de.erethon.questsxl.common.QRegistries;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.error.FriendlyError;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashSet;
import java.util.Set;

public class AetherHolder implements QComponent, Quester {

    private AetherBaseMob mob;

    private Set<QAction> rightClickActions = new HashSet<>();
    private Set<QAction> leftClickActions = new HashSet<>();
    private Set<QAction> deathActions = new HashSet<>();
    private Set<QAction> spawnActions = new HashSet<>();
    private Set<QAction> damageActions = new HashSet<>();
    private Set<QAction> attackActions = new HashSet<>();

    public void onDeath() {
        for (QAction action : deathActions) {
            try {
                action.play(this);
            } catch (Exception e) {
                FriendlyError error = new FriendlyError(mob.getData().getID(), "Failed to play death action", e.getMessage(), "Action: " + action.getClass().getSimpleName());
                error.addStacktrace(e.getStackTrace());
                QuestsXL.getInstance().addRuntimeError(error);
            }
        }
    }

    public void onRightClick(Quester quester) {
        for (QAction action : rightClickActions) {
            try {
                action.play(quester);
            } catch (Exception e) {
                FriendlyError error = new FriendlyError(mob.getData().getID(), "Failed to play rightClick action", e.getMessage(), "Action: " + action.getClass().getSimpleName());
                error.addStacktrace(e.getStackTrace());
                QuestsXL.getInstance().addRuntimeError(error);
            }
        }
    }

    public void onLeftClick(Quester quester) {
        for (QAction action : leftClickActions) {
            try {
                action.play(quester);
            } catch (Exception e) {
                FriendlyError error = new FriendlyError(mob.getData().getID(), "Failed to play leftClick action", e.getMessage(), "Action: " + action.getClass().getSimpleName());
                error.addStacktrace(e.getStackTrace());
                QuestsXL.getInstance().addRuntimeError(error);
            }
        }
    }

    public void onSpawn() {
        for (QAction action : spawnActions) {
            try {
                action.play(this);
            } catch (Exception e) {
                FriendlyError error = new FriendlyError(mob.getData().getID(), "Failed to play spawn action", e.getMessage(), "Action: " + action.getClass().getSimpleName());
                error.addStacktrace(e.getStackTrace());
                QuestsXL.getInstance().addRuntimeError(error);
            }
        }
    }

    public void onDamage(Quester quester) {
        for (QAction action : damageActions) {
            try {
                action.play(quester);
            } catch (Exception e) {
                FriendlyError error = new FriendlyError(mob.getData().getID(), "Failed to play damage action", e.getMessage(), "Action: " + action.getClass().getSimpleName());
                error.addStacktrace(e.getStackTrace());
                QuestsXL.getInstance().addRuntimeError(error);
            }
        }
    }

    public void onAttack(Quester quester) {
        for (QAction action : attackActions) {
            try {
                action.play(quester);
            } catch (Exception e) {
                FriendlyError error = new FriendlyError(mob.getData().getID(), "Failed to play attack action", e.getMessage(), "Action: " + action.getClass().getSimpleName());
                error.addStacktrace(e.getStackTrace());
                QuestsXL.getInstance().addRuntimeError(error);
            }
        }
    }

    public static AetherHolder loadFromConfigSection(ConfigurationSection section, AetherBaseMob mob) {
        AetherHolder holder = new AetherHolder();
        if (section == null) {
            return null;
        }
        String mobID = mob.getData().getID();
        holder.mob = mob;
        try {
            holder.rightClickActions = (Set<QAction>) QConfigLoader.load(holder, mobID, section.getConfigurationSection("rightClickActions"), QRegistries.ACTIONS);
            holder.leftClickActions = (Set<QAction>) QConfigLoader.load(holder, mobID, section.getConfigurationSection("leftClickActions"), QRegistries.ACTIONS);
            holder.deathActions = (Set<QAction>) QConfigLoader.load(holder, mobID, section.getConfigurationSection("deathActions"), QRegistries.ACTIONS);
            holder.spawnActions = (Set<QAction>) QConfigLoader.load(holder, mobID, section.getConfigurationSection("spawnActions"), QRegistries.ACTIONS);
            holder.damageActions = (Set<QAction>) QConfigLoader.load(holder, mobID, section.getConfigurationSection("damageActions"), QRegistries.ACTIONS);
            holder.attackActions = (Set<QAction>) QConfigLoader.load(holder, mobID, section.getConfigurationSection("attackActions"), QRegistries.ACTIONS);
        } catch (Exception e) {
            FriendlyError error = new FriendlyError(mob.getData().getID(), "Failed to load actions", e.getMessage(), "Mob ID: " + mobID);
            error.addStacktrace(e.getStackTrace());
            QuestsXL.getInstance().addRuntimeError(error);
        }
        return holder;
    }

    @Override
    public String getName() {
        return mob.getScoreboardName();
    }

    @Override
    public Location getLocation() {
        return mob.getBukkitEntity().getLocation();
    }
}
