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
    public void validateYaml() throws IOException {

        String yamlDirectoryPath = validationProperties.getSearchPaths().get(0);
        System.out.printf("Starting to validate yaml files in %s.", yamlDirectoryPath);
        System.out.println();
        Path yamlDirectory = getProject().file(yamlDirectoryPath).toPath();
        Files.list(yamlDirectory).filter(this::isYamlFile).forEach(this::validateFile);
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
