package de.erethon.aether.network;

import com.destroystokyo.paper.profile.CraftPlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import de.erethon.aether.Aether;
import de.erethon.aether.creature.ActiveCreatureManager;
import de.erethon.aether.creature.ActiveNPC;
import de.erethon.aether.creature.Skin;
import de.erethon.aether.tools.NMSUtils;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import org.bukkit.GameMode;
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.UUID;

public class AetherPacketHandler extends ChannelDuplexHandler {

    private final Aether aether;
    private final ServerPlayer player;
    private final ActiveCreatureManager manager;

    public AetherPacketHandler(Aether aether, ServerPlayer player) {
        this.aether = aether;
        this.manager = aether.getActiveCreatureManager();
        this.player = player;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof ClientboundBundlePacket bundle) { // entity spawning uses bundles now
            bundle.subPackets().forEach(p -> {
                if (p instanceof ClientboundAddEntityPacket addEntityPacket) {
                    ActiveNPC npc = manager.get(addEntityPacket.getUUID());
                    if (npc != null) {
                        if (npc.getNpc().isInstancable() && !npc.getViewers().contains(player.getBukkitEntity())) {
                            return;
                        }
                        if (NMSUtils.getDisplayType(npc.getBaseEntity()) == EntityType.PLAYER) {
                            CraftWorld craftWorld = (CraftWorld) npc.getBaseEntity().getWorld();
                            ServerLevel level = craftWorld.getHandle();
                            CraftPlayerProfile craftPlayerProfile = new CraftPlayerProfile(addEntityPacket.getUUID(), npc.getNpc().getDisplayName());
                            Skin skin = aether.getSkinCache().get(npc.getNpc().getSkinLink());
                            if (skin != null) {
                                craftPlayerProfile.getProperties().add(new ProfileProperty("textures", skin.texture(), skin.signature()));
                            }
                            ServerPlayer fakePlayer = new ServerPlayer(MinecraftServer.getServer(), level, craftPlayerProfile.buildGameProfile(), new ClientInformation("en", 0, ChatVisiblity.SYSTEM, false, 1, HumanoidArm.RIGHT, false, false));
                            fakePlayer.setId(addEntityPacket.getId());
                            fakePlayer.setPos(addEntityPacket.getX(), addEntityPacket.getY(), addEntityPacket.getZ());
                            fakePlayer.getEntityData().set(Player.DATA_PLAYER_MODE_CUSTOMISATION, (byte) 127); // Show all skin layers
                            fakePlayer.getEntityData().markDirty(Player.DATA_PLAYER_MODE_CUSTOMISATION);
                            ClientboundPlayerInfoUpdatePacket infoUpdatePacket = new ClientboundPlayerInfoUpdatePacket(
                                    EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, ClientboundPlayerInfoUpdatePacket.Action.INITIALIZE_CHAT, ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE, ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED, ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY, ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME),
                                    new ClientboundPlayerInfoUpdatePacket.Entry(fakePlayer.getUUID(), fakePlayer.getGameProfile(), false, 0, GameType.SURVIVAL, Component.empty(), null));
                            ClientboundAddEntityPacket playerPacket = new ClientboundAddEntityPacket(fakePlayer);
                            ClientboundSetEntityDataPacket entityDataPacket = new ClientboundSetEntityDataPacket(addEntityPacket.getId(), fakePlayer.getEntityData().packDirty());
                            player.connection.send(infoUpdatePacket);
                            player.connection.send(playerPacket);
                            player.connection.send(entityDataPacket);
                            return;
                        }
                }
            };
            });
        }
        super.write(ctx, msg, promise);
    }

}