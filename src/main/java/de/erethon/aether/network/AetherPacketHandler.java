package de.erethon.aether.network;

import com.destroystokyo.paper.profile.CraftPlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import de.erethon.aether.Aether;
import de.erethon.aether.creature.ActiveCreatureManager;
import de.erethon.aether.creature.ActiveNPC;
import de.erethon.aether.creature.Skin;
import de.erethon.bedrock.chat.MessageUtil;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import org.bukkit.craftbukkit.v1_19_R2.CraftWorld;

import java.util.List;

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
        if (msg instanceof ClientboundAddEntityPacket addEntityPacket) {
            ActiveNPC npc = manager.get(addEntityPacket.getUUID());
            if (npc != null) {
                if (npc.getNpc().isInstancable() && !npc.getViewers().contains(player.getBukkitEntity())) {
                    return;
                }
                if (addEntityPacket.getType() == EntityType.PLAYER) {
                    CraftWorld craftWorld = (CraftWorld) npc.getBaseEntity().getWorld();
                    ServerLevel level = craftWorld.getHandle();
                    CraftPlayerProfile craftPlayerProfile = new CraftPlayerProfile(addEntityPacket.getUUID(), npc.getNpc().getDisplayName());
                    Skin skin = aether.getSkinCache().get(npc.getNpc().getSkinLink());
                    craftPlayerProfile.getProperties().add(new ProfileProperty("textures", skin.texture(), skin.signature()));
                    ServerPlayer fakePlayer = new ServerPlayer(MinecraftServer.getServer(), level, craftPlayerProfile.buildGameProfile());
                    fakePlayer.setId(addEntityPacket.getId());
                    fakePlayer.setPos(addEntityPacket.getX(), addEntityPacket.getY(), addEntityPacket.getZ());
                    ClientboundPlayerInfoUpdatePacket infoUpdatePacket = ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(fakePlayer));
                    ClientboundAddPlayerPacket playerPacket = new ClientboundAddPlayerPacket(fakePlayer);
                    ClientboundPlayerInfoRemovePacket removePacket = new ClientboundPlayerInfoRemovePacket(List.of(playerPacket.getPlayerId()));
                    player.connection.send(infoUpdatePacket);
                    player.connection.send(playerPacket);
                    player.connection.send(removePacket);
                    return;
                }
            }
        }
        super.write(ctx, msg, promise);
    }

}