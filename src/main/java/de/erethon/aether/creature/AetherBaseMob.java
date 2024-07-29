package de.erethon.aether.creature;

import com.destroystokyo.paper.profile.CraftPlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import de.erethon.aether.Aether;
import de.erethon.aether.ai.goals.AEPathfinderGoal;
import de.erethon.aether.tools.NMSUtils;
import io.papermc.paper.adventure.PaperAdventure;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftSound;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.jetbrains.annotations.NotNull;
import team.unnamed.hephaestus.Model;
import team.unnamed.hephaestus.bukkit.ModelView;

import java.util.EnumSet;

public class AetherBaseMob extends PathfinderMob {

    private Aether plugin = Aether.getInstance();
    private NPCData data;
    private ModelView modelView;
    private Entity dataEntity;

    // Constructor for entity loading
    public AetherBaseMob(EntityType<? extends Mob> type, Level world) {
        super((EntityType<? extends PathfinderMob>) type, world);
    }

    public AetherBaseMob(NPCData data, World world) {
        super((EntityType<? extends PathfinderMob>) data.getDisplayType(), ((CraftWorld) world).getHandle());
        this.data = data;
        initStuff();
    }

    public void addToWorld() {
        level().addFreshEntity(this);
    }

    @Override
    public void tick() {
        super.tick();
        if (modelView != null) {
            modelView.tickAnimations();
        }
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket(@NotNull ServerEntity entity) {
        if (dataEntity instanceof Player player) {
            for (ServerPlayerConnection connection : tracker.seenBy) {
                CraftPlayerProfile craftPlayerProfile = new CraftPlayerProfile(dataEntity.getUUID(), data.getDisplayName().examinableName());
                Skin skin = plugin.getSkinCache().get(data.getSkinLink());
                if (skin != null) {
                    craftPlayerProfile.getProperties().add(new ProfileProperty("textures", skin.texture(), skin.signature()));
                }
                ServerPlayer fakePlayer = new ServerPlayer(MinecraftServer.getServer(), (ServerLevel) level(), craftPlayerProfile.buildGameProfile(), new ClientInformation("en", 0, ChatVisiblity.SYSTEM, false, 1, HumanoidArm.RIGHT, false, false));
                fakePlayer.getEntityData().set(Player.DATA_PLAYER_MODE_CUSTOMISATION, (byte) 127); // Show all skin layers
                fakePlayer.setId(dataEntity.getId());
                fakePlayer.getEntityData().markDirty(Player.DATA_PLAYER_MODE_CUSTOMISATION);
                ClientboundPlayerInfoUpdatePacket infoUpdatePacket = new ClientboundPlayerInfoUpdatePacket(
                        EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, ClientboundPlayerInfoUpdatePacket.Action.INITIALIZE_CHAT, ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE, ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED, ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY, ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME),
                        new ClientboundPlayerInfoUpdatePacket.Entry(fakePlayer.getUUID(), fakePlayer.getGameProfile(), false, 0, GameType.SURVIVAL, Component.empty(), null));

                ClientboundSetEntityDataPacket entityDataPacket = new ClientboundSetEntityDataPacket(fakePlayer.getId(), fakePlayer.getEntityData().packDirty());
                connection.send(infoUpdatePacket);
                connection.send(entityDataPacket);
            }
        }
        return NMSUtils.getAddEntityPacketWithType(this, dataEntity.getType());
    }

    @Override
    public @NotNull SynchedEntityData getEntityData() { // Return the correct entity data so the client isn't confused
        if (dataEntity == null) {
            return super.getEntityData();
        }
        return dataEntity.getEntityData();
    }

    @Override
    public CraftEntity getBukkitEntity() {
        if (dataEntity == null) {
            return EntityType.PIG.create(level()).getBukkitEntity();
        }
        return dataEntity.getBukkitEntity();
    }

    @Override
    protected void playAttackSound() {
        if (data.getAttackSound() != null) {
            level().playSound(this, new BlockPos(getBlockX(), getBlockY(), getBlockZ()), CraftSound.bukkitToMinecraft(data.getAttackSound()), SoundSource.NEUTRAL, 1, 1);
        } else {
            level().playSound(this, new BlockPos(getBlockX(), getBlockY(), getBlockZ()), SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.NEUTRAL, 1, 1);
        }
    }

    public CraftLivingEntity getBukkitLivingEntity() {
        if (dataEntity instanceof LivingEntity livingEntity) {
            return livingEntity.getBukkitLivingEntity();
        }
        return EntityType.PIG.create(level()).getBukkitLivingEntity();
    }


    private void registerAetherGoals() {
        if (data.getGoals().isEmpty() && data.getTargets().isEmpty()) {
            goalSelector.addGoal(0, new RandomStrollGoal(this, 0.5));
            goalSelector.addGoal(1, new RandomLookAroundGoal(this));
            goalSelector.addGoal(2, new MeleeAttackGoal(this, 0.8, false));
            targetSelector.addGoal(0, new HurtByTargetGoal(this));
            targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
            return;
        }
        for (AEPathfinderGoal aeGoal : data.getGoals()) {
            goalSelector.addGoal(aeGoal.getPrio(), aeGoal.get(this));
        }
        for (AEPathfinderGoal aeGoal : data.getTargets()) {
            targetSelector.addGoal(aeGoal.getPrio(), aeGoal.get(this));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        data = plugin.getCreatureManager().getByID(nbt.getString("papyrus-entity-id"));
        if (data == null) {
            plugin.getLogger().warning("Failed to load entity data for " + nbt.getString("papyrus-entity-id" + " for entity " + this));
            remove(RemovalReason.DISCARDED);
            return;
        }
        initStuff();
    }

    private void initStuff() {
        if (data.getModelID() != null) {
            Model model = plugin.getModelRegistry().model(data.getModelID());
            if (model == null) {
                plugin.getLogger().warning("Failed to load model for " + data.getModelID());
            } else {
                modelView = plugin.getModelEngine().spawn(model, getBukkitEntity());
            }
        }
        dataEntity = data.getDisplayType().create(level());
        if (dataEntity == null) {
            plugin.getLogger().warning("Failed to create entity for " + data.getDisplayType());
            remove(RemovalReason.DISCARDED);
            return;
        }
        dataEntity.setId(getId());
        dataEntity.setUUID(getUUID());
        registerAetherGoals();
        dataEntity.setCustomName(PaperAdventure.asVanilla(data.getDisplayName()));
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(data.getMaxHealth());
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(data.getMovementSpeed());
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(data.getDamage());
        getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(data.getRange());
        getAttribute(Attributes.ARMOR).setBaseValue(data.getArmor());
        getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(data.getKnockbackResistance());
        getAttribute(Attributes.ATTACK_KNOCKBACK).setBaseValue(data.getKnockback());
        getAttribute(Attributes.ATTACK_SPEED).setBaseValue(data.getAttackSpeed());
        getAttribute(Attributes.ARMOR).setBaseValue(data.getArmor());
        getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(data.getArmorToughness());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putString("papyrus-entity-id", data.getID());
    }
}
