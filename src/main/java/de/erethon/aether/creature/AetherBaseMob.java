package de.erethon.aether.creature;

import de.erethon.aether.Aether;
import de.erethon.aether.ai.goals.AEPathfinderGoal;
import de.erethon.aether.combat.MobAttributeRange;
import de.erethon.aether.combat.MobLevelInfo;
import de.erethon.aether.combat.MobLevelLoot;
import de.erethon.aether.combat.MobLootEntry;
import de.erethon.aether.combat.SpellCastEntry;
import de.erethon.aether.events.CreatureDeathEvent;
import de.erethon.aether.events.CreatureInteractEvent;
import de.erethon.aether.events.CreatureLoadEvent;
import de.erethon.aether.qxl.AetherHolder;
import de.erethon.aether.tools.NMSUtils;
import de.erethon.daedalus.utils.DataMappings;
import de.erethon.hecate.Hecate;
import de.erethon.hecate.data.HCharacter;
import de.erethon.hecate.progression.LevelUtil;
import de.erethon.hephaestus.items.HItem;
import de.erethon.papyrus.CraftPDamageType;
import de.erethon.papyrus.entities.CraftCustomMob;
import de.erethon.papyrus.entities.MobSpawnCategoryOverrideProvider;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.player.QPlayer;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
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
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.bukkit.Bukkit;
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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class AetherBaseMob extends Monster implements RangedAttackMob, CrossbowAttackMob, MobSpawnCategoryOverrideProvider {

    protected Aether plugin = Aether.getInstance();
    protected NPCData data;
    protected EntityType<?> displayType;
    private int version = 0;
    protected CraftCustomMob bukkitLivingEntity;
    private AetherHolder holder;

    private int mobLevel = 1;
    private MobLevelInfo levelInfo;

    private boolean isTalking = false;
    private boolean isChargingCrossbow = false;

    private Map<Player, Double> damageTracker = new HashMap<>();


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

        if (data.hasLevels()) {
            mobLevel = Objects.requireNonNullElseGet(overrideLevel, data::selectRandomLevel);
            levelInfo = data.getCompositeLevelInfoForLevel(mobLevel);
        }

        bukkitLivingEntity = new CraftCustomMob(MinecraftServer.getServer().server, this);
        bukkitLivingEntity.setHandle(this);
        bukkitLivingEntity.setPapyrusId(data.getID());
        bukkitLivingEntity.setType(data.getDisplayType());
        entityData = DataMappings.getSynchedEntityData(data.getDisplayType());
        version = data.getCurrentVersion();
        onLoad();
        onFirstSpawn();
    }

    public void addToWorld() {
        ServerLevel level = (ServerLevel) level();
        level.addFreshEntity(this);
    }

    @Override
    public void tick() {
        super.tick();
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
    public @NotNull CraftEntity getBukkitEntity() {
        if (bukkitLivingEntity == null) {
            return new CraftCustomMob(MinecraftServer.getServer().server, this);
        } else {
            return bukkitLivingEntity;
        }
    }

    public @NotNull CraftLivingEntity getBukkitLivingEntity() {
        if (bukkitLivingEntity == null) {
            return new CraftCustomMob(MinecraftServer.getServer().server, this);
        } else {
            return bukkitLivingEntity;
        }
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
                if (qPlayer != null && holder != null) {
                    holder.onAttack(qPlayer);
                }
            } catch (Exception e) {
                Aether.log("Failed to handle attack for " + data.getID() + ": " + e.getMessage());
            }
        }
        return super.doHurtTarget(level, target);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount, CraftPDamageType type) {
        for (SpellCastEntry spell : data.getOnDamagedSpells()) {
            if (spell.getSpell() == null) {
                logDebug("Spell " + spell + " does not exist");
                continue;
            }
            logDebug("Casting onHurt spell " + spell.getSpell().getName());
            castSpell(spell);
        }
        if (source.getEntity() instanceof Player player) {
            try {
                QPlayer qPlayer = QuestsXL.get().getDatabaseManager().getCurrentPlayer((org.bukkit.entity.Player) player.getBukkitEntity());
                if (qPlayer != null && holder != null) {
                    holder.onLeftClick(qPlayer);
                    if (amount > 0) {
                        holder.onDamage(qPlayer);
                    }
                }
            } catch (Exception e) {
                Aether.log("Failed to handle damage for " + data.getID() + ": " + e.getMessage());
            }
            damageTracker.put(player, damageTracker.getOrDefault(player, 0.0) + amount);
        }
        return super.hurtServer(level, source, amount, type);
    }

    @Override
    public void onCrossbowAttackPerformed() {
        noActionTime = 0;
    }

    @Override
    public boolean canFireProjectileWeapon(ProjectileWeaponItem projectileWeapon) {
        return projectileWeapon == Items.CROSSBOW;
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
        if (displayType != null && displayType != EntityType.PLAYER) {
            return displayType;
        }
        return EntityType.PLAYER;
    }

    @Override
    public MobCategory getEffectiveMobCategory() {
        return data.getMobCategoryOverride();
    }

    @Override
    public void die(DamageSource damageSource) {
        // We need to re-implement this logic, otherwise we cause normal death events for plugins
        Entity entity = damageSource.getEntity();
        dead = true;
        getCombatTracker().recheckStatus();
        if (entity != null) {
            entity.killedEntity((ServerLevel) this.level(), this);
        }
        gameEvent(GameEvent.ENTITY_DIE);
        level().broadcastEntityEvent(this, (byte) 3);
        setPose(Pose.DYING);

        // onDeath spells
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
        // Death event
        if (damageSource.getEntity() != null && damageSource.getEntity().getBukkitEntity() instanceof org.bukkit.entity.Player player) {
            CreatureDeathEvent creatureDeathEvent = new CreatureDeathEvent(data, player, this);
            Bukkit.getPluginManager().callEvent(creatureDeathEvent);
        }

        // Calculate damage distribution and distribute loot/XP
        distributeLootAndXP();

        if (holder != null) {
            holder.onDeath();
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
                    ItemEntity itemEntity = new ItemEntity(level(), getX(), getY(), getZ(), item.rollRandomStack(mobLevel).getVanillaStack());
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

    @Override
    public boolean setTarget(LivingEntity entityliving, EntityTargetEvent.TargetReason reason) {
        boolean bool = super.setTarget(entityliving, reason);
        for (SpellCastEntry spell : data.getOnTargetSpells()) {
            if (spell.getSpell() == null) {
                logDebug("Spell " + spell + " does not exist");
                continue;
            }
            logDebug("Casting onTarget spell " + spell.getSpell().getName());
            castSpell(spell);
        }
        return bool;
    }

    @Override
    protected @NotNull InteractionResult mobInteract(Player player, InteractionHand hand) {
        CreatureInteractEvent event = new CreatureInteractEvent((org.bukkit.entity.Player) player.getBukkitEntity(), this, data);
        Bukkit.getPluginManager().callEvent(event);
        try {
            QPlayer qPlayer = QuestsXL.get().getDatabaseManager().getCurrentPlayer((org.bukkit.entity.Player) player.getBukkitEntity());
            if (qPlayer != null && holder != null) {
                holder.onRightClick(qPlayer);
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
        Display.TextDisplay textDisplay = EntityType.TEXT_DISPLAY.create(level(), EntitySpawnReason.NATURAL);
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

    private void registerAetherGoals() {
        if (data.getGoals().isEmpty() && data.getTargets().isEmpty()) {
            goalSelector.addGoal(0, new RandomStrollGoal(this, 1));
            goalSelector.addGoal(1, new RandomLookAroundGoal(this));
            goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.4, false));
            targetSelector.addGoal(0, new HurtByTargetGoal(this));
            targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
            return;
        }
        for (AEPathfinderGoal aeGoal : data.getGoals()) {
            goalSelector.addGoal(aeGoal.getPrio(), aeGoal.get(this));
        }
        if (data.getTargets().isEmpty()) {
            targetSelector.addGoal(0, new HurtByTargetGoal(this));
            targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
            return;
        }
        for (AEPathfinderGoal aeGoal : data.getTargets()) {
            targetSelector.addGoal(aeGoal.getPrio(), aeGoal.get(this));
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        Optional<String> papyrusId = input.getString("papyrus-entity-id");
        if (papyrusId.isEmpty()) {
            return;
        }
        data = plugin.getCreatureManager().getByID(papyrusId.get());
        if (data == null) {
            plugin.getLogger().warning("Failed to load entity data for " + input.getString("papyrus-entity-id") + " for entity " + this);
            remove(RemovalReason.DISCARDED);
            return;
        }
        this.displayType = data.getDisplayType();
        Optional<Integer> papyrusVersion = input.getInt("papyrus-entity-version");
        version = papyrusVersion.orElse(0);

        Optional<Integer> savedLevel = input.getInt("aether-mob-level");
        if (savedLevel.isPresent()) {
            mobLevel = savedLevel.get();
            levelInfo = data.getCompositeLevelInfoForLevel(mobLevel);
            Aether.log("Loaded " + data.getID() + " at level " + mobLevel);
        }

        bukkitLivingEntity = new CraftCustomMob(MinecraftServer.getServer().server, this);
        bukkitLivingEntity.setHandle(this);
        bukkitLivingEntity.setPapyrusId(data.getID());
        bukkitLivingEntity.setType(data.getDisplayType());
        onLoad();
        if (version <= data.getCurrentVersion()) {
            plugin.getLogger().warning("Entity " + data.getID() + " (" + getUUID() + ") is outdated. Updating...");
            onFirstSpawn();
        }
    }

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("papyrus-entity-id", data.getID());
        output.putInt("aether-mob-version", version);
        output.putInt("aether-mob-level", mobLevel);
    }

    private void onLoad() {
        ServerLevel level = (ServerLevel) level();
        level.chunkSource.removeEntity(this);
        if (data.getQXLSection() != null) {
            holder = AetherHolder.loadFromConfigSection(data.getQXLSection(), this);
            if (holder != null) {
                holder.onSpawn();
            }
        }
        registerAetherGoals();
        if (data.getHomeLocation() != null) {
            setHomeTo(data.getHomeLocation(), data.getHomeRange());
        }
        drops.clear();
        setDropChance(EquipmentSlot.HEAD, 0);
        setDropChance(EquipmentSlot.CHEST, 0);
        setDropChance(EquipmentSlot.LEGS, 0);
        setDropChance(EquipmentSlot.FEET, 0);
        setDropChance(EquipmentSlot.MAINHAND, 0);
        setDropChance(EquipmentSlot.OFFHAND, 0);
        expToDrop = data.getDropXP();
        setCanPickUpLoot(false);

        CreatureLoadEvent event = new CreatureLoadEvent(data, this);
        Bukkit.getPluginManager().callEvent(event);
    }

    protected void onFirstSpawn() {
        getBukkitEntity().getPersistentDataContainer().set(plugin.getKey(), PersistentDataType.BOOLEAN, true);
        Component name = Component.empty();
        name = name.append(Component.text("Lv. " + mobLevel + " ", NamedTextColor.GOLD));
        name = name.append(Component.text("| ", NamedTextColor.DARK_GRAY));
        if (data.getDisplayName() != null) {
            name = name.append(data.getDisplayName().color(NamedTextColor.YELLOW));
        }
        setCustomName(PaperAdventure.asVanilla(name));
        setGlowingTag(data.isGlowing());
        setNoGravity(!data.isGravity());
        setInvulnerable(data.isInvulnerable());
        setPersistenceRequired(data.isPersistent());
        collides = data.hasCollision();
        maxAirTicks = data.getMaximumAir();

        applyLevelAttributes();

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

    public NPCData getData() {
        return data;
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

    private void applyLevelAttributes() {
        if (levelInfo == null || levelInfo.baseAttributeBonus().isEmpty()) {
            return;
        }

        for (Map.Entry<org.bukkit.attribute.Attribute, MobAttributeRange> entry : levelInfo.baseAttributeBonus().entrySet()) {
            org.bukkit.attribute.Attribute bukkitAttribute = entry.getKey();
            MobAttributeRange range = entry.getValue();

            try {
                Holder<Attribute> minecraftAttributeHolder = org.bukkit.craftbukkit.attribute.CraftAttribute.bukkitToMinecraftHolder(bukkitAttribute);
                if (getAttribute(minecraftAttributeHolder) == null) {
                    continue;
                }

                double bonus;
                if (range.min() == range.max()) {
                    bonus = range.min();
                } else {
                    bonus = range.min() + (random.nextDouble() * (range.max() - range.min()));
                }

                double currentValue = getAttribute(minecraftAttributeHolder).getBaseValue();
                double newValue = currentValue + bonus;
                getAttribute(minecraftAttributeHolder).setBaseValue(newValue);
            } catch (Exception e) {
                Aether.log("Failed to apply level attribute bonus for " + bukkitAttribute.getKey() + " on " + data.getID() + ": " + e.getMessage());
            }
        }
    }

}

