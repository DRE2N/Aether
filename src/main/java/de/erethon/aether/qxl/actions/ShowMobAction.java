package de.erethon.aether.qxl.actions;

import de.erethon.aether.Aether;
import de.erethon.aether.creature.AetherBaseMob;
import de.erethon.aether.creature.CreatureManager;
import de.erethon.aether.qxl.QxlInstanceScope;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.doc.QParamDoc;
import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.component.action.QBaseAction;
import de.erethon.questsxl.error.FriendlyError;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

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
    public void playInternal(Quester quester) {
        if (!conditions(quester)) return;
        try {
            Set<AetherBaseMob> mobs = creatureManager.getTaggedMobs(tag);
            if (mobs == null) {
                throw new IllegalArgumentException("No mobs with tag " + tag + " found.");
            }
            for (AetherBaseMob mob : mobs) {
                if (quester instanceof QEvent event) {
                    switch (mode) {
                        case EVENT_IN_RANGE -> execute(quester, player -> show(player.getPlayer(), mob));
                        case EVENT_PARTICIPANTS -> {
                            for (QPlayer player : event.getParticipants().keySet()) {
                                show(player.getPlayer(), mob);
                            }
                        }
                        case ALL -> {
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                show(player, mob);
                            }
                        }
                        default -> {
                            // Not applicable to events
                        }
                    }
                }
                if (quester instanceof QPlayer qPlayer) {
                    switch (mode) {
                        case SELF -> show(qPlayer.getPlayer(), mob);
                        case GROUP -> {
                            // TODO, need to implement groups with Aergia
                        }
                        case ALL -> {
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                show(player, mob);
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

    private void show(Player player, AetherBaseMob mob) {
        if (QxlInstanceScope.canInteract(mob, player)) {
            player.showEntity(Aether.getInstance(), mob.getBukkitLivingEntity());
        }
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
