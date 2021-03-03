package de.erethon.mobsxl.npc;

import de.erethon.commons.config.DREConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

import java.io.File;

public class NPC {

    private int ID;
    ConfigurationSection cfg;
    private EntityType baseType;
    private EntityType displayType;
    private String displayName;

    public NPC(ConfigurationSection cfg) {
        this.cfg = cfg;
        load();
    }

    public EntityType getBaseType() {
        return baseType;
    }

    public void setBaseType(EntityType baseType) {
        this.baseType = baseType;
    }

    public EntityType getDisplayType() {
        return displayType;
    }

    public void setDisplayType(EntityType displayType) {
        this.displayType = displayType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public int getID() {
        return ID;
    }

    public void load() {
        ID = cfg.getInt("id");
        displayName = cfg.getString("displayname");
        baseType = EntityType.valueOf(cfg.getString("baseType"));
        displayType = EntityType.valueOf(cfg.getString("type"));

    }
}
