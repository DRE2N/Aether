package de.erethon.aether.creature;

import de.erethon.aether.Aether;
import de.erethon.aether.models.AEModel;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.worldseed.multipart.animations.AnimationHandler;
import net.worldseed.multipart.animations.AnimationHandlerImpl;
import net.worldseed.util.DataMappings;
import net.worldseed.util.math.Pos;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ModelledMob extends AetherBaseMob {

    private AEModel model;
    private AnimationHandler animationHandler;

    public ModelledMob(EntityType<? extends Mob> type, Level world) {
        super(type, world);
    }

    public ModelledMob(NPCData data, World world) {
        super(data, world);
        displayType = EntityType.PILLAGER; // Hardcode this for now
        String id = data.cfg.getString("model_id");
        if (id == null) {
            Aether.addException("ModelledMob " + data.getID(), "Missing  model_id", "Add a model_id or remove the ModelledMob base class", null);
            return;
        }
        entityData = DataMappings.getSynchedEntityData(displayType);
        this.model = new AEModel(id);
        CraftWorld cw = (CraftWorld) world;
        Level level = cw.getHandle();
        model.init(level, new Pos(getX(), getY(), getZ()), 1.0f);
        this.animationHandler = new AnimationHandlerImpl(model);
        animationHandler.playRepeat("walk");
        setInvisible(true);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer serverPlayer) {
        super.startSeenByPlayer(serverPlayer);
        model.addViewer(serverPlayer);
        refreshEntityData(serverPlayer);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer serverPlayer) {
        super.stopSeenByPlayer(serverPlayer);
        model.removeViewer(serverPlayer);
    }

    @Override
    protected void onFirstSpawn() {
        setInvisible(true);
    }

    @Override
    public void tick() {
        super.tick();
        if (!dead) {
            model.setPosition(new Pos(this.getX(), this.getY(), this.getZ()));
            model.setGlobalRotation(getYRot(), getXRot());
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        //model.remove();
    }
}
