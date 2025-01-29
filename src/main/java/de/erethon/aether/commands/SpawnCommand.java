package de.erethon.aether.commands;

import de.erethon.aether.Aether;
import de.erethon.aether.creature.ActiveNPC;
import de.erethon.aether.creature.AetherBaseMob;
import de.erethon.aether.creature.NPCData;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommand;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SpawnCommand extends ECommand {

    public SpawnCommand() {
        setCommand("spawn");
        setAliases("s");
        setMinArgs(0);
        setMaxArgs(3);
        setPlayerCommand(true);
        setHelp("Help.");
        setPermission("mxl.spawn");
    }
    @Override
    public void onExecute(String[] args, CommandSender commandSender) {
        if (args[1] == null) {
            MessageUtil.sendMessage(commandSender, "&cBitte gebe einen NPC an.");
            return;
        }
        NPCData npcData = Aether.getInstance().getCreatureManager().getByID(args[1]);
        if (npcData == null) {
            MessageUtil.sendMessage(commandSender, "&cDer NPC " + args[1] + " &cexistiert nicht.");
            return;
        }

        Location location = null;
        int amount = 0;
        if (args.length > 2) {
            amount = Integer.parseInt(args[2]);
        }
        if (args.length > 3) {
            if (args.length < 7) {
                MessageUtil.sendMessage(commandSender, "&cBitte gebe alle Parameter an. /ae s <mob> <amount> <world> <x> <y> <z>");
                return;
            }
            World world = Bukkit.getWorld(args[3]);
            int x = Integer.parseInt(args[4]);
            int y = Integer.parseInt(args[5]);
            int z = Integer.parseInt(args[6]);
            location = new Location(world, x, y, z);
        }
        if (location == null) {
            Player player = (Player) commandSender;
            for (int i = 0; i <= amount; i++) {
                try {
                    Class<? extends AetherBaseMob> toSpawn = npcData.getEntityClass();
                    AetherBaseMob activeNPC = toSpawn.getConstructor(NPCData.class, World.class).newInstance(npcData, player.getWorld());
                    activeNPC.setPos(player.getX(), player.getY(), player.getZ());
                    activeNPC.addToWorld();
                    MessageUtil.sendMessage(commandSender, "&aNPC " + npcData.getID() + " gespawnt. BaseClass: " + activeNPC.getClass().getSimpleName());
                } catch (Exception e) {
                    MessageUtil.sendMessage(commandSender, "&cFehler beim Spawnen des NPCs: " + e.getMessage());
                    MessageUtil.log("Error while spawning NPC " + npcData.getID()+ " for " + player.getName());
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        // Copied from QXL Dialogues because I have no idea how this works
        if (args.length == 2) {
            List<String> completes = new ArrayList<>();
            for (NPCData dialogue : Aether.getInstance().getCreatureManager().getCreatures()) {
                if (dialogue.getID().toLowerCase().startsWith(args[1].toLowerCase())) {
                    completes.add(dialogue.getID());
                }
            }
            return completes;
        }
        return null;
    }



}
