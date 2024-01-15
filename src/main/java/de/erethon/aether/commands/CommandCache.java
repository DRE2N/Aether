package de.erethon.aether.commands;

import de.erethon.bedrock.command.ECommandCache;
import de.erethon.bedrock.plugin.EPlugin;

public class CommandCache extends ECommandCache {

    public static final String LABEL = "aether";

    EPlugin plugin;

    public CommandCache(EPlugin plugin) {
        super(LABEL, plugin);
        this.plugin = plugin;
        addCommand(new TestCommand());
        addCommand(new SpawnCommand());
        addCommand(new ReloadCommand());
        addCommand(new FormationCommand());

    }
}


