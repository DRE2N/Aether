package de.erethon.aether.qxl;

import de.erethon.aether.events.CreatureDeathEvent;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class QxlDeathCredit {
    private QxlDeathCredit() {}

    public static Set<QPlayer> contributors(CreatureDeathEvent death, QEvent event) {
        var database = QuestsXL.get().getDatabaseManager();
        var world = event.getLocation().getWorld();
        Set<QPlayer> contributors = new LinkedHashSet<>();
        for (UUID id : death.getContributors()) {
            QPlayer player = database.getLoadedCharacter(id);
            if (player != null && player.getLocation().getWorld().equals(world)) contributors.add(player);
        }
        return contributors;
    }
}
