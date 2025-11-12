package de.erethon.aether.creature;

import com.magmaguy.freeminecraftmodels.animation.AnimationStateType;
import com.magmaguy.freeminecraftmodels.customentity.ModeledEntity;
import com.magmaguy.freeminecraftmodels.utils.DataMappings;
import de.erethon.aether.Aether;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.ui.EntityStatusDisplay;
import de.erethon.papyrus.CraftPDamageType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

public class ModelledMob extends AetherBaseMob {

    private static final NamespacedKey MARKER_KEY = new NamespacedKey("erethon", "no_nametag");

    private ModeledEntity model;

    private EntityStatusDisplay display;
    private TextDisplay healthDisplay;
    private TextDisplay nameTag;
    private TextDisplay statusDisplay;
    private double hitboxHeight;

    private AnimationStateType currentMovementState = AnimationStateType.IDLE;

    public ModelledMob(EntityType<? extends Mob> type, Level world) {
        super(type, world);
    }

    public ModelledMob(NPCData data, World world) {
        this(data, world, null);
    }

    public ModelledMob(NPCData data, World world, Integer overrideLevel) {
        super(data, world, overrideLevel);
        displayType = EntityType.PIG; // Hardcode this for now
        String id = data.cfg.getString("modelId");
        if (id == null) {
            Aether.addException("ModelledMob " + data.getID(), "Missing  modelId", "Add a modelId or remove the ModelledMob base class", null);
            return;
        }
        entityData = DataMappings.getSynchedEntityData(displayType);
        this.model = new ModeledEntity(id, getBukkitEntity().getLocation());
        if (model == null) {
            Aether.addException("ModelledMob " + data.getID(), "Could not find model with id " + id, "Make sure the model is loaded correctly", null);
            valid = false;
            return;
        }
        hitboxHeight = model.getSkeletonBlueprint().getHitbox().getHeight();
        getBukkitEntity().setVisibleByDefault(false);
        getBukkitEntity().getPersistentDataContainer().set(MARKER_KEY, PersistentDataType.BOOLEAN, true);
        onLoad();
        onFirstSpawn();
    }

    @Override
    public void addToWorld() {
        super.addToWorld();
        model.spawn(getBukkitEntity());
        model.setUnderlyingEntity(getBukkitEntity());
        Hecate hecate = Hecate.getInstance();
        display = new EntityStatusDisplay(getBukkitLivingEntity());
        healthDisplay = display.healthDisplay;
        nameTag = display.entityNameTag;
        statusDisplay = display.statusDisplay;
        if (healthDisplay == null || nameTag == null || statusDisplay == null) {
            return;
        }
        hecate.getStatusDisplayManager().addStatusDisplay(getBukkitLivingEntity(), display);
        // We will handle positioning ourselves to account for the entity changes
        getBukkitEntity().removePassenger(nameTag);
        getBukkitEntity().removePassenger(healthDisplay);
        getBukkitEntity().removePassenger(statusDisplay);
        healthDisplay.setTeleportDuration(1);
        statusDisplay.setTeleportDuration(1);
        nameTag.setTeleportDuration(1);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount, CraftPDamageType type) {
        model.getSkeleton().tint();
        return super.hurtServer(level, source, amount, type);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (model.hasAnimation("attack")) {
            model.playAnimation("attack", false, false);
        }
        return super.doHurtTarget(level, target);
    }

    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);
        if (model.hasAnimation("death")) {
            model.removeWithDeathAnimation();
        } else {
            model.removeWithMinimizedAnimation();
        }
    }

    @Override
    public void onRemoval(RemovalReason reason) {
        super.onRemoval(reason);
        model.setMarkForRemoval(true);
    }

    private void syncSkeletonWithEntity() {
        if (isDeadOrDying()) return;
        model.getSkeleton().setCurrentHeadPitch(getBukkitLivingEntity().getEyeLocation().getPitch());
        model.getSkeleton().setCurrentHeadYaw(getBukkitLivingEntity().getEyeLocation().getYaw());
    }

    @Override
    protected void onFirstSpawn() {
        super.onFirstSpawn();
        setInvisible(true);
    }

    @Override
    public void tick() {
        super.tick();
        syncSkeletonWithEntity();
        model.tick();
        // Determine movement state based on velocity
        boolean isMoving = getDeltaMovement().horizontalDistanceSqr() > 0.0001 && !isDeadOrDying();
        AnimationStateType targetState = isMoving ? AnimationStateType.WALK : AnimationStateType.IDLE;

        if (targetState != currentMovementState) {
            model.getAnimationComponent().playAnimation(targetState.name().toLowerCase(), false, true);
            currentMovementState = targetState;
        }
        if (nameTag == null || healthDisplay == null || statusDisplay == null) {
            return;
        }
        // Update displays
        Location loc = getBukkitEntity().getLocation().add(0, hitboxHeight + 0.18f, 0);
        loc.setPitch(0);
        loc.setYaw(0);
        nameTag.teleport(loc);
        healthDisplay.teleport(loc);
        statusDisplay.teleport(loc);
    }
}
