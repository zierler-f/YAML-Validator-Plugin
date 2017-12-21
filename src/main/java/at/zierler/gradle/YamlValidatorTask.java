package at.zierler.gradle;

import com.esotericsoftware.yamlbeans.YamlConfig;
import com.esotericsoftware.yamlbeans.YamlReader;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

public class YamlValidatorTask extends DefaultTask {

    private final ValidationProperties validationProperties;

    public YamlValidatorTask() {
        this.validationProperties = getProject().getExtensions().findByType(ValidationProperties.class);
    }

    @TaskAction
    public void validateAllProvidedFilesAndDirectories() throws IOException {

        for (String path : validationProperties.getSearchPaths()) {
            System.out.println(String.format("Starting to validate yaml files in %s.", path));
            Path fileOrDirectory = getProject().file(path).toPath();
            if (Files.isDirectory(fileOrDirectory)) {
                Path directory = fileOrDirectory;
                boolean shouldSearchForYamlFilesRecursively = validationProperties.isSearchRecursive();
                if (shouldSearchForYamlFilesRecursively) {
                    Files.walk(directory).filter(this::isYamlFile).forEach(this::validateFile);
                } else {
                    Files.list(directory).filter(this::isYamlFile).forEach(this::validateFile);
                }
            } else if (Files.isRegularFile(fileOrDirectory)) {
                Path file = fileOrDirectory;
                if (isYamlFile(file)) {
                    Path yamlFile = file;
                    validateFile(fileOrDirectory);
                }
            } else {
                throw new IllegalStateException(String.format("File at path %s is neither a file nor a directory.", path));
            }
        }
    }

    private boolean isYamlFile(Path file) {

        String fileName = file.toString();
        return fileName.endsWith(".yaml") || fileName.endsWith(".yml");
    }

    private void validateFile(Path file) {

        String absolutePath = file.toAbsolutePath().toString();

        System.out.println("Validating " + absolutePath);

        try (Reader yamlFileReader = Files.newBufferedReader(file)) {
            YamlReader yamlReader = new YamlReader(yamlFileReader);

            YamlConfig yamlReaderConfig = yamlReader.getConfig();
            setConfigValues(yamlReaderConfig);

            yamlReader.read();
        } catch (IOException e) {
            throw new GradleException(absolutePath + " is not valid.", e);
        }

        System.out.println(absolutePath + " is valid.");
    }

    private void setConfigValues(YamlConfig yamlReaderConfig) {

        yamlReaderConfig.setAllowDuplicates(validationProperties.isAllowDuplicates());
    }

}
