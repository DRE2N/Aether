package de.erethon.aether.models;

import de.erethon.bedrock.chat.MessageUtil;
import net.minecraft.world.level.Level;
import net.worldseed.multipart.GenericModelImpl;
import net.worldseed.util.math.Pos;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AEModel extends GenericModelImpl {

    private final String modelId;

    public AEModel(String modelId) {
        this.modelId = modelId;
    }

    @Override
    public String getId() {
        return modelId;
    }

    @Override
    public void init(@Nullable Level level, @NotNull Pos position, float scale) {
        super.init(level, position, scale);
        MessageUtil.log("Model initialized at " + position + " with scale " + scale + " in level " + level);
    }

    @Override
    public Level getInstance() {
        return null;
    }

    @Override
    public void bindNametag(String s, Entity entity) {

    }

    @Override
    public void unbindNametag(String s) {

    }

    @Override
    public @Nullable Entity getNametag(String s) {
        return null;
    }
}
