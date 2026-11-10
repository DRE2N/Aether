package de.erethon.aether.ai.goals;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

final class TargetEntityClassResolver {

    private static final Map<EntityType<?>, Class<? extends LivingEntity>> TYPE_CLASSES = buildTypeClasses();

    private TargetEntityClassResolver() {
    }

    static Optional<Class<? extends LivingEntity>> resolve(String input) {
        Identifier identifier = parseIdentifier(input);
        if (identifier == null) {
            return Optional.empty();
        }
        if ("minecraft".equals(identifier.getNamespace()) && "player".equals(identifier.getPath())) {
            return Optional.of(Player.class);
        }
        return BuiltInRegistries.ENTITY_TYPE.getOptional(identifier)
                .flatMap(type -> Optional.ofNullable(TYPE_CLASSES.get(type)));
    }

    private static Identifier parseIdentifier(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String normalized = input.toLowerCase();
        if (normalized.contains(":")) {
            return Identifier.tryParse(normalized);
        }
        return Identifier.fromNamespaceAndPath("minecraft", normalized);
    }

    private static Map<EntityType<?>, Class<? extends LivingEntity>> buildTypeClasses() {
        Map<EntityType<?>, Class<? extends LivingEntity>> classes = new HashMap<>();
        for (Field field : net.minecraft.world.entity.EntityTypes.class.getFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || !EntityType.class.isAssignableFrom(field.getType())) {
                continue;
            }
            Class<? extends LivingEntity> entityClass = getLivingEntityClass(field);
            if (entityClass == null) {
                continue;
            }
            try {
                classes.put((EntityType<?>) field.get(null), entityClass);
            } catch (IllegalAccessException ignored) {
            }
        }
        return classes;
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends LivingEntity> getLivingEntityClass(Field field) {
        Type genericType = field.getGenericType();
        if (!(genericType instanceof ParameterizedType parameterizedType)) {
            return null;
        }
        Type actualType = parameterizedType.getActualTypeArguments()[0];
        if (!(actualType instanceof Class<?> entityClass) || !LivingEntity.class.isAssignableFrom(entityClass)) {
            return null;
        }
        return (Class<? extends LivingEntity>) entityClass;
    }
}
