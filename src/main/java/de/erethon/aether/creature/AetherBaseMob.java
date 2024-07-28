package de.erethon.aether.creature;

import com.google.common.base.Predicates;
import de.erethon.aether.Aether;
import de.erethon.aether.ai.goals.AEPathfinderGoal;
import de.erethon.aether.tools.NMSUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import team.unnamed.hephaestus.Model;
import team.unnamed.hephaestus.bukkit.ModelView;

public class AetherBaseMob extends PathfinderMob {

    private Aether plugin = Aether.getInstance();
    private NPCData data;
    private ModelView modelView;
    private Entity dataEntity;

    // Constructor for entity loading
    public AetherBaseMob(EntityType<? extends Mob> type, Level world) {
        super((EntityType<? extends PathfinderMob>) type, world);
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
    protected void registerGoals() {
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
        if (data.getModelID() != null) {
            Model model = plugin.getModelRegistry().model(data.getModelID());
            if (model == null) {
                plugin.getLogger().warning("Failed to load model for " + data.getModelID());
                return;
            }
            modelView = plugin.getModelEngine().spawn(model, getBukkitEntity());
        }
        dataEntity = data.getDisplayType().create(level());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putString("papyrus-entity-id", data.getID());
    }
}
