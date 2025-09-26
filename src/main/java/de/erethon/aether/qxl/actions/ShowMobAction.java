package de.erethon.aether.qxl.actions;

import de.erethon.aether.Aether;
import de.erethon.aether.creature.AetherBaseMob;
import de.erethon.aether.creature.CreatureManager;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.action.QBaseAction;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QLocation;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.error.FriendlyError;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

enum ShowMode {
    ALL,
    SELF,
    GROUP,
    EVENT_IN_RANGE,
    EVENT_PARTICIPANTS
}

@QLoadableDoc(
        value = "show_mob",
        description = "Makes tagged mobs visible. They have to be set to `instanceable` for this to work.",
        shortExample = "show_mob: tag=bandit; mode=event_participants",
        longExample = {
                "show_mob:",
                "  tag: bandit",
        }
)
public class ShowMobAction extends QBaseAction {

    Aether aether = Aether.getInstance();
    CreatureManager creatureManager = aether.getCreatureManager();

    @QParamDoc(name = "tag" , description = "The mob tag to show")
    String tag = null;

    @QParamDoc(name = "mode", description = "The show mode. Possible values: ALL, SELF, GROUP, EVENT_IN_RANGE, EVENT_PARTICIPANTS. Default is ALL")
    ShowMode mode = ShowMode.ALL;


    @Override
    public void play(Quester quester) {
        if (!conditions(quester)) return;
        try {
            Set<AetherBaseMob> mobs = creatureManager.getTaggedMobs(tag);
            if (mobs == null) {
                throw new IllegalArgumentException("No mobs with tag " + tag + " found.");
            }
            for (AetherBaseMob mob : mobs) {
                if (quester instanceof QEvent event) {
                    switch (mode) {
                        case EVENT_IN_RANGE -> execute(quester, player -> player.getPlayer().showEntity(Aether.getInstance(), mob.getBukkitLivingEntity()));
                        case EVENT_PARTICIPANTS -> {
                            for (QPlayer player : event.getParticipants().keySet()) {
                                player.getPlayer().showEntity(Aether.getInstance(), mob.getBukkitLivingEntity());
                            }
                        }
                        case ALL -> {
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                player.showEntity(Aether.getInstance(), mob.getBukkitLivingEntity());
                            }
                        }
                        default -> {
                            // Not applicable to events
                        }
                    }
                }
                if (quester instanceof QPlayer qPlayer) {
                    switch (mode) {
                        case SELF -> qPlayer.getPlayer().showEntity(Aether.getInstance(), mob.getBukkitLivingEntity());
                        case GROUP -> {
                            // TODO, need to implement groups with Aergia
                        }
                        case ALL -> {
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                player.showEntity(Aether.getInstance(), mob.getBukkitLivingEntity());
                            }
                        }
                        default -> {
                            // Not applicable to players
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            FriendlyError error = new FriendlyError(id,"Failed to show mob", e.getMessage(), "Mob tag: " + tag).addStacktrace(e.getStackTrace());
            QuestsXL.get().addRuntimeError(error);
        }
        onFinish(quester);
    }


    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        tag = cfg.getString("tag", null);
        String modeString = cfg.getString("mode", "ALL").toUpperCase();
        try {
            mode = ShowMode.valueOf(modeString);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid mode: " + modeString + ". Valid modes are: ALL, SELF, GROUP, EVENT_IN_RANGE, EVENT_PARTICIPANTS.");
        }
        if (tag == null) {
            throw new IllegalArgumentException("Tag cannot be null.");
        }
    }
}
