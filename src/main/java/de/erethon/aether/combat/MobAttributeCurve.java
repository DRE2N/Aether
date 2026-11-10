package de.erethon.aether.combat;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

public record MobAttributeCurve(double base, double growth, double exponent) {

    private static final int MAX_CURVE_LEVEL = 20;

    public double valueAt(int level) {
        return base * Math.pow(1.0 + growth * (Math.clamp(level, 1, MAX_CURVE_LEVEL) - 1), exponent);
    }

    public static Map<Holder<Attribute>, MobAttributeCurve> load(ConfigurationSection config) {
        ConfigurationSection curves = config.getConfigurationSection("attributeCurves");
        if (curves == null) {
            throw new IllegalArgumentException("Expected an attributeCurves section");
        }
        Map<Holder<Attribute>, MobAttributeCurve> loaded = new HashMap<>();
        for (String name : curves.getKeys(false)) {
            Identifier id = Identifier.tryParse(name);
            Holder<Attribute> attribute = id == null ? null : BuiltInRegistries.ATTRIBUTE.get(id).orElse(null);
            ConfigurationSection entry = curves.getConfigurationSection(name);
            if (attribute == null || entry == null) {
                throw new IllegalArgumentException("Invalid attribute curve: " + name);
            }
            double base = entry.getDouble("base", Double.NaN);
            double growth = entry.getDouble("growth", Double.NaN);
            double exponent = entry.getDouble("exponent", Double.NaN);
            MobAttributeCurve curve = new MobAttributeCurve(base, growth, exponent);
            if (!Double.isFinite(base) || base < 0 || (attribute.equals(Attributes.MAX_HEALTH) && base <= 0)
                    || !Double.isFinite(growth) || growth < 0 || !Double.isFinite(exponent) || exponent <= 0
                    || !Double.isFinite(curve.valueAt(MAX_CURVE_LEVEL))) {
                throw new IllegalArgumentException("Invalid base, growth, or exponent for " + name);
            }
            loaded.put(attribute, curve);
        }
        return loaded;
    }
}
