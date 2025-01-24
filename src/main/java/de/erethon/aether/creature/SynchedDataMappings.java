package de.erethon.aether.creature;

import com.mojang.authlib.GameProfile;
import de.erethon.bedrock.chat.MessageUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SynchedDataMappings {

    public static Map<EntityType<?>, SynchedEntityData> ENTITY_DATA_MAPPINGS = new HashMap<>();

    public static void generateMappings(Level level) {
        MessageUtil.log("Generating SynchedEntityData mappings...");
        BuiltInRegistries.ENTITY_TYPE.forEach(type -> {
            Entity entity = type.create(level, EntitySpawnReason.LOAD);
            if (entity == null) {
                MessageUtil.log("Failed to create entity for " + type);
                return;
            }
            SynchedEntityData data = entity.getEntityData();
            ENTITY_DATA_MAPPINGS.put(type, data);
        });
        // Special case for players
        try {
            Player player = new ServerPlayer(MinecraftServer.getServer(), (ServerLevel) level, new GameProfile(UUID.randomUUID(), "MappingsGenerator"), ClientInformation.createDefault());
            SynchedEntityData playerData = player.getEntityData();
            ENTITY_DATA_MAPPINGS.put(EntityType.PLAYER, playerData);
            MessageUtil.log("Successfully generated player data mapping.");
        }
        catch (Exception e) {
            MessageUtil.log("Failed to generate player data mapping.");
            e.printStackTrace();
        }
        MessageUtil.log("Generated " + ENTITY_DATA_MAPPINGS.size() + " mappings.");
    }


}
