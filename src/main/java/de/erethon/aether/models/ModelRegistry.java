package de.erethon.aether.models;

import de.erethon.bedrock.chat.MessageUtil;
import team.unnamed.hephaestus.Model;
import team.unnamed.hephaestus.reader.blockbench.BBModelReader;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ModelRegistry {

    private final Map<String, Model> models = new HashMap<>();

    public void registerModel(Model model) {
        models.put(model.name(), model);
    }

    public @Nullable Model model(String name) {
        return models.get(name);
    }

    public Collection<String> modelNames() {
        return models.keySet();
    }

    public Collection<Model> models() {
        return models.values();
    }

    public void loadFromFolder(File folder) {
        models.clear();
        for (File file : folder.listFiles()) {
            if (file.getName().endsWith(".bbmodel")) {
                try {
                    Model model = BBModelReader.blockbench().read(file);
                    registerModel(model);
                }
                catch (Exception e) {
                    MessageUtil.log("Failed to load model from file: " + file.getName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

}