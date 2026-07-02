package de.erethon.aether.qxl;

import de.erethon.aether.creature.AetherBaseMob;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QComponent;
import de.erethon.questsxl.common.QRegistries;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.common.script.QConfigLoader;
import de.erethon.questsxl.component.action.QAction;
import de.erethon.questsxl.component.condition.QCondition;
import de.erethon.questsxl.error.FriendlyError;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class AetherHolder implements QComponent, Quester {

    private AetherBaseMob mob;

    private Set<QCondition> visibilityConditions = new HashSet<>();
    private Set<QCondition> spawnConditions = new HashSet<>();
    private Set<QAction> rightClickActions = new HashSet<>();
    private Set<QAction> leftClickActions = new HashSet<>();
    private Set<QAction> deathActions = new HashSet<>();
    private Set<QAction> spawnActions = new HashSet<>();
    private Set<QAction> damageActions = new HashSet<>();
    private Set<QAction> attackActions = new HashSet<>();

    public boolean hasLoaded(String path) {
        return switch (path) {
            case "visibilityConditions" -> visibilityConditions != null && !visibilityConditions.isEmpty();
            case "spawnConditions" -> spawnConditions != null && !spawnConditions.isEmpty();
            case "rightClickActions" -> rightClickActions != null && !rightClickActions.isEmpty();
            case "leftClickActions" -> leftClickActions != null && !leftClickActions.isEmpty();
            case "deathActions" -> deathActions != null && !deathActions.isEmpty();
            case "spawnActions" -> spawnActions != null && !spawnActions.isEmpty();
            case "damageActions" -> damageActions != null && !damageActions.isEmpty();
            case "attackActions" -> attackActions != null && !attackActions.isEmpty();
            default -> false;
        };
    }

    public boolean checkVisibilityConditions(Player player) {
        QPlayer qPlayer = QuestsXL.get().getDatabaseManager().getCurrentPlayer(player);
        if (qPlayer == null) {
            return false;
        }
        for (QCondition condition : visibilityConditions) {
            if (!condition.check(qPlayer)) {
                return false;
            }
        }
        return true;
    }

    public boolean checkSpawnConditions() {
        for (QCondition condition : spawnConditions) {
            if (!condition.check(this)) {
                return false;
            }
        }
        return true;
    }

    public void onDeath() {
        if (deathActions == null || deathActions.isEmpty()) {
            return;
        }
        for (QAction action : deathActions) {
            try {
                action.play(this);
            } catch (Exception e) {
                FriendlyError error = new FriendlyError(mob.getData().getID(), "Failed to play death action", e.getMessage(), "Action: " + action.getClass().getSimpleName());
                error.addStacktrace(e.getStackTrace());
                QuestsXL.get().addRuntimeError(error);
            }
        }
    }

    public void onRightClick(Quester quester) {
        if (rightClickActions == null || rightClickActions.isEmpty()) {
            return;
        }
        for (QAction action : rightClickActions) {
            try {
                action.play(quester);
            } catch (Exception e) {
                FriendlyError error = new FriendlyError(mob.getData().getID(), "Failed to play rightClick action", e.getMessage(), "Action: " + action.getClass().getSimpleName());
                error.addStacktrace(e.getStackTrace());
                QuestsXL.get().addRuntimeError(error);
            }
        }
    }

    public void onLeftClick(Quester quester) {
        if (leftClickActions == null || leftClickActions.isEmpty()) {
            return;
        }
        for (QAction action : leftClickActions) {
            try {
                action.play(quester);
            } catch (Exception e) {
                FriendlyError error = new FriendlyError(mob.getData().getID(), "Failed to play leftClick action", e.getMessage(), "Action: " + action.getClass().getSimpleName());
                error.addStacktrace(e.getStackTrace());
                QuestsXL.get().addRuntimeError(error);
            }
        }
    }

    public void onSpawn() {
        if (spawnActions == null || spawnActions.isEmpty()) {
            return;
        }
        for (QAction action : spawnActions) {
            try {
                action.play(this);
            } catch (Exception e) {
                FriendlyError error = new FriendlyError(mob.getData().getID(), "Failed to play spawn action", e.getMessage(), "Action: " + action.getClass().getSimpleName());
                error.addStacktrace(e.getStackTrace());
                QuestsXL.get().addRuntimeError(error);
            }
        }
    }

    public void onDamage(Quester quester) {
        if (damageActions == null || damageActions.isEmpty()) {
            return;
        }
        for (QAction action : damageActions) {
            try {
                action.play(quester);
            } catch (Exception e) {
                FriendlyError error = new FriendlyError(mob.getData().getID(), "Failed to play damage action", e.getMessage(), "Action: " + action.getClass().getSimpleName());
                error.addStacktrace(e.getStackTrace());
                QuestsXL.get().addRuntimeError(error);
            }
        }
    }

    public void onAttack(Quester quester) {
        if (attackActions == null || attackActions.isEmpty()) {
            return;
        }
        for (QAction action : attackActions) {
            try {
                action.play(quester);
            } catch (Exception e) {
                FriendlyError error = new FriendlyError(mob.getData().getID(), "Failed to play attack action", e.getMessage(), "Action: " + action.getClass().getSimpleName());
                error.addStacktrace(e.getStackTrace());
                QuestsXL.get().addRuntimeError(error);
            }
        }
    }

    public static AetherHolder loadFromConfigSection(ConfigurationSection section, AetherBaseMob mob) {
        AetherHolder holder = new AetherHolder();
        if (section == null) {
            return null;
        }
        String mobID = mob.getData().getID();
        String source = "aether:" + mobID;
        holder.mob = mob;
        try {
            holder.visibilityConditions = (Set<QCondition>) QConfigLoader.load(holder, "visibilityConditions", section, QRegistries.CONDITIONS, source);
            holder.spawnConditions = (Set<QCondition>) QConfigLoader.load(holder, "spawnConditions", section, QRegistries.CONDITIONS, source);
            holder.rightClickActions = (Set<QAction>) QConfigLoader.load(holder, "rightClickActions", section, QRegistries.ACTIONS, source);
            holder.leftClickActions = (Set<QAction>) QConfigLoader.load(holder, "leftClickActions", section, QRegistries.ACTIONS, source);
            holder.deathActions = (Set<QAction>) QConfigLoader.load(holder, "deathActions", section, QRegistries.ACTIONS, source);
            holder.spawnActions = (Set<QAction>) QConfigLoader.load(holder, "spawnActions", section, QRegistries.ACTIONS, source);
            holder.damageActions = (Set<QAction>) QConfigLoader.load(holder, "damageActions", section, QRegistries.ACTIONS, source);
            holder.attackActions = (Set<QAction>) QConfigLoader.load(holder, "attackActions", section, QRegistries.ACTIONS, source);
        } catch (Exception e) {
            FriendlyError error = new FriendlyError(mob.getData().getID(), "Failed to load actions", e.getMessage(), "Mob ID: " + mobID);
            error.addStacktrace(e.getStackTrace());
            QuestsXL.get().addRuntimeError(error);
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

    @Override
    public QComponent getParent() {
        return null; // Parent is always self for mobs
    }

    @Override
    public void setParent(QComponent qComponent) {

    }

    @Override
    public String id() {
        return mob.getData().getID();
    }
}
