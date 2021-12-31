package de.erethon.aether.commands;

import de.erethon.commons.command.DRECommandCache;
import de.erethon.commons.javaplugin.DREPlugin;

public class CommandCache extends DRECommandCache {

    public static final String LABEL = "aether";

    DREPlugin plugin;

    public CommandCache(DREPlugin plugin) {
        super(LABEL, plugin);
        this.plugin = plugin;
        addCommand(new TestCommand());
        addCommand(new SpawnCommand());
        addCommand(new ReloadCommand());

    }
}


