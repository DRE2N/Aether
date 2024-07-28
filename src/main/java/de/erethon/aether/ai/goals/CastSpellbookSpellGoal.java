package de.erethon.aether.ai.goals;

import de.erethon.aether.ai.SpellTargetMode;
import de.erethon.aether.ai.goals.custom.CastSpellbookSpellGoalImpl;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellbookAPI;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.Locale;

public class CastSpellbookSpellGoal extends AEPathfinderGoal {

    private SpellData spell;
    private SpellTargetMode mode;
    private int cooldownBetween;
    private double range;
    private float targetMaxPitchChange = 30;
    private float targetMaxYawChange = 40;

    @Override
    public Goal get(LivingEntity entity) {
        return new CastSpellbookSpellGoalImpl((Mob) entity, mode, spell, cooldownBetween, range, targetMaxPitchChange, targetMaxYawChange);
    }

    @Override
    public void load(String[] args) {
        spell = SpellbookAPI.getInstance().getLibrary().getSpellByID(args[0]);
        mode = SpellTargetMode.valueOf(args[1].toUpperCase(Locale.ROOT));
        if (args.length >= 3) {
            cooldownBetween = Integer.parseInt(args[2]);
        }
        if (args.length >= 4) {
            range = Double.parseDouble(args[3]);
        }
        if (args.length >= 5) {
            targetMaxYawChange = Float.parseFloat(args[4]);
        }
        if (args.length >= 6) {
            targetMaxPitchChange = Float.parseFloat(args[5]);
        }
    }
}
