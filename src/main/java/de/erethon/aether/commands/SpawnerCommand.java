package de.erethon.aether.commands;

import de.erethon.aether.Aether;
import de.erethon.aether.creature.NPCData;
import de.erethon.aether.spawning.AESpawner;
import de.erethon.aether.spawning.SpawnerManager;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

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
        MessageUtil.sendMessage(player, "&aShowing &f" + nearby.size() + "&a spawners for &f" + (durationTicks/20) + "&a seconds.");
        final int step = 4;
        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            for (AESpawner s : nearby) {
                Location c = s.getCenterLocation();
                if (c == null || c.getWorld() == null) continue;
                // center marker
                c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(0.5, 0.5, 0.5), 3, 0.2, 0.2, 0.2, 0.01);
                //  small ring
                int r = s.getRadius();
                for (int deg = 0; deg < 360; deg += step) {
                    double rad = Math.toRadians(deg);
                    double x = Math.cos(rad) * r;
                    double z = Math.sin(rad) * r;
                    Location lp = c.clone().add(x, 0.1, z);
                    c.getWorld().spawnParticle(Particle.END_ROD, lp, 1, 0, 0, 0, 0);
                }
            }
        }, 0L, 10L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> Bukkit.getScheduler().cancelTask(taskId), durationTicks);
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
            MessageUtil.sendMessage(player, String.format(Locale.ROOT,
                    "&7- &f%s &7at &7%s %d %d %d",
                    s.getId(), c.getWorld().getName(), c.getBlockX(), c.getBlockY(), c.getBlockZ()));
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
        MessageUtil.sendMessage(sender, "&7/ae spawner set <spawnerId> <property> <value>");
        MessageUtil.sendMessage(sender, "&7/ae spawner movehere <spawnerId>");
        MessageUtil.sendMessage(sender, "&7/ae spawner show [range] [seconds]");
        MessageUtil.sendMessage(sender, "&7/ae spawner info [range]");
        MessageUtil.sendMessage(sender, "&7/ae spawner toggle <spawnerId>");
        MessageUtil.sendMessage(sender, "&7/ae spawner list");
        MessageUtil.sendMessage(sender, "&8Properties: id|npc, radius, radiusY, mobsPerSpawn, maxMobs, maxMobsRange, maxMobsRangeY, cooldown, activationRange, chance, isTicking, world, x, y, z");
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
            case "mobsaperspawn":
            case "mobsperspawn":
                return "mobsPerSpawn";
            case "radiusy":
                return "radiusY";
            case "maxmobsrangey":
                return "maxMobsRangeY";
            default:
                return property;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 2) {
            return Arrays.asList("create", "set", "movehere", "show", "info", "toggle", "list");
        }
        if (args.length == 3) {
            if (args[1].equalsIgnoreCase("create")) {
                out.add("<spawnerId>");
            } else if (Arrays.asList("set", "movehere", "toggle").contains(args[1].toLowerCase(Locale.ROOT))) {
                out.addAll(spawnerManager.getConfiguredSpawners().stream().map(AESpawner::getId).collect(Collectors.toList()));
            }
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("create")) {
            out.addAll(plugin.getCreatureManager().getCreatures().stream().map(NPCData::getID).collect(Collectors.toList()));
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("set")) {
            out.addAll(Arrays.asList("id","npc","radius","radiusY","mobsPerSpawn","maxMobs","maxMobsRange","maxMobsRangeY","cooldown","activationRange","chance","isTicking","world","x","y","z"));
        }
        return out;
    }
}
