package de.erethon.aether.ai.behavior.actions;

import de.erethon.aether.Aether;
import de.erethon.aether.ai.SpellTargetMode;
import de.erethon.aether.ai.behavior.AetherAction;
import de.erethon.aether.ai.behavior.BehaviorContext;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookAPI;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class CastSpellAction extends AetherAction {

    private final String spellId;
    private final SpellTargetMode mode;
    private final int cooldownTicks;
    private final double range;

    public CastSpellAction(String spellId, SpellTargetMode mode, int cooldownTicks, double range) {
        this.spellId = spellId;
        this.mode = mode;
        this.cooldownTicks = cooldownTicks;
        this.range = range;
    }

    @Override
    public void execute(BehaviorContext context) {
        String cooldownKey = "spell:" + spellId;
        if (!context.isCooldownReady(cooldownKey)) {
            return;
        }

        SpellData spell = SpellbookAPI.getInstance().getLibrary().getSpellByID(spellId);
        if (spell == null) {
            Aether.addException(context.mob().getData().getID(), "Unknown spell in cast_spell action: " + spellId, "Use a valid Spellbook spell ID", null);
            return;
        }

        LivingEntity target = context.target();
        if (mode == SpellTargetMode.TARGET && target != null) {
            context.mob().lookAt(target, 180, 180);
        } else if (mode == SpellTargetMode.LAST_DAMAGE && context.mob().getLastHurtByMob() != null) {
            context.mob().lookAt(context.mob().getLastHurtByMob(), 180, 180);
        } else if (mode == SpellTargetMode.NEAREST) {
            Entity nearest = context.mob().level().getNearestPlayer(context.mob().getX(), context.mob().getY(), context.mob().getZ(), range, false);
            if (nearest != null) {
                context.mob().lookAt(nearest, 180, 180);
            }
        }

        SpellbookAPI.getInstance().getQueue().addToQueue(spell.getActiveSpell(context.mob().getBukkitLivingEntity()));
        context.setCooldown(cooldownKey, cooldownTicks);
    }
}

