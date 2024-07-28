package de.erethon.aether.ai.goals.custom;

import com.google.common.base.Predicates;
import de.erethon.aether.ai.SpellTargetMode;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookAPI;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

public class CastSpellbookSpellGoalImpl extends Goal {

    private final Mob mob;
    private final SpellTargetMode mode;
    private final SpellData spell;
    private final float targetMaxPitchChange;
    private final float targetMaxYawChange;
    private final double range;
    private final int cooldownBetween;

    public CastSpellbookSpellGoalImpl(Mob mob, SpellTargetMode mode, SpellData spell, int cooldownBetween, double range, float targetMaxPitchChange, float targetMaxYawChange) {
        this.mob = mob;
        this.mode = mode;
        this.spell = spell;
        this.targetMaxPitchChange = targetMaxPitchChange;
        this.targetMaxYawChange = targetMaxYawChange;
        this.range = range;
        this.cooldownBetween = cooldownBetween;
    }

    @Override
    public boolean canUse() {
        if (cooldownBetween > 0) {
            return mob.tickCount % cooldownBetween == 0;
        }
        return true;
    }

    @Override
    public void tick() {
        if (mode == SpellTargetMode.TARGET) {
            if (mob.getTarget() == null) {
                return;
            }
            mob.lookAt(mob.getTarget(), targetMaxYawChange, targetMaxPitchChange);
        }
        if (mode == SpellTargetMode.NEAREST) {
            for (Entity entity : mob.level().getEntities(this.mob, this.mob.getBoundingBox().inflate(range, range / 2, range), Predicates.alwaysTrue())) {
                if (entity instanceof Player player && !player.canUseGameMasterBlocks()) {
                    mob.lookAt(entity, targetMaxYawChange, targetMaxPitchChange);
                    break;
                }
            }
        }
        if (mode == SpellTargetMode.LAST_DAMAGE && mob.getLastAttacker() instanceof Player) {
            mob.lookAt(mob.getLastAttacker(), targetMaxYawChange, targetMaxPitchChange);
        }
        SpellbookAPI.getInstance().getQueue().addToQueue(spell.getActiveSpell(mob.getBukkitLivingEntity()));
    }
}
