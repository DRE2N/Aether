package de.erethon.aether.combat;

import java.util.List;

public record MobLevelLoot(int level, List<MobLootEntry> lootItems, MobXPRange xpRange) {
}
