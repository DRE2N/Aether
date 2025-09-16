package de.erethon.aether.commands;

import de.erethon.bedrock.command.ECommandCache;
import de.erethon.bedrock.plugin.EPlugin;

import java.util.HashSet;
import java.util.Set;

public class CommandCache extends ECommandCache {

    public static final String LABEL = "aether";
    public static final Set<String> aliases = Set.of("ae", "mob");

    EPlugin plugin;

    public CommandCache(EPlugin plugin) {
        super(LABEL, plugin, aliases, new HashSet<>());
        this.plugin = plugin;
        addCommand(new TestCommand());
        addCommand(new SpawnCommand());
        addCommand(new ReloadCommand());
        addCommand(new FormationCommand());
        addCommand(new KillCommand());
        addCommand(new DebugCommand());
        addCommand(new SpawnerCommand());
        addCommand(new NaturalSpawnCommand());

    }
}
