package de.erethon.aether.combat;

import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellLibrary;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

public class SpellCastEntry {

    private SpellData spell;
    private int chance;

    private SpellLibrary library = Bukkit.getServer().getSpellbookAPI().getLibrary();


    public boolean canCast() {
        return Math.random() * 100 < chance;
    }

    public SpellData getSpell() {
        return spell;
    }

    public void load(ConfigurationSection section) {
        spell = library.getSpellByID(section.getName());
        chance = section.getInt("chance");
    }
}
