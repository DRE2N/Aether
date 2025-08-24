package de.erethon.aether.creature;

import com.destroystokyo.paper.profile.CraftPlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import de.erethon.aether.tools.NMSUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public class AetherPlayer extends AetherBaseMob {

    public AetherPlayer(EntityType<? extends Mob> type, Level world) {
        super(type, world);
    }

    public AetherPlayer(NPCData data, World world) {
        super(data, world);
    }

    public AetherPlayer(NPCData data, World world, Integer overrideLevel) {
        super(data, world, overrideLevel);
    }

    @Override
    protected void onFirstSpawn() {
        super.onFirstSpawn();
        getAttribute(Attributes.WAYPOINT_TRANSMIT_RANGE).setBaseValue(0); // Do not transmit waypoints for NPCs
    }

    @Override
    public void startSeenByPlayer(ServerPlayer serverPlayer) {
        super.startSeenByPlayer(serverPlayer);
        sendPlayerStuff(serverPlayer);
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket(@NotNull ServerEntity entity) {
        for (ServerPlayerConnection serverPlayerConnection : moonrise$getTrackedEntity().seenBy) {
            sendPlayerStuff(serverPlayerConnection.getPlayer());
        }
        detectEquipmentUpdates();
        return NMSUtils.getAddEntityPacketWithType(this, EntityType.PLAYER);
    }

    @Override
    public void setNoAi(boolean noAi) {
    }

    @Override
    public void setLeftHanded(boolean leftHanded) {
    }

    @Override
    public void setAggressive(boolean aggressive) {
    }

    @Override
    public boolean isNoAi() {
        return false;
    }

    @Override
    public boolean isLeftHanded() {
        return false;
    }

    @Override
    public boolean isAggressive() {
        return true;
    }

    @Override
    public float getPreciseBodyRotation(float partialTick) {
        return super.getPreciseBodyRotation(partialTick);
    }

    @Override
    public void lookAt(Entity entity, float maxYRotIncrease, float maxXRotIncrease) {
        super.lookAt(entity, maxYRotIncrease, maxXRotIncrease);
        yBodyRot = Mth.rotateIfNecessary(yBodyRot, yHeadRot, (float) getMaxHeadYRot());
    }

    private void sendPlayerStuff(ServerPlayer serverPlayer) {
        ServerGamePacketListenerImpl connection = serverPlayer.connection;
        CraftPlayerProfile craftPlayerProfile = new CraftPlayerProfile(getUUID(), PlainTextComponentSerializer.plainText().serialize(Component.empty()));
        Skin skin = plugin.getSkinCache().get(data.getSkinLink());
        if (skin != null) {
            craftPlayerProfile.getProperties().add(new ProfileProperty("textures", skin.texture(), skin.signature()));
        }
        getEntityData().set(Player.DATA_PLAYER_MODE_CUSTOMISATION, (byte) 127); // Show all skin layers
        getEntityData().markDirty(Player.DATA_PLAYER_MODE_CUSTOMISATION);
        ClientboundPlayerInfoUpdatePacket infoUpdatePacket = new ClientboundPlayerInfoUpdatePacket(
                EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, ClientboundPlayerInfoUpdatePacket.Action.INITIALIZE_CHAT, ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE, ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED, ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY),
                new ClientboundPlayerInfoUpdatePacket.Entry(getUUID(), craftPlayerProfile.buildGameProfile(), false, -1, GameType.SURVIVAL, net.minecraft.network.chat.Component.empty(), true, 0, null));

        ClientboundSetEntityDataPacket entityDataPacket = new ClientboundSetEntityDataPacket(getId(), getEntityData().packDirty());
        connection.send(infoUpdatePacket);
        BukkitRunnable entityDataPacketSender = new BukkitRunnable() {
            @Override
            public void run() {
                connection.send(entityDataPacket);
            }
        };
        entityDataPacketSender.runTaskLater(plugin, 3);
    }
}
