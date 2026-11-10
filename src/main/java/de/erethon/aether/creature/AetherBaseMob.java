package de.erethon.aether.creature;

import com.magmaguy.freeminecraftmodels.utils.DataMappings;
import de.erethon.aether.Aether;
import de.erethon.aether.ai.behavior.BehaviorTrigger;
import de.erethon.aether.ai.goals.AetherTargetingCache;
import de.erethon.aether.ai.behavior.MobBehaviorController;
import de.erethon.aether.ai.goals.AEMeleeAttackGoal;
import de.erethon.aether.ai.goals.AEPathfinderGoal;
import de.erethon.aether.ai.goals.HurtByTarget;
import de.erethon.aether.ai.goals.MobSeparationGoal;
import de.erethon.aether.ai.goals.ReturnHomeGoal;
import de.erethon.aether.ai.goals.MeleeAttackGoal;
import de.erethon.aether.ai.goals.NearestAttackableTarget;
import de.erethon.aether.ai.goals.RangedBowAttackGoal;
import de.erethon.aether.ai.goals.RangedCrossbowAttackGoal;
import de.erethon.aether.ai.goals.SharedNearestAttackableTargetGoal;
import de.erethon.aether.combat.BossBreakbar;
import de.erethon.aether.combat.MobAttributeRange;
import de.erethon.aether.combat.MobAttributeCurve;
import de.erethon.aether.combat.MobLevelInfo;
import de.erethon.aether.combat.MobLevelLoot;
import de.erethon.aether.combat.MobLootEntry;
import de.erethon.aether.combat.SpellCastEntry;
import de.erethon.aether.events.CreatureDeathEvent;
import de.erethon.aether.events.CreatureInteractEvent;
import de.erethon.aether.events.CreatureLoadEvent;
import de.erethon.aether.events.MobDeliverItemEvent;
import de.erethon.aether.qxl.AetherHolder;
import de.erethon.aether.qxl.QxlInstanceScope;
import de.erethon.aether.tools.NMSUtils;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.data.HCharacter;
import de.erethon.hecate.progression.LevelUtil;
import de.erethon.hecate.ui.EntityStatusDisplay;
import de.erethon.hephaestus.items.HItem;
import de.erethon.papyrus.CraftPDamageType;
import de.erethon.papyrus.entities.CraftCustomMob;
import de.erethon.papyrus.entities.MobSpawnCategoryOverrideProvider;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.dialogue.QDialogueManager;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.spellbook.Spellbook;
import de.erethon.spellbook.teams.SpellbookTeam;
import de.erethon.spellbook.teams.TeamManager;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftSound;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class AetherBaseMob extends Monster implements RangedAttackMob, CrossbowAttackMob, MobSpawnCategoryOverrideProvider {

    private static final int DEFAULT_TARGET_RANGE = 32;
    private static final String DEFAULT_TEAM_ID = "mobs";

    protected Aether plugin = Aether.getInstance();
    protected NPCData data;
    protected EntityType<?> displayType;
    private int version = 0;
    protected CraftCustomMob bukkitLivingEntity;
    public AetherHolder holder;
    private String mobTag = null;
    private String combatFactionId = DEFAULT_TEAM_ID;
    private String instanceScope = null;
    private int instanceVisibilityTicks = 0;
    private final Set<java.util.UUID> instanceHiddenPlayers = new HashSet<>();
    private EntityStatusDisplay detachedHorseStatusDisplay;

    protected int mobLevel = 1;
    private boolean hasSpawnLevel;
    protected MobLevelInfo levelInfo;
    private BossBreakbar bossBreakbar;

    public BossBreakbar getBossBreakbar() {
        if (bossBreakbar == null && data != null && data.hasBossBreakbar()) {
            bossBreakbar = new BossBreakbar(this, data.getBossBreakbarMax(),
                    data.getBossBreakbarWindowTicks(), data.getBossBreakbarIntervalTicks(),
                    data.getBossBreakbarBrokenTicks(), data.getBossBreakbarDamageMultiplier(),
                    data.getBossBreakbarViewRadius(), data.getBossBreakbarName(),
                    data.isBossBreakbarAutoWindow());
        }
        return bossBreakbar;
    }

    private boolean isTalking = false;
    private boolean isChargingCrossbow = false;
    private ReturnHomeGoal returnHomeGoal;
    private boolean inSetTarget = false;
    private boolean skipPostTargetSideEffects = false;
    private MobBehaviorController behaviorController;

    private org.bukkit.entity.Player lastAttackingPlayer;

    private final Map<Player, Double> damageTracker = new HashMap<>();
    private final Set<UUID> eventContributors = new HashSet<>();
    private PlayerHit pendingHit;
    private QEvent eventOwner;
    private UUID eventRunId;

    private static final class PlayerHit {
        private final Player player;
        private final UUID characterId;
        private float health;

        private PlayerHit(Player player, UUID characterId, float health) {
            this.player = player;
            this.characterId = characterId;
            this.health = health;
        }
    }

    private void recordAppliedDamage(PlayerHit hit) {
        if (hit == null) return;
        float applied = hit.health - getHealth();
        hit.health = getHealth();
        if (applied <= 0) return;
        damageTracker.merge(hit.player, (double) applied, Double::sum);
        if (hit.characterId != null) eventContributors.add(hit.characterId);
    }

    public boolean attachToEvent(QEvent event) {
        if (!event.ownMob(getUUID(), () -> {
            if (isAlive()) remove(RemovalReason.DISCARDED);
        })) return false;
        eventOwner = event;
        eventRunId = event.getRunId();
        return true;
    }

    public UUID getEventRunId() {
        return eventRunId;
    }

    protected Set<org.bukkit.entity.Player> getDamageParticipants() {
        return damageTracker.keySet().stream()
                .map(p -> (org.bukkit.entity.Player) p.getBukkitEntity())
                .filter(p -> p != null && p.isOnline())
                .collect(Collectors.toSet());
    }

    // Constructor for entity loading
    public AetherBaseMob(EntityType<? extends Mob> type, Level world) {
        super((EntityType<? extends Monster>) type, world);
    }

    public AetherBaseMob(NPCData data, World world) {
        this(data, world, null);
    }

    public AetherBaseMob(NPCData data, World world, Integer overrideLevel) {
        super((EntityType<? extends Monster>) data.getDisplayType(), ((CraftWorld) world).getHandle());
        this.data = data;
        this.displayType = data.getDisplayType();

        hasSpawnLevel = overrideLevel != null;
        mobLevel = hasSpawnLevel ? overrideLevel : data.selectRandomLevel();
        levelInfo = data.getCompositeLevelInfoForLevel(mobLevel);

        bukkitLivingEntity = new CraftCustomMob(MinecraftServer.getServer().server, this);
        bukkitLivingEntity.setHandle(this);
        bukkitLivingEntity.setPapyrusId(data.getID());
        bukkitLivingEntity.setType(data.getDisplayType());
        entityData = DataMappings.getSynchedEntityData(data.getDisplayType());
        version = data.getCurrentVersion();
        onLoad();
        onFirstSpawn();
        if (holder != null && !holder.checkSpawnConditions()) {
            remove(Entity.RemovalReason.DISCARDED);
            return;
        }
    }

    public void addToWorld() {
        applyRelativeHome();
        ServerLevel level = (ServerLevel) level();
        if (data.isInstancable() || instanceScope != null) {
            getBukkitLivingEntity().setVisibleByDefault(false);
        }
        level.addFreshEntity(this);
        QxlInstanceScope.syncVisibility(this);
    }

    @Override
    public void tick() {
        if (data == null) {
            remove(RemovalReason.DISCARDED);
            return;
        }
        if (QxlInstanceScope.hasNoParticipants(this)) {
            remove(RemovalReason.DISCARDED);
            return;
        }
        getBossBreakbar();
        if (bossBreakbar != null) bossBreakbar.tick();
        if (getTarget() != null && !QxlInstanceScope.canInteract(this, getTarget())) {
            setCombatTarget(null, EntityTargetEvent.TargetReason.FORGOT_TARGET);
        }
        if (behaviorController != null && (bossBreakbar == null || !bossBreakbar.isBroken())) {
            behaviorController.tickBeforeGoals();
        }
        super.tick();
        float headBodyDiff = Mth.wrapDegrees(yHeadRot - yBodyRot);
        if (Math.abs(headBodyDiff) > 30.0f) {
            yBodyRot += headBodyDiff * 0.3f;
        }
        getBukkitLivingEntity().setMaxEnergy(100);
        getBukkitLivingEntity().setEnergy(100);
        if (behaviorController != null && (bossBreakbar == null || !bossBreakbar.isBroken())) {
            behaviorController.tickAfterGoals();
        }
        syncHorseStatusDisplay();
        if (++instanceVisibilityTicks >= 10) {
            instanceVisibilityTicks = 0;
            QxlInstanceScope.syncVisibility(this);
        }
    }

    private void syncHorseStatusDisplay() {
        if (!isHorseDisplayType() || this instanceof ModelledMob) {
            return;
        }
        EntityStatusDisplay display = Hecate.getInstance().getStatusDisplayManager().getStatusDisplay(getBukkitLivingEntity());
        if (display == null || display.entityNameTag == null || display.healthDisplay == null || display.statusDisplay == null) {
            return;
        }
        if (display != detachedHorseStatusDisplay) {
            getBukkitEntity().removePassenger(display.entityNameTag);
            getBukkitEntity().removePassenger(display.healthDisplay);
            getBukkitEntity().removePassenger(display.statusDisplay);
            display.entityNameTag.setTeleportDuration(1);
            display.healthDisplay.setTeleportDuration(1);
            display.statusDisplay.setTeleportDuration(1);
            detachedHorseStatusDisplay = display;
        }
        Location location = getBukkitLivingEntity().getLocation();
        location.setY(getBoundingBox().maxY + 0.18);
        location.setPitch(0);
        location.setYaw(0);
        display.entityNameTag.teleport(location);
        display.healthDisplay.teleport(location);
        display.statusDisplay.teleport(location);
    }

    private boolean isHorseDisplayType() {
        return displayType == net.minecraft.world.entity.EntityTypes.HORSE
                || displayType == net.minecraft.world.entity.EntityTypes.DONKEY
                || displayType == net.minecraft.world.entity.EntityTypes.MULE
                || displayType == net.minecraft.world.entity.EntityTypes.SKELETON_HORSE
                || displayType == net.minecraft.world.entity.EntityTypes.ZOMBIE_HORSE;
    }

    /**
     * Aether's separation goal owns crowd spacing. Skipping this mob's native push scan avoids a
     * dense entity-section query every tick while preserving external pushability and hitboxes.
     */
    @Override
    protected void pushEntities() {
        // Intentionally handled by MobSeparationGoal/AEMeleeAttackGoal.
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        if (entity instanceof LivingEntity livingEntity
                && !QxlInstanceScope.canInteract(this, livingEntity)) {
            return false;
        }
        return super.canCollideWith(entity);
    }

    private void logDebug(String message) {
        getBukkitEntity().getLocation().getNearbyPlayers(8).forEach(p -> {
            if (p.hasPermission("aether.debug") || !Aether.AETHER_DEBUG_MODE) {
                return;
            }
            p.sendMessage(Component.text("[DEBUG] " + message));
        });
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket(@NotNull ServerEntity entity) {
        return NMSUtils.getAddEntityPacketWithType(this, displayType);
    }

    @Override
    public @NotNull SynchedEntityData getEntityData() {// Return the correct entity data so the client isn't confused
        return entityData;
    }

    @Override
    public @NotNull CraftLivingEntity getBukkitEntity() {
        return Objects.requireNonNullElseGet(bukkitLivingEntity, () -> new CraftCustomMob(MinecraftServer.getServer().server, this));
    }

    public @NotNull CraftLivingEntity getBukkitLivingEntity() {
        return Objects.requireNonNullElseGet(bukkitLivingEntity, () -> new CraftCustomMob(MinecraftServer.getServer().server, this));
    }

    @Override
    protected void dropFromLootTable(ServerLevel level, DamageSource source, boolean causedByPlayer) {
        // Custom drops are distributed by distributeLootAndXP().
    }


    @Override
    protected void playAttackSound() {
        if (data.getAttackSound() != null) {
            level().playSound(this, new BlockPos(getBlockX(), getBlockY(), getBlockZ()), CraftSound.bukkitToMinecraft(data.getAttackSound()), SoundSource.NEUTRAL, 1, 1);
        } else {
            level().playSound(this, new BlockPos(getBlockX(), getBlockY(), getBlockZ()), SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.NEUTRAL, 1, 1);
        }
    }

    // Spells
    private void castSpell(SpellCastEntry entry) {
        if (!entry.canCast()) {
            logDebug("Failed to cast spell: Chance not met");
            return;
        }
        org.bukkit.entity.LivingEntity caster = getBukkitLivingEntity();
        if (entry.getSpell() == null) {
            logDebug("Failed to cast spell: Spell is null");
            return;
        }
        if (getTarget() != null) {
            lookAt(getTarget(), 180f, 180f); // I hope large values are enough
            logDebug("Looking at target...");
        }
        entry.getSpell().queue(caster);
        logDebug("Cast spell " + entry.getSpell().getName() + "!");
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (target instanceof LivingEntity livingEntity && !QxlInstanceScope.canInteract(this, livingEntity)) {
            return false;
        }
        if (behaviorController != null) {
            behaviorController.trigger(BehaviorTrigger.ATTACK);
        }
        for (SpellCastEntry spell : data.getOnAttackSpells()) {
            if (spell.getSpell() == null) {
                logDebug("Spell " + spell + " does not exist");
                continue;
            }
            logDebug("Casting onAttack spell " + spell.getSpell().getName());
            castSpell(spell);
        }
        if (target instanceof Player player) {
            try {
                QPlayer qPlayer = QuestsXL.get().getDatabaseManager().getCurrentPlayer((org.bukkit.entity.Player) player.getBukkitEntity());
                AetherHolder qxlHolder = getOrLoadHolder("attackActions");
                if (qPlayer != null && qxlHolder != null) {
                    qxlHolder.onAttack(qPlayer);
                }
            } catch (Exception e) {
                Aether.log("Failed to handle attack for " + data.getID() + ": " + e.getMessage());
            }
        }
        return super.doHurtTarget(level, target);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount, CraftPDamageType type) {
        // Vanilla's invulnerable flag still allows creative-player damage. For authored NPCs the
        // Aether flag means that no damage source may hurt the mob, including creative attacks.
        if (data.isInvulnerable()) {
            return false;
        }
        // Model hitboxes can forward an attack without vanilla's normal packet reach validation.
        // Use the player's configured entity-interaction range against the underlying mob bounds.
        if (source.getEntity() instanceof ServerPlayer player
                && source.getDirectEntity() == player
                && !isWithinPlayerInteractionRange(player)) {
            return false;
        }
        if (source.getEntity() instanceof LivingEntity livingEntity
                && !QxlInstanceScope.canInteract(this, livingEntity)) {
            return false;
        }
        if (source.getEntity() instanceof LivingEntity livingEntity && !Spellbook.canAttack(livingEntity.getBukkitEntity(), getBukkitLivingEntity())) {
            return false;
        }
        if (source.getEntity() instanceof Player) {
            if (data.isInvulnerableToPlayers()) {
                return false;
            }
        }
        for (SpellCastEntry spell : data.getOnDamagedSpells()) {
            if (spell.getSpell() == null) {
                logDebug("Spell " + spell + " does not exist");
                continue;
            }
            logDebug("Casting onHurt spell " + spell.getSpell().getName());
            castSpell(spell);
        }
        UUID characterId = null;
        if (source.getEntity() instanceof Player player) {
            lastAttackingPlayer = (org.bukkit.entity.Player) player.getBukkitEntity();
            try {
                QPlayer qPlayer = QuestsXL.get().getDatabaseManager().getCurrentPlayer((org.bukkit.entity.Player) player.getBukkitEntity());
                AetherHolder qxlHolder = getOrLoadHolder("leftClickActions");
                if (qPlayer != null) {
                    if (qPlayer.isDataLoaded() && !qPlayer.isClosed()) characterId = qPlayer.getCharacterId();
                    if (qxlHolder != null) {
                        qxlHolder.onLeftClick(qPlayer);
                    }
                    if (amount > 0) {
                        qxlHolder = getOrLoadHolder("damageActions");
                        if (qxlHolder != null) {
                            qxlHolder.onDamage(qPlayer);
                        }
                    }
                }
            } catch (Exception e) {
                Aether.log("Failed to handle damage for " + data.getID() + ": " + e.getMessage());
            }
            if (data.getFaction() != null && !Spellbook.canAttack(player.getBukkitEntity(), getBukkitLivingEntity())) {
                return false;
            }
        }
        float exposedAmount = bossBreakbar == null ? amount : amount * (float) bossBreakbar.damageMultiplier();
        PlayerHit previousHit = pendingHit;
        recordAppliedDamage(previousHit);
        pendingHit = source.getEntity() instanceof Player player ? new PlayerHit(player, characterId, getHealth()) : null;
        boolean hurt;
        try {
            hurt = super.hurtServer(level, source, exposedAmount, type);
            if (hurt) recordAppliedDamage(pendingHit);
        } finally {
            pendingHit = previousHit;
            if (previousHit != null) previousHit.health = getHealth();
        }
        if (hurt && behaviorController != null) {
            behaviorController.trigger(BehaviorTrigger.DAMAGED);
        }
        return hurt;
    }

    private boolean isWithinPlayerInteractionRange(ServerPlayer player) {
        org.bukkit.entity.Player bukkitPlayer = player.getBukkitEntity();
        org.bukkit.attribute.AttributeInstance attribute =
                bukkitPlayer.getAttribute(org.bukkit.attribute.Attribute.ENTITY_INTERACTION_RANGE);
        double range = attribute == null ? 3.0 : attribute.getValue();
        org.bukkit.util.Vector eye = bukkitPlayer.getEyeLocation().toVector();
        org.bukkit.util.BoundingBox bounds = getBukkitLivingEntity().getBoundingBox();
        double dx = Math.max(Math.max(bounds.getMinX() - eye.getX(), 0.0), eye.getX() - bounds.getMaxX());
        double dy = Math.max(Math.max(bounds.getMinY() - eye.getY(), 0.0), eye.getY() - bounds.getMaxY());
        double dz = Math.max(Math.max(bounds.getMinZ() - eye.getZ(), 0.0), eye.getZ() - bounds.getMaxZ());
        return dx * dx + dy * dy + dz * dz <= range * range;
    }

    @Override
    public void onCrossbowAttackPerformed() {
        noActionTime = 0;
    }

    public boolean isChargingCrossbow() {
        return isChargingCrossbow;
    }

    @Override
    public void setChargingCrossbow(boolean isCharging) {
        isChargingCrossbow = isCharging;
    }

    @Override
    public EntityType<?> getType() {
        if (displayType != null) {
            return displayType;
        }
        return net.minecraft.world.entity.EntityTypes.PIG;
    }

    @Override
    public MobCategory getEffectiveMobCategory() {
        if (data == null) {
            return MobCategory.MONSTER;
        }
        return data.getMobCategoryOverride();
    }

    @Override
    public void die(DamageSource damageSource) {
        if (dead || isRemoved()) return;
        recordAppliedDamage(pendingHit);
        if (bossBreakbar != null) bossBreakbar.close();
        if (data == null) {
            remove(RemovalReason.DISCARDED);
            return;
        }
        PlayerCombatTracker.getInstance().unregister(getUUID());
        // We need to re-implement this logic, otherwise we cause normal death events for plugins
        Entity entity = damageSource.getEntity();
        dead = true;
        getCombatTracker().recheckStatus();
        if (entity != null) {
            entity.killedEntity((ServerLevel) this.level(), this, damageSource);
        }
        gameEvent(GameEvent.ENTITY_DIE);
        level().broadcastEntityEvent(this, (byte) 3);
        setPose(Pose.DYING);

        // onDeath spells
        if (behaviorController != null) {
            behaviorController.trigger(BehaviorTrigger.DEATH);
        }
        for (SpellCastEntry spell : data.getOnDeathSpells()) {
            if (spell.getSpell() == null) {
                logDebug("Spell " + spell + " does not exist");
                continue;
            }
            logDebug("Casting onDeath spell " + spell.getSpell().getName());
            try {
                castSpell(spell);
            } catch (Throwable e) {
                Aether.log("Failed to cast spell " + spell.getSpell().getName() + " for " + data.getID() + ": " + e.getMessage());
            }
        }
        // Spell and projectile damage may not expose a living direct source. Use vanilla's
        // kill credit when available so player kills still advance quest objectives.
        LivingEntity killer = damageSource.getEntity() instanceof LivingEntity livingEntity
                ? livingEntity : getKillCredit();
        org.bukkit.entity.LivingEntity bukkitKiller = killer != null
                && killer.getBukkitEntity() instanceof org.bukkit.entity.LivingEntity living ? living : null;
        Bukkit.getPluginManager().callEvent(new CreatureDeathEvent(data, bukkitKiller, this, eventContributors));

        // Calculate damage distribution and distribute loot/XP
        distributeLootAndXP();

        AetherHolder qxlHolder = getOrLoadHolder("deathActions");
        if (qxlHolder != null) {
            qxlHolder.onDeath();
        }
        if (mobTag != null) {
            plugin.getCreatureManager().removeTaggedMob(mobTag, this);
        }
    }

    private void distributeLootAndXP() {
        try {
            Map<Player, Double> nearbyParticipants = new HashMap<>();
            double totalDamage = 0.0;

            for (Map.Entry<Player, Double> entry : damageTracker.entrySet()) {
                Player player = entry.getKey();
                double damage = entry.getValue();

                // Check if player is still nearby (within 16 blocks)
                if (player.distanceToSqr(this) <= 16 * 16) {
                    nearbyParticipants.put(player, damage);
                    totalDamage += damage;
                }
            }

            if (nearbyParticipants.isEmpty()) {
                return;
            }

            logDebug("Distributing loot among " + nearbyParticipants.size() + " participants");

            if (levelInfo != null && levelInfo.loot() != null) {
                distributeLevelBasedLoot(nearbyParticipants, totalDamage);
                distributeLevelBasedXP(nearbyParticipants, totalDamage);
            } else {
                distributeClassicLoot(nearbyParticipants, totalDamage);
                distributeClassicXP(nearbyParticipants, totalDamage);
            }
        } catch (Throwable e) {
            Aether.log("Failed to distribute loot for " + data.getID() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void distributeLevelBasedLoot(Map<Player, Double> participants, double totalDamage) {
        MobLevelLoot loot = levelInfo.loot();

        for (MobLootEntry entry : loot.lootItems()) {
            HItem item = entry.item();
            float baseChance = entry.chance() / 100.0f;

            for (Map.Entry<Player, Double> participant : participants.entrySet()) {
                Player player = participant.getKey();
                double damageContribution = participant.getValue() / totalDamage;

                // Scale chance based on damage contribution
                // Players with higher damage get better chances at rare loot
                float scaledChance = (float) (baseChance * damageContribution);

                // Add a minimum chance for all participants (10% of base chance)
                float minimumChance = baseChance * 0.1f;
                scaledChance = Math.max(scaledChance, minimumChance);

                // Roll for this player
                if (random.nextFloat() <= scaledChance) {
                    // Never drop items below the mob's level
                    ItemEntity itemEntity = new ItemEntity(level(), getX(), getY(), getZ(), item.rollRandomStack(mobLevel, mobLevel).getVanillaStack());
                    itemEntity.setTarget(player.getUUID());
                    itemEntity.getBukkitEntity().setVisibleByDefault(false);
                    org.bukkit.entity.Player bukkitPlayer = (org.bukkit.entity.Player) player.getBukkitEntity();
                    bukkitPlayer.showEntity(Aether.getInstance(), itemEntity.getBukkitEntity());
                    logDebug("Dropping level " + mobLevel + " loot " + item.getPatch().toString() +
                            " for " + player.getName().getString() +
                            " (damage: " + String.format("%.1f", damageContribution * 100) + "%, chance: " + String.format("%.1f", scaledChance * 100) + "%)");
                    level().addFreshEntity(itemEntity);
                } else{
                    logDebug("No loot, chance: " + String.format("%.1f", scaledChance * 100) + "% - Minimum: " + String.format("%.1f", (baseChance * 0.1f) * 100) + "%");
                }
            }
        }
    }

    private void distributeLevelBasedXP(Map<Player, Double> participants, double totalDamage) {
        if (levelInfo.loot().xpRange().min() <= 0 && levelInfo.loot().xpRange().max() <= 0) {
            return;
        }

        int baseXP = levelInfo.loot().xpRange().getRandom();

        for (Map.Entry<Player, Double> participant : participants.entrySet()) {
            Player player = participant.getKey();
            double damageContribution = participant.getValue() / totalDamage;

            // Scale XP based on damage contribution with a minimum of 10%
            double xpMultiplier = Math.max(damageContribution, 0.1);
            int playerXP = (int) (baseXP * xpMultiplier);

            if (playerXP > 0) {
                org.bukkit.entity.Player bukkitPlayer = (org.bukkit.entity.Player) player.getBukkitEntity();
                HCharacter character = Hecate.getInstance().getDatabaseManager().getCurrentCharacter(bukkitPlayer);
                // Scale XP based on level difference between the player and the mob, in both directions
                CompletableFuture<Integer> playerLevelFuture = LevelUtil.getCharacterLevel(character);
                playerLevelFuture.thenAcceptAsync(playerLevel -> {
                    int levelDifference = mobLevel - playerLevel;
                    double levelFactor = 0.05; // 5% more/less XP per level difference
                    double levelMultiplier = 1 + (levelDifference * levelFactor);
                    int finalXP = (int) (playerXP * levelMultiplier);
                    LevelUtil.giveCharacterXp(character, finalXP);
                    logDebug("Gave " + finalXP + " XP for " + player.getName().getString() +
                            " (damage: " + String.format("%.1f", damageContribution * 100) + "%, level diff: " + levelDifference + ")");
                }, Bukkit.getScheduler().getMainThreadExecutor(Aether.getInstance()));
            }
        }
    }

    private void distributeClassicLoot(Map<Player, Double> participants, double totalDamage) {
        for (Map.Entry<HItem, Float> lootEntry : data.getLoot().entrySet()) {
            HItem item = lootEntry.getKey();
            float baseChance = lootEntry.getValue() / 100.0f;

            // For each participant, calculate their chance based on damage contribution
            for (Map.Entry<Player, Double> participant : participants.entrySet()) {
                Player player = participant.getKey();
                double damageContribution = participant.getValue() / totalDamage;

                // Scale chance based on damage contribution
                float scaledChance = (float) (baseChance * damageContribution);

                // Add a minimum chance for all participants (10% of base chance)
                float minimumChance = baseChance * 0.1f;
                scaledChance = Math.max(scaledChance, minimumChance);

                // Roll for this player
                if (random.nextFloat() <= scaledChance) {
                    ItemEntity itemEntity = new ItemEntity(level(), getX(), getY(), getZ(), item.rollRandomStack().getVanillaStack());
                    itemEntity.setTarget(player.getUUID());
                    itemEntity.getBukkitEntity().setVisibleByDefault(false);
                    org.bukkit.entity.Player bukkitPlayer = (org.bukkit.entity.Player) player.getBukkitEntity();
                    bukkitPlayer.showEntity(Aether.getInstance(), itemEntity.getBukkitEntity());
                    logDebug("Dropping loot " + item.getPatch().toString() +
                            " for " + player.getName().getString() +
                            " (damage: " + String.format("%.1f", damageContribution * 100) + "%, chance: " + String.format("%.1f", scaledChance * 100) + "%)");
                    level().addFreshEntity(itemEntity);
                }
            }
        }
    }

    private void distributeClassicXP(Map<Player, Double> participants, double totalDamage) {
        if (data.getDropXP() <= 0) {
            return;
        }

        int baseXP = data.getDropXP();

        for (Map.Entry<Player, Double> participant : participants.entrySet()) {
            Player player = participant.getKey();
            double damageContribution = participant.getValue() / totalDamage;

            // Scale XP based on damage contribution with a minimum of 10%
            double xpMultiplier = Math.max(damageContribution, 0.1);
            int playerXP = (int) (baseXP * xpMultiplier);

            if (playerXP > 0) {
                org.bukkit.entity.Player bukkitPlayer = (org.bukkit.entity.Player) player.getBukkitEntity();
                HCharacter character = Hecate.getInstance().getDatabaseManager().getCurrentCharacter(bukkitPlayer);
                LevelUtil.giveCharacterXp(character, playerXP);
                logDebug("Gave " + playerXP + " XP for " + player.getName().getString() +
                        " (damage: " + String.format("%.1f", damageContribution * 100) + "%)");
            }
        }
    }

    /**
     * Sets target from behavior actions without re-entering {@link #setTarget} side effects
     * (avoids Paper/Mob {@code setTarget} recursion and {@code TARGET} triggers during {@code DAMAGED}).
     */
    public boolean setCombatTarget(LivingEntity entityliving, EntityTargetEvent.TargetReason reason) {
        skipPostTargetSideEffects = true;
        try {
            return setTarget(entityliving, reason);
        } finally {
            skipPostTargetSideEffects = false;
        }
    }

    @Override
    public boolean setTarget(LivingEntity entityliving, EntityTargetEvent.TargetReason reason) {
        if (inSetTarget) {
            return super.setTarget(entityliving, reason);
        }
        inSetTarget = true;
        try {
            if (entityliving != null && (!isValidCombatTarget(entityliving)
                    || (returnHomeGoal != null && returnHomeGoal.blocksTargeting()))) {
                return false;
            }

            PlayerCombatTracker tracker = PlayerCombatTracker.getInstance();
            boolean retaliation = reason == EntityTargetEvent.TargetReason.TARGET_ATTACKED_ENTITY
                    && entityliving == getLastHurtByMob();
            if (entityliving instanceof Player player && entityliving != getTarget() && !retaliation
                    && !tracker.canEngage(player.getUUID(), this)) {
                return false;
            }

            boolean applied = super.setTarget(entityliving, reason);
            LivingEntity actualTarget = getTarget();
            tracker.updateTarget(this, actualTarget instanceof Player player ? player.getUUID() : null);
            if (applied && actualTarget != null && !skipPostTargetSideEffects) {
                schedulePostTargetSideEffects(actualTarget);
            }
            return applied;
        } finally {
            inSetTarget = false;
        }
    }

    private void schedulePostTargetSideEffects(LivingEntity entityliving) {
        if (!(level() instanceof ServerLevel)) {
            return;
        }
        java.util.UUID targetId = entityliving.getUUID();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isAlive() || isRemoved()) {
                    return;
                }
                LivingEntity current = getTarget();
                if (current == null || !current.getUUID().equals(targetId)) {
                    return;
                }
                if (behaviorController != null) {
                    behaviorController.trigger(BehaviorTrigger.TARGET);
                }
                for (SpellCastEntry spell : data.getOnTargetSpells()) {
                    if (spell.getSpell() == null) {
                        logDebug("Spell " + spell + " does not exist");
                        continue;
                    }
                    logDebug("Casting onTarget spell " + spell.getSpell().getName());
                    castSpell(spell);
                }
            }
        }.runTask(plugin);
    }

    @Override
    protected @NotNull InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!QxlInstanceScope.canInteract(this, player)) {
            return InteractionResult.PASS;
        }
        if (hand == InteractionHand.OFF_HAND) { // Called for both hands, we only want main hand
            return super.mobInteract(player, hand);
        }
        // Interaction event
        CreatureInteractEvent event = new CreatureInteractEvent((org.bukkit.entity.Player) player.getBukkitEntity(), this, data);
        Bukkit.getPluginManager().callEvent(event);

        // Item delivery
        ItemStack itemInHand = player.getItemInHand(hand);
        if (!itemInHand.isEmpty()) {
            HItem hItem = Aether.getInstance().getItemLibrary().get(itemInHand).getItem();
            if (hItem != null) {
                int amount = itemInHand.getCount();
                MobDeliverItemEvent deliverEvent = new MobDeliverItemEvent((org.bukkit.entity.Player) player.getBukkitEntity(), data.getID(), hItem, amount);
                deliverEvent.callEvent();
                if (deliverEvent.getAmount() != amount) {
                    itemInHand.setCount(deliverEvent.getAmount());
                    if (itemInHand.getCount() <= 0) {
                        player.setItemInHand(hand, new ItemStack(Items.AIR));
                    }
                    return super.mobInteract(player, hand); // A delivery consumes this interaction
                }
            }
        }

        // QXL
        try {
            QPlayer qPlayer = QuestsXL.get().getDatabaseManager().getCurrentPlayer((org.bukkit.entity.Player) player.getBukkitEntity());
            AetherHolder qxlHolder = getOrLoadHolder("rightClickActions");
            if (qPlayer != null && qxlHolder != null) {
                qxlHolder.onRightClick(qPlayer);
            }
            QDialogueManager dialogueManager = QuestsXL.get().getDialogueManager();
            if ( dialogueManager != null) {
                dialogueManager.onNPCRightClick(data.getID(), (org.bukkit.entity.Player) player.getBukkitEntity());
            }
        } catch (Exception e) {
            Aether.log("Failed to handle interaction for " + data.getID() + ": " + e.getMessage());
        }
        return super.mobInteract(player, hand);
    }

    public void displayTextAboveHead(org.bukkit.entity.Player player, String text, int timeout) {
        if (isTalking) {
            return;
        }
        isTalking = true;
        String[] strings = text.split(";");
        Display.TextDisplay textDisplay = net.minecraft.world.entity.EntityTypes.TEXT_DISPLAY.create(level(), EntitySpawnReason.NATURAL);
        textDisplay.persist = false;
        textDisplay.visibleByDefault = false;
        textDisplay.setPos(getX(), getY() + 2, getZ());
        MiniMessage mm = MiniMessage.miniMessage();
        Component component = Component.empty();
        for (String string : strings) {
            component = component.append(mm.deserialize(string)).append(Component.newline());
        }
        textDisplay.setText(PaperAdventure.asVanilla(component));
        level().addFreshEntity(textDisplay);
        addPassenger(textDisplay);
        player.showEntity(Aether.getInstance(), textDisplay.getBukkitEntity());
        BukkitRunnable removeStand = new BukkitRunnable() {
            @Override
            public void run() {
                textDisplay.remove(RemovalReason.DISCARDED);
                isTalking = false;
            }
        };
        removeStand.runTaskLater(plugin, timeout * 20L);
    }

    public void setMobTag(String tag) {
        if (mobTag != null && !mobTag.equals(tag)) {
            plugin.getCreatureManager().removeTaggedMob(mobTag);
        }
        this.mobTag = tag;
        plugin.getCreatureManager().addTaggedMob(tag, this);
    }

    public String getMobTag() {
        return mobTag;
    }

    public void setFaction(String faction) {
        TeamManager teamManager = Spellbook.getInstance().getTeamManager();
        combatFactionId = Objects.requireNonNullElse(faction, DEFAULT_TEAM_ID);
        if (faction != null) {
            SpellbookTeam team = teamManager.getTeam(faction);
            if (team == null) {
                teamManager.createTeam(faction, faction, Color.RED);
                team = teamManager.getTeam(faction);
            }
            teamManager.setTeam(getBukkitLivingEntity(), team);
        }
    }

    public String getCombatFactionId() {
        return combatFactionId;
    }

    public void clearAllAIGoals() {
        returnHomeGoal = null;
        Set<net.minecraft.world.entity.ai.goal.Goal> goalsToRemove = new HashSet<>();
        for (WrappedGoal wrappedGoal : goalSelector.getAvailableGoals()) {
            goalsToRemove.add(wrappedGoal.getGoal());
        }
        goalsToRemove.forEach(goalSelector::removeGoal);

        Set<net.minecraft.world.entity.ai.goal.Goal> targetsToRemove = new HashSet<>();
        for (WrappedGoal wrappedGoal : targetSelector.getAvailableGoals()) {
            targetsToRemove.add(wrappedGoal.getGoal());
        }
        targetsToRemove.forEach(targetSelector::removeGoal);
    }

    public void resetDefaultGoals() {
        clearAllAIGoals();
        registerAetherGoals();
    }

    public void applyGoalProfile(String profileId) {
        GoalProfile profile = data.getBehaviorGoalProfile(profileId);
        if (profile == null) {
            Aether.addException(data.getID(), "Unknown behavior goal profile: " + profileId, "Define ai.behavior.goalProfiles." + profileId, null);
            return;
        }
        clearAllAIGoals();
        addConfiguredGoals(profile.goals());
        if (!profile.goals().isEmpty()) {
            addHomeGoals();
        }
        for (AEPathfinderGoal aeGoal : profile.targets()) {
            if (aeGoal instanceof NearestAttackableTarget nearestAttackableTarget) {
                targetSelector.addGoal(1, new SharedNearestAttackableTargetGoal<>(this, nearestAttackableTarget.getTarget(), 32, nearestAttackableTarget.isCheckVisibility()));
                continue;
            }
            if (aeGoal instanceof HurtByTarget) {
                targetSelector.addGoal(aeGoal.getPrio(), new HurtByTargetGoal(this, this.getClass()));
                continue;
            }
            targetSelector.addGoal(aeGoal.getPrio(), aeGoal.get(this));
        }
    }

    private void registerAetherGoals() {
        clearAllAIGoals();
        boolean explicitlyNoGoals = data.hasConfiguredGoals() && data.getGoals().isEmpty();
        boolean explicitlyNoTargets = data.hasConfiguredTargets() && data.getTargets().isEmpty();
        if (!explicitlyNoGoals) {
            goalSelector.addGoal(8, new MobSeparationGoal(this));
            addHomeGoals();
        }
        if (!data.hasConfiguredGoals() && !data.hasConfiguredTargets()) {
            goalSelector.addGoal(2, new RandomStrollGoal(this, 1));
            goalSelector.addGoal(1, new RandomLookAroundGoal(this));
            goalSelector.addGoal(0, new AEMeleeAttackGoal(this, 1.4, false));
            targetSelector.addGoal(0, new HurtByTargetGoal(this, this.getClass()));
            if (data.getFaction() == null) {
                targetSelector.addGoal(1, new SharedNearestAttackableTargetGoal<>(this, Player.class, DEFAULT_TARGET_RANGE, true));
                return;
            }
            targetSelector.addGoal(1, new SharedNearestAttackableTargetGoal<>(this, LivingEntity.class, DEFAULT_TARGET_RANGE, false));
            return;
        }
        addConfiguredGoals(data.getGoals());
        if (data.getTargets().isEmpty()) {
            // An empty target list should not disable combat for a mob with an attack goal.
            // Noncombat NPCs can still use targets: [] to stay passive.
            if (explicitlyNoGoals || (explicitlyNoTargets && !hasConfiguredAttackGoal())) {
                return;
            }
            targetSelector.addGoal(0, new HurtByTargetGoal(this, this.getClass()));
            if (data.getFaction() == null) {
                targetSelector.addGoal(1, new SharedNearestAttackableTargetGoal<>(this, Player.class, DEFAULT_TARGET_RANGE, true));
                return;
            }
            targetSelector.addGoal(1, new SharedNearestAttackableTargetGoal<>(this, LivingEntity.class, DEFAULT_TARGET_RANGE, false));
            return;
        }
        for (AEPathfinderGoal aeGoal : data.getTargets()) {
            if (aeGoal instanceof NearestAttackableTarget nearestAttackableTarget) {
                targetSelector.addGoal(1, new SharedNearestAttackableTargetGoal<>(this, nearestAttackableTarget.getTarget(), 32, nearestAttackableTarget.isCheckVisibility()));
                continue; // Always add the spellbook team test
            }
            if (aeGoal instanceof HurtByTarget) {
                targetSelector.addGoal(aeGoal.getPrio(), new HurtByTargetGoal(this, this.getClass()));
                continue; // Always add the class exclusion
            }
            targetSelector.addGoal(aeGoal.getPrio(), aeGoal.get(this));
        }
    }

    private void addHomeGoals() {
        returnHomeGoal = new ReturnHomeGoal(this);
        goalSelector.addGoal(-1, returnHomeGoal);
        goalSelector.addGoal(16, new MoveTowardsRestrictionGoal(this, 1.2f));
    }

    @Override
    public void onRemoval(RemovalReason reason) {
        if (eventOwner != null) {
            eventOwner.forgetMob(getUUID());
            if (reason == RemovalReason.UNLOADED_TO_CHUNK && !dead) eventOwner.interruptRun(eventRunId);
        }
        PlayerCombatTracker.getInstance().unregister(getUUID());
        if (bossBreakbar != null) bossBreakbar.close();
        super.onRemoval(reason);
    }

    private boolean hasConfiguredAttackGoal() {
        return data.getGoals().stream().anyMatch(goal -> goal instanceof MeleeAttackGoal
                || goal instanceof RangedBowAttackGoal
                || goal instanceof RangedCrossbowAttackGoal);
    }

    private void addConfiguredGoals(Set<AEPathfinderGoal> configuredGoals) {
        Set<Item> rangedWeapons = new HashSet<>();
        int rangedPriority = Integer.MAX_VALUE;
        boolean hasMelee = false;
        for (AEPathfinderGoal goal : configuredGoals) {
            goalSelector.addGoal(goal.getPrio(), goal.get(this));
            if (goal instanceof MeleeAttackGoal) {
                hasMelee = true;
            } else if (goal instanceof RangedBowAttackGoal) {
                rangedWeapons.add(Items.BOW);
                rangedPriority = Math.min(rangedPriority, goal.getPrio());
            } else if (goal instanceof RangedCrossbowAttackGoal) {
                rangedWeapons.add(Items.CROSSBOW);
                rangedPriority = Math.min(rangedPriority, goal.getPrio());
            }
        }
        if (!hasMelee && !rangedWeapons.isEmpty()) {
            // Equipment is applied after goal registration and can change during combat.
            goalSelector.addGoal(rangedPriority, new AEMeleeAttackGoal(this, 1.4, false) {
                @Override
                public boolean canUse() {
                    return super.canUse() && rangedWeapons.stream().noneMatch(AetherBaseMob.this::isHolding);
                }
            });
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        Optional<String> papyrusId = input.getString("papyrus-entity-id");
        if (papyrusId.isEmpty()) {
            Aether.log("Failed to load entity data: No papyrus ID found for entity " + this);
            remove(RemovalReason.DISCARDED);
            return;
        }
        data = plugin.getCreatureManager().getByID(papyrusId.get());
        if (data == null) {
            plugin.getLogger().warning("Failed to load entity data for " + input.getString("papyrus-entity-id") + " for entity " + this);
            remove(RemovalReason.DISCARDED);
            return;
        }
        this.displayType = data.getDisplayType();
        entityData = DataMappings.getSynchedEntityData(data.getDisplayType());
        Optional<Integer> savedVersion = input.getInt("aether-mob-version");
        if (savedVersion.isEmpty()) {
            savedVersion = input.getInt("papyrus-entity-version");
        }
        version = savedVersion.orElse(0);

        Optional<Integer> savedLevel = input.getInt("aether-mob-level");
        if (savedLevel.isPresent()) {
            mobLevel = savedLevel.get();
            levelInfo = data.getCompositeLevelInfoForLevel(mobLevel);
        }
        hasSpawnLevel = input.getBooleanOr("aether-has-spawn-level", mobLevel > 1 && !data.hasLevels());
        // The display-type template replaced the custom name read from vanilla NBT.
        restoreConfiguredName();
        Optional<String> savedTag = input.getString("aether-mob-tag");
        savedTag.ifPresent(this::setMobTag);
        input.getString("aether-instance-scope").ifPresent(this::setInstanceScope);
        bukkitLivingEntity = new CraftCustomMob(MinecraftServer.getServer().server, this);
        bukkitLivingEntity.setHandle(this);
        bukkitLivingEntity.setPapyrusId(data.getID());
        bukkitLivingEntity.setType(data.getDisplayType());
        if (instanceScope != null) {
            bukkitLivingEntity.setVisibleByDefault(false);
        }
        onLoad();
        int currentVersion = data.getCurrentVersion();
        if (version < currentVersion) {
            plugin.getLogger().warning("Entity " + data.getID() + " (" + getUUID() + ") is outdated (" + version + " -> " + currentVersion + "). Updating...");
            onFirstSpawn();
            version = currentVersion;
        }
    }

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (data == null) {
            plugin.getLogger().warning("Skipping save for Aether entity " + getUUID() + " because no NPC data is loaded.");
            return;
        }
        try {
            output.putString("papyrus-entity-id", data.getID());
            output.putInt("aether-mob-version", version);
            output.putInt("aether-mob-level", mobLevel);
            output.putBoolean("aether-has-spawn-level", hasSpawnLevel);
            if (mobTag != null) {
                output.putString("aether-mob-tag", mobTag);
                plugin.getCreatureManager().addTaggedMob(mobTag, this);
            }
            if (instanceScope != null) {
                output.putString("aether-instance-scope", instanceScope);
            }
            if (data.getFaction() != null) {
                output.putString("aether-mob-faction", data.getFaction());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save entity data for " + data.getID() + " (" + getUUID() + "): " + e.getMessage());
        }
    }

    @Override
    public boolean saveAsPassenger(ValueOutput output, boolean includeAll, boolean includeNonSaveable, boolean forceSerialization) {
        if (this.getRemovalReason() != null && !this.getRemovalReason().shouldSave() && !forceSerialization) {
            return false;
        } else {
            // We need to handle the encodeId ourselves to avoid issues with player NPCs and to ensure its called at all
            EntityType<?> type = this.getType();
            if (type == net.minecraft.world.entity.EntityTypes.PLAYER) {
                type = net.minecraft.world.entity.EntityTypes.MANNEQUIN;
            }
            Identifier key = EntityType.getKey(type);
            String outputKey = key.toString();
            output.putString("id", outputKey);
            this.saveWithoutId(output , includeAll, includeNonSaveable, forceSerialization); // CraftBukkit - pass on includeAll // Paper - Raw entity serialization API
            return true;
        }
    }


    @Override
    public boolean shouldBeSaved() {
        if (data == null || eventRunId != null) {
            return false;
        }
        return data.isPersistent();
    }

    @Override
    public boolean isPersistenceRequired() {
        if (eventRunId != null) return true;
        if (data == null) {
            return false;
        }
        return data.isPersistent();
    }

    protected void onLoad() {
        if (data == null) {
            remove(RemovalReason.DISCARDED);
            return;
        }
        if (data.getBehaviorDefinition() != null) {
            behaviorController = new MobBehaviorController(this, data.getBehaviorDefinition());
        }
        setFaction(Objects.requireNonNullElse(data.getFaction(), DEFAULT_TEAM_ID));
        if (data.getQXLSection() != null) {
            holder = AetherHolder.loadFromConfigSection(data.getQXLSection(), this);
            if (holder != null) {
                holder.onSpawn();
            }
        }
        registerAetherGoals();
        // Set home location/range if configured
        if (data.getHomeLocation() != null) {
            setHomeTo(data.getHomeLocation(), data.getHomeRange());
        }
        applyRelativeHome();
    }

    public void applyRelativeHome() {
        if (data.getHomeLocation() == null && data.getHomeRange() >= 0) {
            setHomeTo(blockPosition(), data.getHomeRange());
        }
    }

    private AetherHolder getOrLoadHolder(String path) {
        if (data == null || data.getQXLSection() == null) {
            return holder;
        }
        if (holder == null || (data.getQXLSection().contains(path) && !holder.hasLoaded(path))) {
            holder = AetherHolder.loadFromConfigSection(data.getQXLSection(), this);
        }
        return holder;
    }

    protected void onFirstSpawn() {
        getBukkitEntity().getPersistentDataContainer().set(plugin.getKey(), PersistentDataType.BOOLEAN, true);
        restoreConfiguredName();
        setGlowingTag(data.isGlowing());
        setNoGravity(!data.isGravity());
        getBukkitEntity().setInvulnerable(data.isInvulnerable());
        persistenceRequired = data.isPersistent();
        persist = data.isPersistent();
        collides = data.hasCollision();
        maxAirTicks = data.getMaximumAir();
        if (getAttribute(Attributes.COMBAT_HURTINVULNERABILITY) != null) {
            getAttribute(Attributes.COMBAT_HURTINVULNERABILITY).setBaseValue(data.getNoDamageTicks());
        }

        applySpawnAttributes();

        // Love how bukkit and vanilla names don't match here lol
        if (data.getMainHand() != null) {
            setItemSlot(EquipmentSlot.MAINHAND, data.getMainHand().rollRandomStack().getVanillaStack());
        }
        if (data.getOffHand() != null) {
            setItemSlot(EquipmentSlot.OFFHAND, data.getOffHand().rollRandomStack().getVanillaStack());
        }
        if (data.getHelmet() != null) {
            setItemSlot(EquipmentSlot.HEAD, data.getHelmet().rollRandomStack().getVanillaStack());
        }
        if (data.getChest() != null) {
            setItemSlot(EquipmentSlot.CHEST, data.getChest().rollRandomStack().getVanillaStack());
        }
        if (data.getLeggings() != null) {
            setItemSlot(EquipmentSlot.LEGS, data.getLeggings().rollRandomStack().getVanillaStack());
        }
        if (data.getBoots() != null) {
            setItemSlot(EquipmentSlot.FEET, data.getBoots().rollRandomStack().getVanillaStack());
        }
        setHealth(getMaxHealth());
    }

    private void restoreConfiguredName() {
        Component name = Component.empty();
        name = name.append(Component.text("Lv. " + mobLevel + " ", NamedTextColor.GOLD));
        name = name.append(Component.text("| ", NamedTextColor.DARK_GRAY));
        if (data.getDisplayName() != null) {
            name = name.append(data.getDisplayName().color(NamedTextColor.YELLOW));
        }
        setCustomName(PaperAdventure.asVanilla(name));
    }

    public NPCData getData() {
        return data;
    }

    public boolean shouldSuppressCombatGoalsFromBehavior() {
        return behaviorController != null && behaviorController.shouldSuppressCombatGoals();
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        if (getItemInHand(InteractionHand.MAIN_HAND).getItem() == Items.CROSSBOW) { // We need to handle crossbow attacks separately
            performCrossbowAttack(this, 1.6F);
            return;
        }
        net.minecraft.world.InteractionHand hand = ProjectileUtil.getWeaponHoldingHand(this, Items.BOW); // Paper - call EntityShootBowEvent
        ItemStack itemInHand = this.getItemInHand(hand); // Paper - call EntityShootBowEvent
        ItemStack projectile = this.getProjectile(itemInHand);
        AbstractArrow arrow = this.getArrow(projectile, distanceFactor, itemInHand);
        double d = target.getX() - this.getX();
        double d1 = target.getY(0.3333333333333333) - arrow.getY();
        double d2 = target.getZ() - this.getZ();
        double squareRoot = Math.sqrt(d * d + d2 * d2);
        if (this.level() instanceof ServerLevel serverLevel) {
            Projectile.Delayed<AbstractArrow> delayedEntity = Projectile.spawnProjectileUsingShootDelayed( // Paper - delayed
                    arrow, serverLevel, projectile, d, d1 + squareRoot * 0.2F, d2, 1.6F, 14 - serverLevel.getDifficulty().getId() * 4
            );

            // Paper start - call EntityShootBowEvent
            org.bukkit.event.entity.EntityShootBowEvent event = org.bukkit.craftbukkit.event.CraftEventFactory.callEntityShootBowEvent(this, itemInHand, arrow.getPickupItem(), arrow, hand, distanceFactor, true);
            if (event.isCancelled()) {
                event.getProjectile().remove();
                return;
            }

            if (event.getProjectile() == arrow.getBukkitEntity()) {
                delayedEntity.spawn();
            }
            // Paper end - call EntityShootBowEvent
        }

        this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
    }

    protected AbstractArrow getArrow(ItemStack arrow, float velocity, @Nullable ItemStack weapon) {
        return ProjectileUtil.getMobArrow(this, arrow, velocity, weapon);
    }

    private void applySpawnAttributes() {
        Map<Holder<Attribute>, MobAttributeCurve> curves = plugin.getMobAttributeCurves();
        if (!data.usesAttributeCurves(hasSpawnLevel, curves.keySet())) {
            curves = Map.of();
        }
        for (var entry : data.getAttributes().entrySet()) {
            var instance = getAttribute(entry.getKey());
            if (instance != null && !curves.containsKey(entry.getKey())) {
                instance.setBaseValue(entry.getValue());
            }
        }
        for (var entry : curves.entrySet()) {
            var instance = getAttribute(entry.getKey());
            if (instance != null) {
                double base = entry.getValue().valueAt(mobLevel);
                instance.setBaseValue(base + data.getConfiguredAttribute(entry.getKey()));
            }
        }
        applyLevelAttributes(curves.keySet());
    }

    private void applyLevelAttributes(Set<Holder<Attribute>> curvedAttributes) {
        if (levelInfo == null || levelInfo.baseAttributeBonus().isEmpty()) {
            return;
        }

        for (Map.Entry<org.bukkit.attribute.Attribute, MobAttributeRange> entry : levelInfo.baseAttributeBonus().entrySet()) {
            org.bukkit.attribute.Attribute bukkitAttribute = entry.getKey();
            MobAttributeRange range = entry.getValue();

            try {
                Holder<Attribute> minecraftAttributeHolder = org.bukkit.craftbukkit.attribute.CraftAttribute.bukkitToMinecraftHolder(bukkitAttribute);
                var instance = getAttribute(minecraftAttributeHolder);
                if (instance == null) {
                    continue;
                }

                double bonus;
                if (range.min() == range.max()) {
                    bonus = range.min();
                } else {
                    bonus = range.min() + (random.nextDouble() * (range.max() - range.min()));
                }

                double base = curvedAttributes.contains(minecraftAttributeHolder) ? instance.getBaseValue() : 0.0;
                instance.setBaseValue(base + bonus);
            } catch (Exception e) {
                Aether.log("Failed to apply level attribute bonus for " + bukkitAttribute.getKey() + " on " + data.getID() + ": " + e.getMessage());
            }
        }
    }

    public boolean testSpellbookTeam(LivingEntity target, ServerLevel level) {
        return isValidCombatTarget(target);
    }

    public boolean isValidCombatTarget(LivingEntity target) {
        if (target == null || target == this || !target.isAlive()) {
            return false;
        }
        if (!QxlInstanceScope.canInteract(this, target)) {
            return false;
        }
        org.bukkit.entity.LivingEntity bukkitTarget;
        if (target instanceof AetherBaseMob mob) {
            bukkitTarget = mob.getBukkitLivingEntity();
        } else if (target instanceof Player player) {
            bukkitTarget = player.getBukkitEntity();
        } else {
            return false;
        }

        org.bukkit.entity.LivingEntity bukkitAttacker = getBukkitLivingEntity();
        return AetherTargetingCache.canAttack(bukkitAttacker, bukkitTarget);
    }

    public org.bukkit.entity.Player getLastAttackingPlayer() {
        return lastAttackingPlayer;
    }

    public String getInstanceScope() {
        return instanceScope;
    }

    public void setInstanceScope(String instanceScope) {
        this.instanceScope = instanceScope;
        QxlInstanceScope.syncTerrain(this);
        if (instanceScope != null && bukkitLivingEntity != null) {
            getBukkitLivingEntity().setVisibleByDefault(false);
        }
    }

    public void rememberInstanceHide(org.bukkit.entity.Player player) {
        instanceHiddenPlayers.add(player.getUniqueId());
    }

    public boolean restoreInstanceHide(org.bukkit.entity.Player player) {
        return instanceHiddenPlayers.remove(player.getUniqueId());
    }
}
