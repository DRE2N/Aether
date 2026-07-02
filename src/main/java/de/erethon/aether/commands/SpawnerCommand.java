package de.erethon.aether.commands;

import de.erethon.aether.Aether;
import de.erethon.aether.creature.NPCData;
import de.erethon.aether.spawning.AESpawner;
import de.erethon.aether.spawning.SpawnerManager;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class SpawnerCommand extends ECommand {

    private final Aether plugin = Aether.getInstance();
    private final SpawnerManager spawnerManager = plugin.getSpawnerManager();

    public SpawnerCommand() {
        setCommand("spawner");
        setAliases("sp");
        setMinArgs(1);
        setMaxArgs(100);
        setPlayerCommand(true);
        setConsoleCommand(false);
        setHelp("Manage Aether spawners ingame");
        setPermission("aether.spawner");
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
            case "create":
                handleCreate(args, player);
                return;
            case "copy":
                handleCopy(args, player);
                return;
            case "copyproperties":
                handleCopyProperties(args, player);
                return;
            case "set":
                handleSet(args, player);
                return;
            case "movehere":
            case "setloc":
                handleMoveHere(args, player);
                return;
            case "show":
                handleShow(args, player);
                return;
            case "info":
                handleInfo(args, player);
                return;
            case "toggle":
                handleToggle(args, player);
                return;
            case "list":
                handleList(args, player);
                return;
            case "tp":
            case "teleport":
                if (args.length < 3) {
                    MessageUtil.sendMessage(player, "&cUsage: /ae spawner tp <spawnerId>");
                    return;
                }
                String spawnerId = args[2];
                AESpawner spawner = spawnerManager.getConfiguredSpawners().stream()
                        .filter(s -> s.getId().equalsIgnoreCase(spawnerId))
                        .findFirst().orElse(null);
                if (spawner == null) {
                    MessageUtil.sendMessage(player, "&cSpawner '&f" + spawnerId + "&c' not found.");
                    return;
                }
                Location loc = spawner.getCenterLocation();
                if (loc == null || loc.getWorld() == null) {
                    MessageUtil.sendMessage(player, "&cSpawner location is invalid.");
                    return;
                }
                teleport(player, loc);
                return;
            default:
                sendUsage(sender);
        }
    }

    private void handleCreate(String[] args, Player player) {
        if (args.length < 4) {
            MessageUtil.sendMessage(player, "&cUsage: /ae spawner create <spawnerId> <npcId>");
            return;
        }
        String spawnerId = args[2];
        String npcId = args[3];
        if (findSpawnerFile(spawnerId) != null) {
            MessageUtil.sendMessage(player, "&cA spawner with id '&f" + spawnerId + "&c' already exists.");
            return;
        }
        NPCData npcData = plugin.getCreatureManager().getByID(npcId);
        if (npcData == null) {
            MessageUtil.sendMessage(player, "&cNPC '&f" + npcId + "&c' not found.");
            return;
        }
        File file = new File(Aether.SPAWNERS, spawnerId + ".yml");
        YamlConfiguration yaml = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
        writeDefaultSpawnerSection(yaml, spawnerId, npcId, player.getLocation());
        try {
            yaml.save(file);
        } catch (IOException e) {
            Aether.addException("SpawnerCommand.create", "Failed saving spawner '" + spawnerId + "'", "Could not save spawner file", e);
            MessageUtil.sendMessage(player, "&cFailed to save spawner file: " + e.getMessage());
            return;
        }
        spawnerManager.reloadSpawners();
        MessageUtil.sendMessage(player, "&aCreated spawner '&f" + spawnerId + "&a' for NPC '&f" + npcId + "&a' at your location.");
    }

    private void handleCopy(String[] args, Player player) {
        if (args.length < 4) {
            MessageUtil.sendMessage(player, "&cUsage: /ae spawner copy <sourceSpawnerId> <newSpawnerId>");
            return;
        }
        String sourceSpawnerId = args[2];
        String newSpawnerId = args[3];

        // Check if source spawner exists
        File sourceFile = findSpawnerFile(sourceSpawnerId);
        if (sourceFile == null) {
            MessageUtil.sendMessage(player, "&cSource spawner '&f" + sourceSpawnerId + "&c' not found.");
            return;
        }

        // Check if new spawner ID already exists
        if (findSpawnerFile(newSpawnerId) != null) {
            MessageUtil.sendMessage(player, "&cA spawner with id '&f" + newSpawnerId + "&c' already exists.");
            return;
        }

        // Load source spawner configuration
        YamlConfiguration sourceYaml = YamlConfiguration.loadConfiguration(sourceFile);
        if (!sourceYaml.contains(sourceSpawnerId)) {
            MessageUtil.sendMessage(player, "&cSource spawner configuration not found in file.");
            return;
        }

        // Create new file for the copied spawner
        File newFile = new File(Aether.SPAWNERS, newSpawnerId + ".yml");
        YamlConfiguration newYaml = new YamlConfiguration();

        // Copy all properties from source spawner to new spawner
        String sourceBase = sourceSpawnerId + ".";
        String newBase = newSpawnerId + ".";

        // Copy all configuration values
        for (String key : sourceYaml.getConfigurationSection(sourceSpawnerId).getKeys(true)) {
            Object value = sourceYaml.get(sourceBase + key);
            newYaml.set(newBase + key, value);
        }

        // Update location to player's current location
        Location playerLoc = player.getLocation();
        newYaml.set(newBase + "loc.world", playerLoc.getWorld().getName());
        newYaml.set(newBase + "loc.x", playerLoc.getBlockX());
        newYaml.set(newBase + "loc.y", playerLoc.getBlockY());
        newYaml.set(newBase + "loc.z", playerLoc.getBlockZ());

        try {
            newYaml.save(newFile);
        } catch (IOException e) {
            Aether.addException("SpawnerCommand.copy", "Failed saving spawner '" + newSpawnerId + "'", "Could not save spawner file", e);
            MessageUtil.sendMessage(player, "&cFailed to save spawner file: " + e.getMessage());
            return;
        }

        spawnerManager.reloadSpawners();
        MessageUtil.sendMessage(player, "&aCreated copy of spawner '&f" + sourceSpawnerId + "&a' as '&f" + newSpawnerId + "&a' at your location.");
    }

    private void handleCopyProperties(String[] args, Player player) {
        if (args.length < 4) {
            MessageUtil.sendMessage(player, "&cUsage: /ae spawner copyproperties <sourceSpawnerId> <targetSpawnerId>");
            return;
        }
        String sourceSpawnerId = args[2];
        String targetSpawnerId = args[3];

        // Check if source spawner exists
        File sourceFile = findSpawnerFile(sourceSpawnerId);
        if (sourceFile == null) {
            MessageUtil.sendMessage(player, "&cSource spawner '&f" + sourceSpawnerId + "&c' not found.");
            return;
        }

        // Check if target spawner exists
        File targetFile = findSpawnerFile(targetSpawnerId);
        if (targetFile == null) {
            MessageUtil.sendMessage(player, "&cTarget spawner '&f" + targetSpawnerId + "&c' not found.");
            return;
        }

        // Load source spawner configuration
        YamlConfiguration sourceYaml = YamlConfiguration.loadConfiguration(sourceFile);
        if (!sourceYaml.contains(sourceSpawnerId)) {
            MessageUtil.sendMessage(player, "&cSource spawner configuration not found in file.");
            return;
        }

        // Load target spawner configuration
        YamlConfiguration targetYaml = YamlConfiguration.loadConfiguration(targetFile);
        if (!targetYaml.contains(targetSpawnerId)) {
            MessageUtil.sendMessage(player, "&cTarget spawner configuration not found in file.");
            return;
        }

        // Save the target's location before copying properties
        String targetBase = targetSpawnerId + ".";
        String targetWorld = targetYaml.getString(targetBase + "loc.world");
        int targetX = targetYaml.getInt(targetBase + "loc.x");
        int targetY = targetYaml.getInt(targetBase + "loc.y");
        int targetZ = targetYaml.getInt(targetBase + "loc.z");

        // Copy properties from source to target (excluding location)
        String sourceBase = sourceSpawnerId + ".";
        List<String> propertiesToCopy = Arrays.asList("id", "isTicking", "radius", "radiusY", "mobsPerSpawn", "chance", "maxMobs", "maxMobsRange", "maxMobsRangeY", "cooldown", "activationRange", "waveSize", "waveCooldown", "minLevel", "maxLevel");

        for (String property : propertiesToCopy) {
            Object value = sourceYaml.get(sourceBase + property);
            if (value != null) {
                targetYaml.set(targetBase + property, value);
            }
        }

        // Restore the target's original location
        targetYaml.set(targetBase + "loc.world", targetWorld);
        targetYaml.set(targetBase + "loc.x", targetX);
        targetYaml.set(targetBase + "loc.y", targetY);
        targetYaml.set(targetBase + "loc.z", targetZ);

        try {
            targetYaml.save(targetFile);
        } catch (IOException e) {
            Aether.addException("SpawnerCommand.copyProperties", "Failed saving spawner '" + targetSpawnerId + "'", "Could not save spawner file", e);
            MessageUtil.sendMessage(player, "&cFailed to save spawner file: " + e.getMessage());
            return;
        }

        spawnerManager.reloadSpawners();
        MessageUtil.sendMessage(player, "&aCopied properties from spawner '&f" + sourceSpawnerId + "&a' to '&f" + targetSpawnerId + "&a' (location preserved).");
    }

    private void handleSet(String[] args, Player player) {
        if (args.length < 5) {
            MessageUtil.sendMessage(player, "&cUsage: /ae spawner set <spawnerId> <property> <value>");
            return;
        }
        String spawnerId = args[2];
        String property = args[3].toLowerCase(Locale.ROOT);
        String value = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
        File file = findSpawnerFile(spawnerId);
        if (file == null) {
            MessageUtil.sendMessage(player, "&cSpawner '&f" + spawnerId + "&c' not found.");
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String base = spawnerId + ".";
        switch (property) {
            case "id": // NPC id
            case "npc":
                yaml.set(base + "id", value);
                break;
            case "radius":
            case "radiusy":
            case "mobsaperspawn":
            case "mobsperspawn":
            case "maxmobs":
            case "maxmobsrange":
            case "maxmobsrangey":
            case "cooldown":
            case "activationrange":
            case "wavesize":
            case "wavecooldown":
            case "maxlevel":
            case "minlevel":
                try {
                    int i = Integer.parseInt(value);
                    String key = mapPropertyKey(property);
                    yaml.set(base + key, i);
                } catch (NumberFormatException e) {
                    MessageUtil.sendMessage(player, "&cValue must be a number.");
                    return;
                }
                break;
            case "chance":
            case "probability":
                try {
                    double d = Double.parseDouble(value);
                    yaml.set(base + "chance", d);
                } catch (NumberFormatException e) {
                    MessageUtil.sendMessage(player, "&cValue must be a decimal number.");
                    return;
                }
                break;
            case "isticking":
            case "ticking":
                yaml.set(base + "isTicking", Boolean.parseBoolean(value));
                break;
            case "world":
                yaml.set(base + "loc.world", value);
                break;
            case "x":
            case "y":
            case "z":
                try {
                    int i = Integer.parseInt(value);
                    yaml.set(base + "loc." + property, i);
                } catch (NumberFormatException e) {
                    MessageUtil.sendMessage(player, "&cValue must be a number.");
                    return;
                }
                break;
            default:
                MessageUtil.sendMessage(player, "&cUnknown property '&f" + property + "&c'.");
                return;
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            Aether.addException("SpawnerCommand.set", "Failed saving spawner '" + spawnerId + "'", "Could not save spawner file", e);
            MessageUtil.sendMessage(player, "&cFailed to save spawner: " + e.getMessage());
            return;
        }
        spawnerManager.reloadSpawners();
        MessageUtil.sendMessage(player, "&aUpdated '&f" + property + "&a' for spawner '&f" + spawnerId + "&a'.");
    }

    private void handleMoveHere(String[] args, Player player) {
        if (args.length < 3) {
            MessageUtil.sendMessage(player, "&cUsage: /ae spawner movehere <spawnerId>");
            return;
        }
        String spawnerId = args[2];
        File file = findSpawnerFile(spawnerId);
        if (file == null) {
            MessageUtil.sendMessage(player, "&cSpawner '&f" + spawnerId + "&c' not found.");
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String base = spawnerId + ".";
        Location l = player.getLocation();
        yaml.set(base + "loc.world", l.getWorld().getName());
        yaml.set(base + "loc.x", l.getBlockX());
        yaml.set(base + "loc.y", l.getBlockY());
        yaml.set(base + "loc.z", l.getBlockZ());
        try {
            yaml.save(file);
        } catch (IOException e) {
            Aether.addException("SpawnerCommand.movehere", "Failed saving spawner '" + spawnerId + "'", "Could not save spawner file", e);
            MessageUtil.sendMessage(player, "&cFailed to save spawner: " + e.getMessage());
            return;
        }
        spawnerManager.reloadSpawners();
        MessageUtil.sendMessage(player, "&aMoved spawner '&f" + spawnerId + "&a' to your location.");
    }

    private void handleShow(String[] args, Player player) {
        int range = 64;
        int durationTicks = 20 * 5; // 5s
        if (args.length >= 3) {
            try { range = Integer.parseInt(args[2]); } catch (NumberFormatException ignored) {}
        }
        if (args.length >= 4) {
            try { durationTicks = Integer.parseInt(args[3]) * 20; } catch (NumberFormatException ignored) {}
        }
        Location pLoc = player.getLocation();
        final int fRange = range;
        final Location fPLoc = pLoc;
        List<AESpawner> nearby = spawnerManager.getConfiguredSpawners().stream()
                .filter(s -> s.getCenterLocation() != null && s.getCenterLocation().getWorld() != null && s.getCenterLocation().getWorld().equals(player.getWorld()))
                .filter(s -> s.getCenterLocation().distanceSquared(fPLoc) <= (double) fRange * (double) fRange)
                .collect(Collectors.toList());
        if (nearby.isEmpty()) {
            MessageUtil.sendMessage(player, "&eNo spawners found within &f" + range + "&e blocks.");
            return;
        }
        MessageUtil.sendMessage(player, "&aShowing &f" + nearby.size() + "&a spawners for &f" + (durationTicks / 20) + "&a seconds.");

        List<TextDisplay> textDisplays = new ArrayList<>();
        for (AESpawner s : nearby) {
            Location c = s.getCenterLocation();
            if (c == null || c.getWorld() == null) continue;

            TextDisplay textDisplay = c.getWorld().spawn(c.clone().add(0.5, 3.0, 0.5), TextDisplay.class, e -> {;
                e.setVisibleByDefault(false);
                e.setShadowed(false);
                e.setBackgroundColor(Color.fromARGB(0)); // fully transparent
                int currentTick = Bukkit.getCurrentTick();
                String timingInfo = getSpawnerTimingInfo(s, currentTick);
                Component displayText = Component.text(s.getId(), NamedTextColor.GOLD)
                        .append(Component.newline())
                        .append(Component.text(s.getEntityIDsString(), NamedTextColor.YELLOW))
                        .append(Component.newline())
                        .append(Component.text("Radius: " + s.getRadius(), NamedTextColor.WHITE))
                        .append(Component.newline())
                        .append(Component.text("Wave Size: " + s.getWaveSize(), NamedTextColor.WHITE))
                        .append(Component.newline())
                        .append(Component.text(timingInfo))
                        .append(Component.newline())
                        .append(Component.text("Mobs: " + s.getLastMobCount() + " / " + s.getMaxMobs()));
                e.text(displayText);
                e.setBillboard(Display.Billboard.CENTER);
                Transformation transformation = e.getTransformation();
                transformation.getScale().set(6.0f, 6.0f, 6.0f);
                e.setTransformation(transformation);
                e.setPersistent(false);
                e.setViewRange(256f);
            });
            player.showEntity(plugin, textDisplay);

            textDisplays.add(textDisplay);
        }

        final int step = 4;
        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            int currentTick = Bukkit.getCurrentTick();
            for (int i = 0; i < nearby.size() && i < textDisplays.size(); i++) {
                AESpawner s = nearby.get(i);
                TextDisplay textDisplay = textDisplays.get(i);
                Location c = s.getCenterLocation();
                if (c == null || c.getWorld() == null) continue;

                String timingInfo = getSpawnerTimingInfo(s, currentTick);
                Component displayText = Component.text(s.getId(), NamedTextColor.GOLD)
                        .append(Component.newline())
                        .append(Component.text(s.getEntityIDsString(), NamedTextColor.YELLOW))
                        .append(Component.newline())
                        .append(Component.text("Radius: " + s.getRadius(), NamedTextColor.WHITE))
                        .append(Component.newline())
                        .append(Component.text("Wave Size: " + s.getWaveSize(), NamedTextColor.WHITE))
                        .append(Component.newline())
                        .append(Component.text(timingInfo))
                        .append(Component.newline())
                        .append(Component.text("Mobs: " + s.getLastMobCount() + " / " + s.getMaxMobs()));
                textDisplay.text(displayText);

                c.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, c.clone().add(0.5, 0.5, 0.5), 3, 0.2, 0.2, 0.2, 0.01, null, true);
                int r = s.getRadius();
                for (int deg = 0; deg < 360; deg += step) {
                    double rad = Math.toRadians(deg);
                    double x = Math.cos(rad) * r;
                    double z = Math.sin(rad) * r;
                    Location lp = c.clone().add(x, 0.1, z);
                    c.getWorld().spawnParticle(Particle.END_ROD, lp, 1, 0, 0, 0, 0, null, true);
                }
            }
        }, 0L, 10L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.getScheduler().cancelTask(taskId);
            for (TextDisplay textDisplay : textDisplays) {
                if (textDisplay != null && !textDisplay.isDead()) {
                    textDisplay.remove();
                }
            }
        }, durationTicks);
    }

    private String getSpawnerTimingInfo(AESpawner spawner, int currentTick) {
        if (!spawner.isTicking()) {
            return "INACTIVE";
        }

        if (spawner.isInWave()) {
            int nextIntraTick = spawner.getNextIntraTick();
            if (nextIntraTick > currentTick) {
                int remainingTicks = nextIntraTick - currentTick;
                double remainingSeconds = remainingTicks / 20.0;
                return String.format("IN WAVE (Next spawn: %.1fs)", remainingSeconds);
            } else {
                return "IN WAVE (Spawning now!)";
            }
        } else {
            int nextWaveTick = spawner.getNextWaveTick();
            if (nextWaveTick > currentTick) {
                int remainingTicks = nextWaveTick - currentTick;
                double remainingSeconds = remainingTicks / 20.0;
                return String.format("Next wave: %.1fs", remainingSeconds);
            } else {
                return "Next wave: Ready!";
            }
        }
    }

    private void handleInfo(String[] args, Player player) {
        int range = 32;
        if (args.length >= 3) {
            try { range = Integer.parseInt(args[2]); } catch (NumberFormatException ignored) {}
        }
        Location pLoc = player.getLocation();
        final int fRange = range;
        final Location fPLoc = pLoc;
        List<AESpawner> nearby = spawnerManager.getConfiguredSpawners().stream()
                .filter(s -> s.getCenterLocation() != null && s.getCenterLocation().getWorld() != null && s.getCenterLocation().getWorld().equals(player.getWorld()))
                .filter(s -> s.getCenterLocation().distanceSquared(fPLoc) <= (double) fRange * (double) fRange)
                .collect(Collectors.toList());
        if (nearby.isEmpty()) {
            MessageUtil.sendMessage(player, "&eNo spawners found within &f" + range + "&e blocks.");
            return;
        }
        for (AESpawner s : nearby) {
            Location c = s.getCenterLocation();
            Component message = Component.text("- " + s.getId() + " @ " +
                    (c == null || c.getWorld() == null ? "?" : (c.getWorld().getName() + " " + c.getBlockX() + " " + c.getBlockY() + " " + c.getBlockZ())) +
                    " | NPC: " + s.getEntityID());
            Component hoverProperties = Component.text(
                    "Radius: " + s.getRadius() + "\n" +
                    "RadiusY: " + s.getRadiusY() + "\n" +
                    "MobsPerSpawn: " + s.getMobsPerSpawn() + "\n" +
                    "Chance: " + s.getProbability() + "\n" +
                    "MaxMobs: " + s.getMaxMobs() + "\n" +
                    "MaxMobsRange: " + s.getMaxMobsRange() + "\n" +
                    "MaxMobsRangeY: " + s.getMaxMobsRangeY() + "\n" +
                    "Cooldown: " + s.getCooldown() + "\n" +
                    "ActivationRange: " + s.getActivationRange() + "\n" +
                    "isTicking: " + s.isTicking() + "\n" +
                    "WaveSize: " + s.getWaveSize() + "\n" +
                    "WaveCooldown: " + s.getWaveCooldown() + "\n" +
                    "MinLevel: " + s.getMinLevel() + "\n" +
                    "MaxLevel: " + s.getMaxLevel()
            );
            message = message.hoverEvent(hoverProperties);
            ClickCallback<Audience> callback = audience -> teleport(player, s.getCenterLocation());
            message = message.clickEvent(ClickEvent.callback(callback));
            player.sendMessage(message);
        }
    }

    private void handleToggle(String[] args, Player player) {
        if (args.length < 3) {
            MessageUtil.sendMessage(player, "&cUsage: /ae spawner toggle <spawnerId>");
            return;
        }
        String spawnerId = args[2];
        File file = findSpawnerFile(spawnerId);
        if (file == null) {
            MessageUtil.sendMessage(player, "&cSpawner '&f" + spawnerId + "&c' not found.");
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String base = spawnerId + ".";
        boolean ticking = yaml.getBoolean(base + "isTicking", true);
        yaml.set(base + "isTicking", !ticking);
        try {
            yaml.save(file);
        } catch (IOException e) {
            Aether.addException("SpawnerCommand.toggle", "Failed saving spawner '" + spawnerId + "'", "Could not save spawner file", e);
            MessageUtil.sendMessage(player, "&cFailed to save spawner: " + e.getMessage());
            return;
        }
        spawnerManager.reloadSpawners();
        MessageUtil.sendMessage(player, "&aSpawner '&f" + spawnerId + "&a' ticking is now &f" + (!ticking));
    }

    private void teleport(Player player, Location loc) {
        player.teleport(loc);
        MessageUtil.sendMessage(player, "&aTeleported to spawner location.");
    }

    private void handleList(String[] args, Player player) {
        List<AESpawner> list = new ArrayList<>(spawnerManager.getConfiguredSpawners());
        list.sort(Comparator.comparing(AESpawner::getId));
        if (list.isEmpty()) {
            MessageUtil.sendMessage(player, "&eNo spawners loaded.");
            return;
        }
        MessageUtil.sendMessage(player, "&aLoaded spawners (&f" + list.size() + "&a): ");
        for (AESpawner s : list) {
            Location c = s.getCenterLocation();
            String where = (c == null || c.getWorld() == null) ? "?" : (c.getWorld().getName() + " " + c.getBlockX() + " " + c.getBlockY() + " " + c.getBlockZ());
            MessageUtil.sendMessage(player, "&7- &f" + s.getId() + " &8@ &7" + where);
        }
    }

    private void sendUsage(CommandSender sender) {
        MessageUtil.sendMessage(sender, "&eUsage:");
        MessageUtil.sendMessage(sender, "&7/ae spawner create <spawnerId> <npcId>");
        MessageUtil.sendMessage(sender, "&7/ae spawner copy <sourceSpawnerId> <newSpawnerId>");
        MessageUtil.sendMessage(sender, "&7/ae spawner copyproperties <sourceSpawnerId> <targetSpawnerId>");
        MessageUtil.sendMessage(sender, "&7/ae spawner set <spawnerId> <property> <value>");
        MessageUtil.sendMessage(sender, "&7/ae spawner movehere <spawnerId>");
        MessageUtil.sendMessage(sender, "&7/ae spawner show [range] [seconds]");
        MessageUtil.sendMessage(sender, "&7/ae spawner info [range]");
        MessageUtil.sendMessage(sender, "&7/ae spawner toggle <spawnerId>");
        MessageUtil.sendMessage(sender, "&7/ae spawner list");
        MessageUtil.sendMessage(sender, "&8Properties: id|npc, radius, radiusY, mobsPerSpawn, maxMobs, maxMobsRange, maxMobsRangeY, cooldown, activationRange, chance, isTicking, world, x, y, z, waveSize, waveCooldown, minLevel, maxLevel");
    }

    private void writeDefaultSpawnerSection(YamlConfiguration yaml, String spawnerId, String npcId, Location loc) {
        String base = spawnerId + ".";
        yaml.set(base + "id", npcId);
        yaml.set(base + "isTicking", true);
        yaml.set(base + "loc.world", loc.getWorld().getName());
        yaml.set(base + "loc.x", loc.getBlockX());
        yaml.set(base + "loc.y", loc.getBlockY());
        yaml.set(base + "loc.z", loc.getBlockZ());
        yaml.set(base + "radius", 16);
        yaml.set(base + "radiusY", 4);
        yaml.set(base + "mobsPerSpawn", 1);
        yaml.set(base + "chance", 1.0D);
        yaml.set(base + "maxMobs", 10);
        yaml.set(base + "maxMobsRange", 16);
        yaml.set(base + "maxMobsRangeY", 8);
        yaml.set(base + "cooldown", 30);
        yaml.set(base + "activationRange", 32);
        // Waves
        yaml.set(base + "waveSize", 1);
        yaml.set(base + "waveCooldown", 20 * 60);
        // Levels
        yaml.set(base + "minLevel", -1);
        yaml.set(base + "maxLevel", -1);
    }

    private File findSpawnerFile(String spawnerId) {
        File dir = Aether.SPAWNERS;
        File[] files = dir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return null;
        for (File file : files) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            if (yaml.getKeys(false).contains(spawnerId)) {
                return file;
            }
        }
        return null;
    }

    private String mapPropertyKey(String property) {
        switch (property) {
            case "mobsperspawn":
                return "mobsPerSpawn";
            case "radiusy":
                return "radiusY";
            case "maxmobsrangey":
                return "maxMobsRangeY";
            case "activationrange":
                return "activationRange";
            case "maxmobsrange":
                return "maxMobsRange";
            case "wavesize":
                return "waveSize";
            case "wavecooldown":
                return "waveCooldown";
                case "minlevel":
                return "minLevel";
            case "maxlevel":
                return "maxLevel";
            case "probability":
                return "chance";
            case "maxmobs":
                return "maxMobs";
            default:
                return property;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 2) {
            List<String> subs = Arrays.asList("create", "copy", "copyproperties", "set", "movehere", "show", "info", "toggle", "list", "tp");
            return filterStartsWith(subs, args[1]);
        }
        if (args.length == 3) {
            String sub = args[1].toLowerCase(Locale.ROOT);
            String partial = args[2];
            if (sub.equals("create")) {
                if (partial.isEmpty()) out.add("<spawnerId>");
                return out;
            } else if (sub.equals("copy") || sub.equals("copyproperties")) {
                List<String> ids = spawnerManager.getConfiguredSpawners().stream().map(AESpawner::getId).collect(Collectors.toList());
                return filterStartsWith(ids, partial);
            } else if (Arrays.asList("set", "movehere", "toggle", "tp").contains(sub)) {
                List<String> ids = spawnerManager.getConfiguredSpawners().stream().map(AESpawner::getId).collect(Collectors.toList());
                return filterStartsWith(ids, partial);
            }
            return out;
        }
        if (args.length == 4) {
            String sub = args[1].toLowerCase(Locale.ROOT);
            String partial = args[3];
            if (sub.equals("create")) {
                List<String> npcIds = plugin.getCreatureManager().getCreatures().stream().map(NPCData::getID).collect(Collectors.toList());
                return filterStartsWith(npcIds, partial);
            }
            if (sub.equals("copy")) {
                if (partial.isEmpty()) out.add("<newSpawnerId>");
                return out;
            }
            if (sub.equals("copyproperties")) {
                List<String> ids = spawnerManager.getConfiguredSpawners().stream().map(AESpawner::getId).collect(Collectors.toList());
                return filterStartsWith(ids, partial);
            }
            if (sub.equals("set")) {
                List<String> props = Arrays.asList("id","npc","radius","radiusY","mobsPerSpawn","maxMobs","maxMobsRange","maxMobsRangeY","cooldown","activationRange","chance","isTicking","world","x","y","z","waveSize","waveCooldown","minLevel","maxLevel");
                return filterStartsWith(props, partial);
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
}
