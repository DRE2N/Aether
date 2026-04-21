package de.erethon.aether.ai.behavior;

import de.erethon.aether.creature.AetherBaseMob;
import de.erethon.aether.qxl.AetherHolder;
import net.minecraft.world.entity.LivingEntity;
import org.bukkit.entity.Player;

public class BehaviorContext {

    private final MobBehaviorController controller;
    private final AetherBaseMob mob;

    public BehaviorContext(MobBehaviorController controller, AetherBaseMob mob) {
        this.controller = controller;
        this.mob = mob;
    }

    public AetherBaseMob mob() {
        return mob;
    }

    public LivingEntity target() {
        return mob.getTarget();
    }

    public Player lastAttacker() {
        return mob.getLastAttackingPlayer();
    }

    public AetherHolder holder() {
        return mob.holder;
    }

    public int stateTicks() {
        return controller.getStateTicks();
    }

    public int worldTicks() {
        return controller.getWorldTicks();
    }

    public double healthPercent() {
        double max = Math.max(1.0, mob.getMaxHealth());
        return (mob.getHealth() / max) * 100.0;
    }

    public boolean isCooldownReady(String key) {
        return controller.isCooldownReady(key);
    }

    public void setCooldown(String key, int ticks) {
        controller.setCooldown(key, ticks);
    }
}

