package de.erethon.aether.creature;

import com.magmaguy.freeminecraftmodels.customentity.DynamicEntity;
import com.magmaguy.freeminecraftmodels.utils.DataMappings;
import de.erethon.aether.Aether;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import org.bukkit.World;

public class ModelledMob extends AetherBaseMob {

    private DynamicEntity model;

    public ModelledMob(EntityType<? extends Mob> type, Level world) {
        super(type, world);
    }

    public ModelledMob(NPCData data, World world, Integer overrideLevel) {
        super(data, world, overrideLevel);
    }

    public ModelledMob(NPCData data, World world) {
        super(data, world);
        displayType = EntityType.PILLAGER; // Hardcode this for now
        String id = data.cfg.getString("modelId");
        if (id == null) {
            Aether.addException("ModelledMob " + data.getID(), "Missing  modelId", "Add a modelId or remove the ModelledMob base class", null);
            return;
        }
        entityData = DataMappings.getSynchedEntityData(displayType);
        this.model = DynamicEntity.create(id, getBukkitLivingEntity());
        setInvisible(true);
    }

    @Override
    public void addToWorld() {
        super.addToWorld();
    }

    private void syncSkeletonWithEntity() {
        if (isDeadOrDying()) return;
        model.getSkeleton().setCurrentHeadPitch(getBukkitLivingEntity().getEyeLocation().getPitch());
        model.getSkeleton().setCurrentHeadYaw(getBukkitLivingEntity().getEyeLocation().getYaw());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer serverPlayer) {
        super.startSeenByPlayer(serverPlayer);
        refreshEntityData(serverPlayer);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer serverPlayer) {
        super.stopSeenByPlayer(serverPlayer);
    }

    @Override
    protected void onFirstSpawn() {
        setInvisible(true);
    }

    @Override
    public void tick() {
        super.tick();
        syncSkeletonWithEntity();
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        model.remove();
    }
}
