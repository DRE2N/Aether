package de.erethon.aether.combat;

import org.bukkit.attribute.Attribute;

import java.util.Map;

public record MobLevelInfo(int level, Map<Attribute, MobAttributeRange> baseAttributeBonus, MobLevelLoot loot) {
}