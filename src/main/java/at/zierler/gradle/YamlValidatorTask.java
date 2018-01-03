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

    static final String STARTING_DIRECTORY_MESSAGE = "Starting validation of YAML files in directory '%s'.";
    static final String STARTING_DIRECTORY_RECURSIVE_MESSAGE = "Starting validation of YAML files in directory '%s' recursively.";
    static final String STARTING_FILE_MESSAGE = "Starting validation of YAML file '%s'.";
    static final String FILE_SUCCESS_MESSAGE = "Validation of YAML file '%s' successful.";
    static final String FILE_FAILURE_MESSAGE = "Validation of YAML file '%s' failed.";

    private final ValidationProperties validationProperties;

    public YamlValidatorTask() {
        this.validationProperties = getProject().getExtensions().findByType(ValidationProperties.class);
    }

    @TaskAction
    public void validateAllProvidedFilesAndDirectories() throws IOException {

        for (String path : validationProperties.getSearchPaths()) {
            Path fileOrDirectory = resolveFileOrDirectoryByPath(path);
            checkFileOrDirectory(fileOrDirectory);
        }
    }

    private Path resolveFileOrDirectoryByPath(String path) throws IOException {

        return getProject().file(path).toPath().toAbsolutePath().toRealPath();
    }

    private void checkFileOrDirectory(Path fileOrDirectory) throws IOException {

        if (Files.isDirectory(fileOrDirectory)) {
            validateDirectory(fileOrDirectory);
        } else if (Files.isRegularFile(fileOrDirectory)) {
            validateSingleFile(fileOrDirectory);
        } else {
            throw new IOException(String.format("File at path %s is neither a file nor a directory.", fileOrDirectory));
        }
    }

    private void validateSingleFile(Path file) {

        if (isYamlFile(file)) {
            validateYamlFile(file);
        }
    }

    private void validateDirectory(Path directory) throws IOException {

        boolean shouldSearchForYamlFilesRecursively = validationProperties.isSearchRecursive();

        if (shouldSearchForYamlFilesRecursively) {
            validateYamlFilesInDirectoryRecursively(directory);
        } else {
            validateYamlFilesOnlyDirectlyInDirectory(directory);
        }
    }

    private void validateYamlFilesOnlyDirectlyInDirectory(Path directory) throws IOException {

        System.out.println(String.format(STARTING_DIRECTORY_MESSAGE, directory));
        Files.list(directory).filter(this::isYamlFile).forEach(this::validateYamlFile);
    }

    private void validateYamlFilesInDirectoryRecursively(Path directory) throws IOException {

        System.out.println(String.format(STARTING_DIRECTORY_RECURSIVE_MESSAGE, directory));
        Files.walk(directory).filter(this::isYamlFile).forEach(this::validateYamlFile);
    }

    private boolean isYamlFile(Path file) {

        Path fileNameAsPath = file.getFileName();

        if (fileNameAsPath == null) {
            throw new IllegalStateException(String.format("Couldn't extract file name from %s.", file));
        }

        String fileName = fileNameAsPath.toString();

        return fileName.endsWith(".yaml") || fileName.endsWith(".yml");
    }

    private void validateYamlFile(Path file) {

        System.out.println(String.format(STARTING_FILE_MESSAGE, file));

        try (Reader yamlFileReader = Files.newBufferedReader(file)) {
            YamlReader yamlReader = setUpYamlReader(yamlFileReader);

            yamlReader.read();
        } catch (IOException e) {
            throw new GradleException(String.format(FILE_FAILURE_MESSAGE, file), e);
        }

        System.out.println(String.format(FILE_SUCCESS_MESSAGE, file));
    }

    private YamlReader setUpYamlReader(Reader yamlFileReader) {

        YamlConfig yamlConfig = setUpYamlConfig();
        return new YamlReader(yamlFileReader, yamlConfig);
    }

    private YamlConfig setUpYamlConfig() {

        YamlConfig yamlConfig = new YamlConfig();

        boolean allowDuplicates = validationProperties.isAllowDuplicates();
        yamlConfig.setAllowDuplicates(allowDuplicates);

        return yamlConfig;
    }

}
