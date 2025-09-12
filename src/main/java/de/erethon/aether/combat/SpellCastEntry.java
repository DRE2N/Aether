package de.erethon.aether.combat;

import de.erethon.aether.Aether;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellLibrary;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

public class SpellCastEntry {

    private String name;
    private SpellData spell;
    private double chance;

    private final SpellLibrary library = Bukkit.getServer().getSpellbookAPI().getLibrary();


    public boolean canCast() {
        return (Math.random() * 100) < chance;
    }

    public SpellData getSpell() {
        return spell;
    }

    public void load(String loadingID, ConfigurationSection section) {
        name = section.getName();
        spell = library.getSpellByID(section.getName());
        if (spell == null) {
            Aether.addException(loadingID, "Spell not found: " + section.getName(), "Check if the Spellbook spell exists and is loaded", null);
            Aether.log("Spell not found: " + section.getName());
            return;
        }
        chance = section.getDouble("chance");
    }

    @Override
    public String toString() {
        return name;
    }
}
