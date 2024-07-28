package de.erethon.aether.models;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import team.unnamed.hephaestus.Model;
import team.unnamed.hephaestus.bukkit.ModelView;
import team.unnamed.hephaestus.bukkit.track.ModelViewPersistenceHandler;

import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public final class ModelViewPersistenceHandlerImpl implements ModelViewPersistenceHandler {

    private static final NamespacedKey MODEL_KEY = new NamespacedKey("aether", "model");

    private final ModelRegistry modelRegistry;

    public ModelViewPersistenceHandlerImpl(final @NotNull ModelRegistry modelRegistry) {
        this.modelRegistry = requireNonNull(modelRegistry, "modelRegistry");
    }

    @Override
    public @NotNull CompletableFuture<Model> determineModel(final @NotNull Entity entity) {
        final var data = entity.getPersistentDataContainer();
        final var modelName = data.get(MODEL_KEY, PersistentDataType.STRING);

        if (modelName == null) {
            // This entity doesn't specify a model
            return CompletableFuture.completedFuture(null);
        }

        final var model = modelRegistry.model(modelName);
        if (model == null) {
            // This entity specifies an unknown model
            System.err.println("Entity with UUID: " + entity.getUniqueId() + " specifies an unknown model: " + modelName + "!");
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.completedFuture(model);
    }

    @Override
    public void saveModel(final @NotNull Entity entity, final @NotNull ModelView view) {
        final var model = view.model();
        final var data = entity.getPersistentDataContainer();
        data.set(MODEL_KEY, PersistentDataType.STRING, model.name());
    }
}