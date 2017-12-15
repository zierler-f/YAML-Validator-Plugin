package at.zierler.gradle.internal;

import at.zierler.gradle.YamlValidatorExtension;
import at.zierler.gradle.YamlValidatorTask;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class YamlValidatorTaskImpl extends YamlValidatorTask {

    @Override
    @TaskAction
    public void validateYaml() throws IOException {

        YamlValidatorExtension extension = getProject().getExtensions().findByType(YamlValidatorExtension.class);
        if (extension == null) {
            extension = new YamlValidatorExtension();
        }

        String yamlDirectoryPath = extension.getDirectory();
        System.out.printf("Starting to validate yaml files in %s.", yamlDirectoryPath);
        System.out.println();
        Path yamlDirectory = getProject().file(yamlDirectoryPath).toPath();
        Files.list(yamlDirectory).filter(this::isYamlFile).forEach(this::validateFile);
    }

    private boolean isYamlFile(Path file) {

        return file.toString().endsWith(".yaml") || file.toString().endsWith(".yaml");
    }

    private void validateFile(Path file) {

        System.out.println(file.toAbsolutePath().toString() + " is valid.");
    }

}
