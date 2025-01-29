package de.erethon.aether.models;

import de.erethon.aether.Aether;
import net.worldseed.multipart.ModelEngine;
import net.worldseed.resourcepack.PackBuilder;
import org.apache.commons.io.FileUtils;
import org.bukkit.Material;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import org.zeroturnaround.zip.ZipUtil;

public class AEModelManager {

    private static final Path BASE_PATH = Aether.getInstance().getDataPath();
    private static final Path ZIP_PATH = BASE_PATH.resolve("resourcepack.zip");
    private static final Path MODEL_PATH = BASE_PATH.resolve("models");

    private final ModelEngine engine;

    public AEModelManager() {
        engine = new ModelEngine(Aether.getInstance());
        try {
            try {
                FileUtils.deleteDirectory(BASE_PATH.resolve("resourcepack").toFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (IllegalArgumentException ignored) {
        }
        ModelEngine.setModelMaterial(Material.MAGMA_CREAM);

        try {
            FileUtils.copyDirectory(BASE_PATH.resolve("resourcepack_template").toFile(), BASE_PATH.resolve("resourcepack").toFile());
            var config = PackBuilder.Generate(BASE_PATH.resolve("bbmodel"), BASE_PATH.resolve("resourcepack"), MODEL_PATH);
            FileUtils.writeStringToFile(BASE_PATH.resolve("model_mappings.json").toFile(), config.modelMappings(), Charset.defaultCharset());

            Reader mappingsData = new InputStreamReader(new FileInputStream(BASE_PATH.resolve("model_mappings.json").toFile()));
            ModelEngine.loadMappings(mappingsData, MODEL_PATH);

            ZipUtil.pack(BASE_PATH.resolve("resourcepack").toFile(), ZIP_PATH.toFile());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ModelEngine getModelEngine() {
        return engine;
    }
}
