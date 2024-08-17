package de.erethon.aether.creature;

import de.erethon.aether.Aether;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.Level;
import team.unnamed.hephaestus.Model;
import team.unnamed.hephaestus.view.track.ModelViewTrackingRule;

public class AetherModelEntity extends Pig {

    public AetherModelEntity(EntityType<? extends Pig> type, Level world) {
        super(type, world);
    }

    public void addModel() {
        Aether plugin = Aether.getInstance();
        Model model = plugin.getModelRegistry().model("redstone_monstrosity");
        plugin.getModelEngine().spawn(model, this.getBukkitEntity());
    }


}
