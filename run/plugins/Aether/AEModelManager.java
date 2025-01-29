public class AEModelManager {
    private static final Path BASE_PATH = Aether.getInstance().getDataPath();
    private static final Path ZIP_PATH = BASE_PATH.resolve("resourcepack.zip");
    private static final Path MODEL_PATH = BASE_PATH.resolve("models");

    public AEModelManager() {
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
            // Copy the template resourcepack files
            FileUtils.copyDirectory(BASE_PATH.resolve("resourcepack_template").toFile(), BASE_PATH.resolve("resourcepack").toFile());
            
            // Generate resourcepack and models
            var config = PackBuilder.Generate(BASE_PATH.resolve("bbmodel"), BASE_PATH.resolve("resourcepack"), MODEL_PATH);
            
            // Write model mappings to file
            FileUtils.writeStringToFile(BASE_PATH.resolve("model_mappings.json").toFile(), config.modelMappings(), Charset.defaultCharset());
            
            // Flatten model files structure
            flattenModelDirectory();

            // Load mappings and pack the resourcepack
            Reader mappingsData = new InputStreamReader(new FileInputStream(BASE_PATH.resolve("model_mappings.json").toFile()));
            ModelEngine.loadMappings(mappingsData, MODEL_PATH);

            ZipUtil.pack(BASE_PATH.resolve("resourcepack").toFile(), ZIP_PATH.toFile());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Flattens the model directory structure by moving files from "<model name>.bbmodel/"
     * directly into the "models/" directory and removing the unnecessary "<model name>.bbmodel/" folder.
     */
    private void flattenModelDirectory() throws IOException {
        File modelDirectory = MODEL_PATH.toFile();
        File[] modelSubDirectories = modelDirectory.listFiles(File::isDirectory);
        
        if (modelSubDirectories != null) {
            for (File subDir : modelSubDirectories) {
                // Move all files from the sub-directory to the main model directory
                File[] files = subDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        FileUtils.moveFileToDirectory(file, modelDirectory, false);
                    }
                }
                // Delete the now-empty sub-directory
                FileUtils.deleteDirectory(subDir);
            }
        }
    }
}