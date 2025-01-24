package de.erethon.aether.creature;

import com.destroystokyo.paper.profile.CraftPlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import de.erethon.aether.Aether;
import de.erethon.aether.ai.goals.AEPathfinderGoal;
import de.erethon.aether.combat.SpellCastEntry;
import de.erethon.aether.events.CreatureDeathEvent;
import de.erethon.aether.events.CreatureInteractEvent;
import de.erethon.aether.events.CreatureLoadEvent;
import de.erethon.aether.tools.NMSUtils;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.hephaestus.items.HItem;
import de.erethon.papyrus.CraftPDamageType;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftSound;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import team.unnamed.hephaestus.Model;
import team.unnamed.hephaestus.animation.Animation;
import team.unnamed.hephaestus.bukkit.ModelView;
import team.unnamed.hephaestus.view.track.ModelViewTrackingRule;

import java.util.EnumSet;
import java.util.Map;
import java.util.function.Consumer;

public class AetherBaseMob extends Monster implements RangedAttackMob, CrossbowAttackMob {

    private Aether plugin = Aether.getInstance();
    private NPCData data;
    private ModelView modelView;
    private Model model;
    private Entity dataEntity;
    private int version = 0;

    private boolean isTalking = false;

   Consumer<Packet<?>> packetConsumer = packet -> {

   };

    // Constructor for entity loading
    public AetherBaseMob(EntityType<? extends Mob> type, Level world) {
        super((EntityType<? extends Monster>) type, world);
    }

    public AetherBaseMob(NPCData data, World world) {
        super((EntityType<? extends Monster>) data.getDisplayType(), ((CraftWorld) world).getHandle());
        this.data = data;
        onLoad();
        onFirstSpawn();
    }

    @ApiStatus.Obsolete
    public void addToWorld() {
        if (dataEntity == null) {
            return;
        }
        dataEntity.setPos(getX(), getY(), getZ());
        dataEntity.setId(getId());
        ServerLevel level = (ServerLevel) level();
        level.addFreshEntity(this);
        if (model != null) {
            modelView = plugin.getModelEngine().spawn(model, this.getBukkitEntity(), ModelViewTrackingRule.all());
            for (Map.Entry<String, Animation> animation : model.animations().entrySet()) {
                MessageUtil.log("Animation: " + animation.getKey());
            }
            modelView.animationPlayer().add(model.animations().get("attack"));
        }
    }

    @Override
    public void tick() {
        super.tick();
        dataEntity.setPos(getX(), getY(), getZ());
        if (modelView != null) {
            modelView.tickAnimations();
        }
    }

    private void logDebug(String message) {
        getBukkitEntity().getLocation().getNearbyPlayers(8).forEach(p -> p.sendMessage(Component.text("[DEBUG] " + message)));
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket(@NotNull ServerEntity entity) {
        if (dataEntity instanceof Player player) {
            for (ServerPlayerConnection connection : moonrise$getTrackedEntity().seenBy) {
                CraftPlayerProfile craftPlayerProfile = new CraftPlayerProfile(getUUID(), PlainTextComponentSerializer.plainText().serialize(Component.empty()));
                Skin skin = plugin.getSkinCache().get(data.getSkinLink());
                if (skin != null) {
                    craftPlayerProfile.getProperties().add(new ProfileProperty("textures", skin.texture(), skin.signature()));
                }
                player.getEntityData().set(Player.DATA_PLAYER_MODE_CUSTOMISATION, (byte) 127); // Show all skin layers
                player.setId(dataEntity.getId());
                player.getEntityData().markDirty(Player.DATA_PLAYER_MODE_CUSTOMISATION);
                player.setCustomName(PaperAdventure.asVanilla(data.getDisplayName()));
                ClientboundPlayerInfoUpdatePacket infoUpdatePacket = new ClientboundPlayerInfoUpdatePacket(
                        EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, ClientboundPlayerInfoUpdatePacket.Action.INITIALIZE_CHAT, ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE, ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED, ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY),
                        new ClientboundPlayerInfoUpdatePacket.Entry(getUUID(), craftPlayerProfile.buildGameProfile(), false, -1, GameType.SURVIVAL, net.minecraft.network.chat.Component.empty(), true, 0, null));

                ClientboundSetEntityDataPacket entityDataPacket = new ClientboundSetEntityDataPacket(player.getId(), player.getEntityData().packDirty());
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
    public void refreshEntityData(@NotNull ServerPlayer to) {
        dataEntity.refreshEntityData(to);
    }

    @Override
    public CraftEntity getBukkitEntity() {
        if (dataEntity == null) { // Workaround for initialization issues
            return EntityType.PIG.create(level(), EntitySpawnReason.NATURAL).getBukkitEntity();
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
            lookAt(getTarget(),180f, 180f); // I hope large values are enough
            logDebug("Looking at target...");
        }
        entry.getSpell().queue(caster);
        logDebug("Cast spell " + entry.getSpell().getName() + "!");
    }

    /*@Override
    public boolean doHurtTarget(Entity target, CraftPDamageType type) {
        for (SpellCastEntry spell : data.getOnAttackSpells()) {
            if (spell.getSpell() == null) {
                logDebug("Spell " + spell + " does not exist");
                continue;
            }
            logDebug("Casting onAttack spell " + spell.getSpell().getName());
            castSpell(spell);
        }
        return super.doHurtTarget(target, type);
    }

    @Override
    public boolean hurt(DamageSource source, float amount, CraftPDamageType type) {
        for (SpellCastEntry spell : data.getOnDamagedSpells()) {
            if (spell.getSpell() == null) {
                logDebug("Spell " + spell + " does not exist");
                continue;
            }
            logDebug("Casting onHurt spell " + spell.getSpell().getName());
            castSpell(spell);
        }
        return super.hurt(source, amount, type);
    }*/

    @Override
    public void setChargingCrossbow(boolean charging) {
        if (dataEntity instanceof CrossbowAttackMob crossbowAttackMob) {
            crossbowAttackMob.setChargingCrossbow(charging);
        }
    }

    @Override
    public void onCrossbowAttackPerformed() {
        // This is apparently useless, thanks Mojang
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
                MessageUtil.log("Failed to cast spell " + spell.getSpell().getName() + " for " + data.getID() + ": " + e.getMessage());
            }
        }
        // Death event
        if (damageSource.getEntity() != null && damageSource.getEntity().getBukkitEntity() instanceof org.bukkit.entity.Player player) {
            CreatureDeathEvent creatureDeathEvent = new CreatureDeathEvent(data, player, this);
            Bukkit.getPluginManager().callEvent(creatureDeathEvent);
        }
        // Drop loot
        try { // Exceptions here crash the server
            for (Map.Entry<HItem, Float> entry : data.getLoot().entrySet()) {
                HItem item = entry.getKey();
                float chance = entry.getValue() / 100;
                if (chance < 1 && random.nextFloat() > chance) {
                    continue;
                }
                ItemEntity itemEntity = new ItemEntity(level(), getX(), getY(), getZ(), item.rollRandomStack().getVanillaStack());
                logDebug("Dropping loot " + item.getPatch().toString() + ", chance was " + chance * 100 + "%");
                level().addFreshEntity(itemEntity);
            }
            if (data.getDropXP() > 0) {
                level().addFreshEntity(new ExperienceOrb(level(), getX(), getY(), getZ(), data.getDropXP(), org.bukkit.entity.ExperienceOrb.SpawnReason.ENTITY_DEATH, entity, this));
            }
        } catch (Throwable e) {
            MessageUtil.log("Failed to drop loot for " + data.getID() + ": " + e.getMessage());
        }
    }

    @Override
    public boolean setTarget(LivingEntity entityliving, EntityTargetEvent.TargetReason reason, boolean fireEvent) {
        boolean bool = super.setTarget(entityliving, reason, fireEvent);
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
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        CreatureInteractEvent event = new CreatureInteractEvent((org.bukkit.entity.Player) player.getBukkitEntity(), this, data);
        Bukkit.getPluginManager().callEvent(event);
        return super.mobInteract(player, hand);
    }

    // Dialogue stuff
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


    public @NotNull CraftLivingEntity getBukkitLivingEntity() {
        if (dataEntity instanceof LivingEntity livingEntity) { // Workaround for initialization issues
            return livingEntity.getBukkitLivingEntity();
        }
        return EntityType.PIG.create(level(), EntitySpawnReason.NATURAL).getBukkitLivingEntity(); // never reached anyway
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
            MessageUtil.log("Added goal " + aeGoal.get(this).getClass().getSimpleName() + " to " + getData().getID());
        }
        for (AEPathfinderGoal aeGoal : data.getTargets()) {
            targetSelector.addGoal(aeGoal.getPrio(), aeGoal.get(this));
            MessageUtil.log("Added target " + aeGoal.get(this).getClass().getSimpleName() + " to " + getData().getID());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        data = plugin.getCreatureManager().getByID(nbt.getString("papyrus-entity-id"));
        if (data == null) {
            plugin.getLogger().warning("Failed to load entity data for " + nbt.getString("papyrus-entity-id") + " for entity " + this);
            remove(RemovalReason.DISCARDED);
            return;
        }
        version = nbt.getInt("aether-mob-version");
        onLoad();
        if (version <= data.getCurrentVersion()) {
            plugin.getLogger().warning("Entity " + data.getID() + " (" + getUUID() + ") is outdated. Updating...");
            onFirstSpawn();
        }
    }

    private void onLoad() {
        dataEntity = data.getDisplayType().create(level(), EntitySpawnReason.NATURAL);
        if (data.getDisplayType() == EntityType.PLAYER) {
            dataEntity = new ServerPlayer(MinecraftServer.getServer(), (ServerLevel) level(), new CraftPlayerProfile(getUUID(), "NPC").buildGameProfile(), ClientInformation.createDefault());
            ServerPlayer player = (ServerPlayer) dataEntity;
            player.moonrise$setRealPlayer(false);
        }
        ServerLevel level = (ServerLevel) level();
        level.chunkSource.removeEntity(this);
        if (data.getModelID() != null) {
            model = plugin.getModelRegistry().model(data.getModelID());
        }
        if (dataEntity == null) {
            plugin.getLogger().warning("Failed to create entity for " + data.getDisplayType());
            remove(RemovalReason.DISCARDED);
            return;
        }
        registerAetherGoals();
        if (data.getHomeLocation() != null) {
            restrictTo(data.getHomeLocation(), data.getHomeRange());
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

    private void onFirstSpawn() {
        getBukkitEntity().getPersistentDataContainer().set(plugin.getKey(), PersistentDataType.BOOLEAN, true);
        dataEntity.setCustomName(PaperAdventure.asVanilla(data.getDisplayName()));
        setCustomName(PaperAdventure.asVanilla(data.getDisplayName()));
        setGlowingTag(data.isGlowing());
        setNoGravity(!data.isGravity());
        setInvulnerable(data.isInvulnerable());
        setPersistenceRequired(data.isPersistent());
        collides = data.hasCollision();
        maxAirTicks = data.getMaximumAir();
        for (Map.Entry<Holder<Attribute>, Double> entry : data.getAttributes().entrySet()) {
            if (getAttribute(entry.getKey()) == null) {
                continue;
            }
            getAttribute(entry.getKey()).setBaseValue(entry.getValue());
            // Handle health separately
            if (entry.getKey().equals(Attributes.MAX_HEALTH)) {
                setHealth(entry.getValue().floatValue());
            }
        }
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
    }

    public NPCData getData() {
        return data;
    }

    @Override
    public void performRangedAttack(LivingEntity target, float pullProgress) {
        // Copied from AbstractSkeleton
        ItemStack itemstack = this.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, Items.BOW));
        if (itemstack.getItem() != Items.BOW) {
            performCrossbowAttack(target, pullProgress); // Maybe we have a crossbow
            return;
        }
        ItemStack itemstack1 = this.getProjectile(itemstack);
        AbstractArrow entityarrow = ProjectileUtil.getMobArrow(this, itemstack1, pullProgress, itemstack);
        double d0 = target.getX() - this.getX();
        double d1 = target.getY(0.3333333333333333D) - entityarrow.getY();
        double d2 = target.getZ() - this.getZ();
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);
        entityarrow.shoot(d0, d1 + d3 * 0.20000000298023224D, d2, 1.6F, (float) (14 - this.level().getDifficulty().getId() * 4));
        this.level().addFreshEntity(entityarrow);
        this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putString("papyrus-entity-id", data.getID());
        nbt.putInt("aether-mob-version", version);
    }
}
