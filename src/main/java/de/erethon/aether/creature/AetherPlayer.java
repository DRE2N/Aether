package de.erethon.aether.creature;

import com.destroystokyo.paper.profile.CraftPlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.magmaguy.freeminecraftmodels.utils.DataMappings;
import de.erethon.aether.tools.NMSUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static net.minecraft.world.entity.Avatar.DATA_PLAYER_MODE_CUSTOMISATION;
import static net.minecraft.world.entity.decoration.Mannequin.ALL_LAYERS;

public class AetherPlayer extends AetherBaseMob {

    private static EntityDataAccessor<ResolvableProfile> DATA_PLAYER_PROFILE = null;
    private static EntityDataAccessor<Optional<net.minecraft.network.chat.Component>> DATA_DESCRIPTION = null;
    private static EntityDataAccessor<HumanoidArm> DATA_PLAYER_MAIN_HAND = null;

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
        if (DATA_PLAYER_PROFILE == null) {
            DATA_PLAYER_PROFILE = DataMappings.getAccessor(Mannequin.class, "DATA_PROFILE");
        }
        if (DATA_DESCRIPTION == null) {
            DATA_DESCRIPTION = DataMappings.getAccessor(Mannequin.class, "DATA_DESCRIPTION");
        }
        if (DATA_PLAYER_MAIN_HAND == null) {
            DATA_PLAYER_MAIN_HAND = DataMappings.getAccessor(Avatar.class, "DATA_PLAYER_MAIN_HAND");
        }
        getEntityData().set(DATA_PLAYER_MODE_CUSTOMISATION,  ALL_LAYERS);
        getEntityData().set(DATA_PLAYER_PROFILE, getResolvableProfile());
        getEntityData().set(DATA_DESCRIPTION, Optional.empty());
        getEntityData().set(DATA_PLAYER_MAIN_HAND, HumanoidArm.RIGHT); // Right hand
    }

    @Override
    public void startSeenByPlayer(ServerPlayer serverPlayer) {
        super.startSeenByPlayer(serverPlayer);
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket(@NotNull ServerEntity entity) {
        detectEquipmentUpdates();
        return NMSUtils.getAddEntityPacketWithType(this, EntityType.MANNEQUIN);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        swing(InteractionHand.MAIN_HAND, false);
        return super.doHurtTarget(level, target);
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
        yBodyRot = Mth.rotateIfNecessary(yBodyRot, yHeadRot, maxYRotIncrease);
    }

    @Override
    public void tick() {
        super.tick();
        // Rotate the NPC
        setYRot(yBodyRot);
    }

    private ResolvableProfile getResolvableProfile() {
        CraftPlayerProfile craftPlayerProfile = new CraftPlayerProfile(getUUID(), "NPC");
        Skin skin = plugin.getSkinCache().get(data.getSkinLink());
        if (skin != null) {
            craftPlayerProfile.getProperties().add(new ProfileProperty("textures", skin.texture(), skin.signature()));
        }
        return craftPlayerProfile.buildResolvableProfile();
    }
}
