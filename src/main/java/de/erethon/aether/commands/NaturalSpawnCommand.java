package de.erethon.aether.commands;

import de.erethon.aether.Aether;
import de.erethon.aether.creature.NPCData;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import de.erethon.factions.Factions;
import de.erethon.factions.entity.FEntityCache;
import de.erethon.factions.entity.FLegalEntity;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.RegionCache;
import de.erethon.factions.region.RegionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biome;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class NaturalSpawnCommand extends ECommand {

    private final Aether plugin = Aether.getInstance();
    private final Factions factions = Factions.get();
    private final RegionManager regionManager = factions.getRegionManager();

    public NaturalSpawnCommand() {
        setCommand("naturalspawn");
        setAliases("ns");
        setMinArgs(1);
        setMaxArgs(100);
        setPlayerCommand(true);
        setConsoleCommand(false);
        setHelp("Manage natural mob spawning for biomes and regions");
        setPermission("aether.naturalspawn");
    }

    @Override
    public void onExecute(String[] args, CommandSender sender) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendMessage(sender, "&cPlayers only.");
            return;
        }
        if (args.length < 2) {
            sendUsage(sender);
            return;
        }
        String sub = args[1].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "add":
                handleAdd(args, player);
                return;
            case "remove":
            case "rm":
                handleRemove(args, player);
                return;
            case "set":
                handleSet(args, player);
                return;
            case "list":
                handleList(args, player);
                return;
            case "clear":
                handleClear(args, player);
                return;
            case "chance":
                handleChance(args, player);
                return;
            case "reload":
                handleReload(args, player);
                return;
            case "info":
                handleInfo(args, player);
                return;
            default:
                sendUsage(sender);
        }
    }

    private void handleAdd(String[] args, Player player) {
        if (args.length < 4) {
            MessageUtil.sendMessage(player, "&cUsage: /ae ns add <biome|region> <name|here> <mobId> [weight]");
            return;
        }

        String type = args[2].toLowerCase(Locale.ROOT);
        String target = args[3];
        String mobId = args[4];
        int weight = 1;

        if (args.length >= 6) {
            try {
                weight = Integer.parseInt(args[5]);
            } catch (NumberFormatException e) {
                MessageUtil.sendMessage(player, "&cWeight must be a number.");
                return;
            }
        }

        NPCData npcData = plugin.getCreatureManager().getByID(mobId);
        if (npcData == null) {
            MessageUtil.sendMessage(player, "&cMob '&f" + mobId + "&c' not found.");
            return;
        }

        String resolvedTarget = resolveTarget(type, target, player);
        if (resolvedTarget == null) {
            return;
        }

        File file = getMobSpawnFile();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        String path = type + "s." + resolvedTarget + ".mobs." + mobId;
        config.set(path, weight);

        saveConfig(config, file);
        reloadConfig();

        MessageUtil.sendMessage(player, "&aAdded mob '&f" + mobId + "&a' with weight &f" + weight + "&a to " + type + " '&f" + resolvedTarget + "&a'.");
    }

    private void handleRemove(String[] args, Player player) {
        if (args.length < 4) {
            MessageUtil.sendMessage(player, "&cUsage: /ae ns remove <biome|region> <name|here> <mobId>");
            return;
        }

        String type = args[2].toLowerCase(Locale.ROOT);
        String target = args[3];
        String mobId = args[4];

        String resolvedTarget = resolveTarget(type, target, player);
        if (resolvedTarget == null) {
            return;
        }

        File file = getMobSpawnFile();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        String path = type + "s." + resolvedTarget + ".mobs." + mobId;
        if (!config.contains(path)) {
            MessageUtil.sendMessage(player, "&cMob '&f" + mobId + "&c' not found in " + type + " '&f" + resolvedTarget + "&c'.");
            return;
        }

        config.set(path, null);

        // Clean up empty sections
        cleanupEmptySection(config, type + "s." + resolvedTarget + ".mobs");
        cleanupEmptySection(config, type + "s." + resolvedTarget);
        cleanupEmptySection(config, type + "s");

        saveConfig(config, file);
        reloadConfig();

        MessageUtil.sendMessage(player, "&aRemoved mob '&f" + mobId + "&a' from " + type + " '&f" + resolvedTarget + "&a'.");
    }

    private void handleSet(String[] args, Player player) {
        if (args.length < 5) {
            MessageUtil.sendMessage(player, "&cUsage: /ae ns set <biome|region> <name|here> <mobId> <weight>");
            return;
        }

        String type = args[2].toLowerCase(Locale.ROOT);
        String target = args[3];
        String mobId = args[4];
        int weight;

        try {
            weight = Integer.parseInt(args[5]);
        } catch (NumberFormatException e) {
            MessageUtil.sendMessage(player, "&cWeight must be a number.");
            return;
        }

        NPCData npcData = plugin.getCreatureManager().getByID(mobId);
        if (npcData == null) {
            MessageUtil.sendMessage(player, "&cMob '&f" + mobId + "&c' not found.");
            return;
        }

        String resolvedTarget = resolveTarget(type, target, player);
        if (resolvedTarget == null) {
            return;
        }

        File file = getMobSpawnFile();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        String path = type + "s." + resolvedTarget + ".mobs." + mobId;
        if (!config.contains(path)) {
            MessageUtil.sendMessage(player, "&cMob '&f" + mobId + "&c' not found in " + type + " '&f" + resolvedTarget + "&c'. Use 'add' instead.");
            return;
        }

        config.set(path, weight);

        saveConfig(config, file);
        reloadConfig();

        MessageUtil.sendMessage(player, "&aSet weight of mob '&f" + mobId + "&a' to &f" + weight + "&a in " + type + " '&f" + resolvedTarget + "&a'.");
    }

    private void handleList(String[] args, Player player) {
        if (args.length < 3) {
            MessageUtil.sendMessage(player, "&cUsage: /ae ns list <biome|region> [name|here]");
            return;
        }

        String type = args[2].toLowerCase(Locale.ROOT);
        String target = args.length >= 4 ? args[3] : "here";

        String resolvedTarget = resolveTarget(type, target, player);
        if (resolvedTarget == null) {
            return;
        }

        File file = getMobSpawnFile();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection section = config.getConfigurationSection(type + "s." + resolvedTarget + ".mobs");
        if (section == null || section.getKeys(false).isEmpty()) {
            MessageUtil.sendMessage(player, "&eNo mobs configured for " + type + " '&f" + resolvedTarget + "&e'.");
            return;
        }

        MessageUtil.sendMessage(player, "&aMobs in " + type + " '&f" + resolvedTarget + "&a':");
        for (String mobId : section.getKeys(false)) {
            int weight = section.getInt(mobId, 1);
            MessageUtil.sendMessage(player, "&7- &f" + mobId + " &8(weight: &7" + weight + "&8)");
        }
    }

    private void handleClear(String[] args, Player player) {
        if (args.length < 4) {
            MessageUtil.sendMessage(player, "&cUsage: /ae ns clear <biome|region> <name|here>");
            return;
        }

        String type = args[2].toLowerCase(Locale.ROOT);
        String target = args[3];

        String resolvedTarget = resolveTarget(type, target, player);
        if (resolvedTarget == null) {
            return;
        }

        File file = getMobSpawnFile();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        String path = type + "s." + resolvedTarget;
        if (!config.contains(path)) {
            MessageUtil.sendMessage(player, "&eNo configuration found for " + type + " '&f" + resolvedTarget + "&e'.");
            return;
        }

        config.set(path, null);
        cleanupEmptySection(config, type + "s");

        saveConfig(config, file);
        reloadConfig();

        MessageUtil.sendMessage(player, "&aCleared all mobs from " + type + " '&f" + resolvedTarget + "&a'.");
    }

    private void handleChance(String[] args, Player player) {
        if (args.length < 5) {
            MessageUtil.sendMessage(player, "&cUsage: /ae ns chance <biome|region> <name|here> <0.0-1.0>");
            return;
        }

        String type = args[2].toLowerCase(Locale.ROOT);
        String target = args[3];
        double chance;

        try {
            chance = Double.parseDouble(args[4]);
            if (chance < 0.0 || chance > 1.0) {
                MessageUtil.sendMessage(player, "&cChance must be between 0.0 and 1.0 (0% to 100%).");
                return;
            }
        } catch (NumberFormatException e) {
            MessageUtil.sendMessage(player, "&cChance must be a decimal number between 0.0 and 1.0.");
            return;
        }

        String resolvedTarget = resolveTarget(type, target, player);
        if (resolvedTarget == null) {
            return;
        }

        File file = getMobSpawnFile();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        String path = type + "s." + resolvedTarget + ".spawnChance";
        config.set(path, chance);

        saveConfig(config, file);
        reloadConfig();

        int percentage = (int) (chance * 100);
        MessageUtil.sendMessage(player, "&aSet spawn chance for " + type + " '&f" + resolvedTarget + "&a' to &f" + percentage + "%&a (&f" + chance + "&a).");
    }

    private void handleReload(String[] args, Player player) {
        reloadConfig();
        MessageUtil.sendMessage(player, "&aReloaded mob spawn configuration.");
    }

    private void handleInfo(String[] args, Player player) {
        Location loc = player.getLocation();

        // Get current region
        Region region = regionManager.getRegionByLocation(loc);
        String regionName = region != null ? region.getName(false) : "None";

        // Get current biome
        CraftWorld craftWorld = (CraftWorld) loc.getWorld();
        Holder<Biome> biomeHolder = craftWorld.getHandle().getBiome(new BlockPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
        String biomeName = biomeHolder.unwrapKey()
                .map(ResourceKey::identifier)
                .map(Object::toString)
                .orElse("unknown");

        MessageUtil.sendMessage(player, "&aLocation Info:");
        MessageUtil.sendMessage(player, "&7Region: &f" + regionName);
        MessageUtil.sendMessage(player, "&7Biome: &f" + biomeName);

        // Show configured mobs and spawn chances
        File file = getMobSpawnFile();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        if (region != null) {
            // Show region spawn chance
            double regionSpawnChance = config.getDouble("regions." + regionName + ".spawnChance", 1.0);
            int regionPercentage = (int) (regionSpawnChance * 100);
            MessageUtil.sendMessage(player, "&7Region spawn chance: &f" + regionPercentage + "% &8(" + regionSpawnChance + ")");

            ConfigurationSection regionSection = config.getConfigurationSection("regions." + regionName + ".mobs");
            if (regionSection != null && !regionSection.getKeys(false).isEmpty()) {
                MessageUtil.sendMessage(player, "&aRegion mobs:");
                for (String mobId : regionSection.getKeys(false)) {
                    int weight = regionSection.getInt(mobId, 1);
                    MessageUtil.sendMessage(player, "&7- &f" + mobId + " &8(weight: &7" + weight + "&8)");
                }
            } else {
                MessageUtil.sendMessage(player, "&eNo region mobs configured.");
            }
        }

        // Show biome spawn chance
        double biomeSpawnChance = config.getDouble("biomes." + biomeName + ".spawnChance", 1.0);
        int biomePercentage = (int) (biomeSpawnChance * 100);
        MessageUtil.sendMessage(player, "&7Biome spawn chance: &f" + biomePercentage + "% &8(" + biomeSpawnChance + ")");

        ConfigurationSection biomeSection = config.getConfigurationSection("biomes." + biomeName + ".mobs");
        if (biomeSection != null && !biomeSection.getKeys(false).isEmpty()) {
            MessageUtil.sendMessage(player, "&aBiome mobs:");
            for (String mobId : biomeSection.getKeys(false)) {
                int weight = biomeSection.getInt(mobId, 1);
                MessageUtil.sendMessage(player, "&7- &f" + mobId + " &8(weight: &7" + weight + "&8)");
            }
        } else {
            MessageUtil.sendMessage(player, "&eNo biome mobs configured.");
        }
    }

    private String resolveTarget(String type, String target, Player player) {
        if (target.equals("here")) {
            Location loc = player.getLocation();
            if (type.equals("region")) {
                Region region = regionManager.getRegionByLocation(loc);
                if (region == null) {
                    MessageUtil.sendMessage(player, "&cYou are not in a region.");
                    return null;
                }
                return region.getName(false);
            } else if (type.equals("biome")) {
                CraftWorld craftWorld = (CraftWorld) loc.getWorld();
                Holder<Biome> biomeHolder = craftWorld.getHandle().getBiome(new BlockPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
                return biomeHolder.unwrapKey()
                        .map(ResourceKey::identifier)
                        .map(Object::toString)
                        .orElse("minecraft:plains");
            }
        }

        if (!type.equals("biome") && !type.equals("region")) {
            MessageUtil.sendMessage(player, "&cType must be 'biome' or 'region'.");
            return null;
        }

        return target;
    }

    private File getMobSpawnFile() {
        return new File(plugin.getDataFolder(), "mob-spawns.yml");
    }

    private void saveConfig(YamlConfiguration config, File file) {
        try {
            config.save(file);
        } catch (IOException e) {
            Aether.addException("MobSpawnCommand.saveConfig", "Failed saving mob spawn config", "Could not save mob-spawns.yml file", e);
            MessageUtil.sendMessage(plugin.getServer().getConsoleSender(), "&cFailed to save mob spawn config: " + e.getMessage());
        }
    }

    private void reloadConfig() {
        plugin.reloadMobSpawns();
    }

    private void cleanupEmptySection(YamlConfiguration config, String path) {
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section != null && section.getKeys(false).isEmpty()) {
            config.set(path, null);
        }
    }

    private void sendUsage(CommandSender sender) {
        MessageUtil.sendMessage(sender, "&eUsage:");
        MessageUtil.sendMessage(sender, "&7/ae ns add <biome|region> <name|here> <mobId> [weight]");
        MessageUtil.sendMessage(sender, "&7/ae ns remove <biome|region> <name|here> <mobId>");
        MessageUtil.sendMessage(sender, "&7/ae ns set <biome|region> <name|here> <mobId> <weight>");
        MessageUtil.sendMessage(sender, "&7/ae ns list <biome|region> [name|here]");
        MessageUtil.sendMessage(sender, "&7/ae ns clear <biome|region> <name|here>");
        MessageUtil.sendMessage(sender, "&7/ae ns chance <biome|region> <name|here> <0.0-1.0>");
        MessageUtil.sendMessage(sender, "&7/ae ns info");
        MessageUtil.sendMessage(sender, "&7/ae ns reload");
        MessageUtil.sendMessage(sender, "&8Use 'here' to target current location's biome/region");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> out = new ArrayList<>();
        Player player = (Player) sender;
        Location loc = player.getLocation();
        if (args.length == 2) {
            List<String> subs = Arrays.asList("add", "remove", "set", "list", "clear", "chance", "info", "reload");
            return filterStartsWith(subs, args[1]);
        }
        if (args.length == 3) {
            String sub = args[1].toLowerCase(Locale.ROOT);
            if (Arrays.asList("add", "remove", "set", "list", "clear", "chance").contains(sub)) {
                return filterStartsWith(Arrays.asList("biome", "region"), args[2]);
            }
            return out;
        }
        if (args.length == 4) {
            String sub = args[1].toLowerCase(Locale.ROOT);
            String type = args[2].toLowerCase(Locale.ROOT);
            if (Arrays.asList("add", "remove", "set", "list", "clear", "chance").contains(sub)) {
                List<String> targets = new ArrayList<>();
                targets.add("here");
                if (type.equals("region")) {
                    List<String> regions = new ArrayList<>();
                    for (RegionCache cache : regionManager.getCaches().values()) {
                        regions.addAll(getTabEntities(cache, args[3]));
                    }
                    targets.addAll(regions);
                } else if (type.equals("biome")) {
                    CraftWorld craftWorld = (CraftWorld) loc.getWorld();
                    for (Identifier holder : craftWorld.getHandle().registryAccess().lookupOrThrow(Registries.BIOME).keySet()) {
                        targets.add(holder.toString());
                    }
                }
                return filterStartsWith(targets, args[3]);
            }
            return out;
        }
        if (args.length == 5) {
            String sub = args[1].toLowerCase(Locale.ROOT);
            if (Arrays.asList("add", "remove", "set").contains(sub)) {
                List<String> npcIds = plugin.getCreatureManager().getCreatures().stream()
                        .map(NPCData::getID)
                        .collect(Collectors.toList());
                return filterStartsWith(npcIds, args[4]);
            } else if (sub.equals("chance")) {
                return filterStartsWith(Arrays.asList("0.0", "0.1", "0.25", "0.5", "0.75", "1.0"), args[4]);
            }
            return out;
        }
        if (args.length == 6) {
            String sub = args[1].toLowerCase(Locale.ROOT);
            if (Arrays.asList("add", "set").contains(sub)) {
                return filterStartsWith(Arrays.asList("1", "5", "10", "15", "20"), args[5]);
            }
            return out;
        }
        return out;
    }

    private List<String> filterStartsWith(Collection<String> candidates, String prefix) {
        if (prefix == null || prefix.isEmpty()) return new ArrayList<>(candidates);
        String lower = prefix.toLowerCase(Locale.ROOT);
        return candidates.stream()
                .filter(s -> s != null && s.toLowerCase(Locale.ROOT).startsWith(lower))
                .sorted()
                .collect(Collectors.toList());
    }

    protected @NotNull List<String> getTabEntities(@NotNull FEntityCache<?> cache, @NotNull String arg) {
        return getTabList(cache.getCache().values(), FLegalEntity::getName, arg);
    }

    protected <E> @NotNull List<String> getTabList(@NotNull Collection<E> list, @NotNull Function<E, String> converter, @NotNull String arg) {
        return list.stream().filter(e -> converter.apply(e).toLowerCase().startsWith(arg.toLowerCase())).map(converter).collect(Collectors.toList());
    }
}
