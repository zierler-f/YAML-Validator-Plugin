package at.zierler.gradle;

import org.gradle.testkit.runner.GradleRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static at.zierler.gradle.YamlValidatorPlugin.VALIDATE_YAML_TASK_NAME;
import static org.gradle.util.GFileUtils.writeFile;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;

public class YamlValidatorPluginIntTest {

    private static final String DEFAULT_YAML_DIRECTORY_RELATIVE_PATH = ValidationProperties.DEFAULT_DIRECTORY;
    private static final String ANY_YAML_DIRECTORY_RELATIVE_PATH = "src/test/resources/";

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder();

    private File buildFile;

    private File defaultYamlDirectory;
    private File anyYamlDirectory;

    private File yamlFileInDefaultYamlDirectory;
    private File yamlFileInAnyYamlDirectory;

    @Before
    public void setupTestProject() throws IOException {

        this.buildFile = testProjectDir.newFile("build.gradle");

        this.defaultYamlDirectory = testProjectDir.newFolder(DEFAULT_YAML_DIRECTORY_RELATIVE_PATH.split("/"));
        this.anyYamlDirectory = testProjectDir.newFolder(ANY_YAML_DIRECTORY_RELATIVE_PATH.split("/"));

        this.yamlFileInDefaultYamlDirectory = testProjectDir.newFile(DEFAULT_YAML_DIRECTORY_RELATIVE_PATH + "file.yaml");
        this.yamlFileInAnyYamlDirectory = testProjectDir.newFile(ANY_YAML_DIRECTORY_RELATIVE_PATH + "file.yml");
    }

    @Test
    public void shouldUseDefaultSearchPathsWhenNonDefined() throws IOException {

        writeBuildFileWithoutProperties();

        expectBuildSuccessAndDirectorySearchStartMessage(defaultYamlDirectory);
    }

    @Test
    public void shouldUseDefinedSearchPaths() throws IOException {

        writeBuildFileWithAnyYamlDirectoryAsOnlyDefinedSearchPath();

        expectBuildSuccessAndDirectorySearchStartMessage(anyYamlDirectory);
    }

    @Test
    public void shouldSucceedForEmptyYaml() throws IOException {

        writeBuildFileWithoutProperties();

        expectBuildSuccessAndSuccessMessageForDefaultYamlFile();
    }

    @Test
    public void shouldNotAllowYamlWithDuplicateKeyWhenAllowDuplicatesIsFalse() throws IOException {

        writeBuildFileWhichDoesNotAllowDuplicateKeys();
        writeYamlFileWithDuplicateKey();

        expectBuildFailureAndFailureMessageForDefaultYamlFile();
    }

    @Test
    public void shouldAllowYamlWithDuplicateKeyWhenAllowDuplicatesIsTrue() throws IOException {

        writeBuildFileWhichDoesAllowDuplicateKeys();
        writeYamlFileWithDuplicateKey();

        expectBuildSuccessAndSuccessMessageForDefaultYamlFile();
    }

    @Test
    public void shouldAllowValidYaml() throws IOException {

        writeBuildFileWithoutProperties();
        writeValidYamlFile();

        expectBuildSuccessAndSuccessMessageForDefaultYamlFile();
    }
    
    @Test
    public void shouldAllowValidYamlWithMultipleDocuments() throws IOException {

        writeBuildFileWithoutProperties();
        writeValidYamlFileWithMultipleDocuments();

        expectBuildSuccessAndSuccessMessageForYamlWithMulipleDocuments();
    }
    
    @Test
    public void shouldNotAllowYamlWithMultipleDocumentsWithErrorInFirstDocument() throws IOException {

    	writeBuildFileWithoutProperties();
    	writeInvalidValidYamlFileWithMultipleDocumentsWithErrorInFirstDocument();

        expectBuildFailureAndFailureMessageForDefaultYamlFile();
    }
    
    @Test
    public void shouldNotAllowYamlWithMultipleDocumentsWithErrorInSecondDocument() throws IOException {

    	writeBuildFileWithoutProperties();
    	writeInvalidValidYamlFileWithMultipleDocumentsWithErrorInSecondDocument();

    	expectBuildFailureAndFailureMessageForYamlWithMulipleDocumentsWithErrorInSecondDocument();
    }
    
    @Test
    public void shouldNotAllowYamlWithMultipleDocumentsWithErrorInLastDocument() throws IOException {

    	writeBuildFileWithoutProperties();
    	writeInvalidValidYamlFileWithMultipleDocumentsWithErrorInLastDocument();

    	expectBuildFailureAndFailureMessageForYamlWithMulipleDocumentsWithErrorInLastDocument();
    }

    @Test
    public void shouldSearchInMultipleFoldersWhenDefined() throws IOException {

        writeBuildFileWhichDefinesTwoDirectories();
        writeValidYamlFileWithMultipleDocuments();

        expectBuildSuccessAndStartingMessageForBothDirectories();
    }

    @Test
    public void shouldSearchInMultipleFoldersRecursivelyWhenDefined() throws IOException {

        writeBuildFileWhichDefinesTwoDirectoriesAndActivatesRecursiveSearch();

        expectBuildSuccessAndRecursiveStartingMessageForBothDirectories();
    }

    @Test
    public void shouldBeAbleToFindYamlsInFolderAsWellAsYamlsDefined() throws IOException {

        writeBuildFileWhichDefinesOneYamlFileAndOneDirectoryContainingAnohterYamlFile();

        expectBuildSuccessAndSuccessMessageForBothFiles();
    }

    @Test
    public void shouldBeAbleToFindYamlsInFoldersRecursivelyWhenActivated() throws IOException {

        File yamlFileInSubdirectoryOfDefaultYamlDirectory = createAndGetYamlFileInSubdirectoryOfDefaultYamlDirectory();
        writeBuildFileWithDefaultYamlDirectoryAndRecursiveSearchActivated();

        expectBuildSuccessAndSuccessMessageForDefaultFileAndFileInSubdirectory(yamlFileInSubdirectoryOfDefaultYamlDirectory);
    }

    @Test
    public void shouldNotBeAbleToFindYamlsInFoldersRecursivelyWhenDeactivated() throws IOException {

        File yamlFileInSubdirectoryOfDefaultYamlDirectory = createAndGetYamlFileInSubdirectoryOfDefaultYamlDirectory();
        writeBuildFileWithDefaultYamlDirectoryAndRecursiveSearchDeactivated();

        expectBuildSuccessAndSuccessMessageForDefaultFileAndNoMessageForFileInSubdirectory(yamlFileInSubdirectoryOfDefaultYamlDirectory);
    }

    @Test
    public void shouldNotValidateFileWithNonYamlEnding() throws IOException {

        File nonYamlFile = createAndGetNonYamlFileInDefaultYamlDirectory();
        writeBuildFileWithNonYamlFileAsSearchPath(nonYamlFile);

        expectBuildSuccessAndNoStartingMessageForNonYamlFile(nonYamlFile);
    }

    @Test
    public void shouldNotValidateFileWithNonYamlEndingButValidateYamlFileInSameDirectory() throws IOException {

        File nonYamlFile = createAndGetNonYamlFileInDefaultYamlDirectory();
        writeBuildFileWithoutProperties();

        expectBuildSuccessAndStartingFileMessageForYamlFileButNoStartingMessageForNonYamlFile(nonYamlFile);
    }

    @Test
    public void shouldNotValidateNonYamlFileRecursivelyWhenActivated() throws IOException {

        File nonYamlFileInSubdirectoryOfDefaultYamlDirectory = createAndGetNonYamlFileInSubdirectoryOfDefaultYamlDirectory();
        writeBuildFileWithDefaultYamlDirectoryAndRecursiveSearchActivated();

        expectBuildSuccessAndStartingFileMessageForYamlFileButNoStartingMessageForNonYamlFileInSubdirectory(nonYamlFileInSubdirectoryOfDefaultYamlDirectory);
    }

    @Test
    public void shouldRunYamlValidationDuringCheckPhase() throws IOException {

        writeFile(
                "plugins { id 'at.zierler.yamlvalidator' }\n" +
                        "apply plugin: 'java'",
                buildFile);

        String expectedLineInOutput = String.format(YamlValidatorTask.STARTING_DIRECTORY_MESSAGE, defaultYamlDirectory.toPath().toRealPath());

        String output = GradleRunner
                .create()
                .withProjectDir(testProjectDir.getRoot())
                .withPluginClasspath()
                .withArguments("check", "--info")
                .build()
                .getOutput();

        assertThat(output, containsString(expectedLineInOutput));
    }

    @Test
    public void shouldNotLogWhenLogLevelIsLifecycle() throws IOException {

        writeFile(
                "plugins { id 'at.zierler.yamlvalidator' }\n" +
                        "apply plugin: 'java'",
                buildFile);

        String startingFileMessage = String.format(YamlValidatorTask.STARTING_FILE_MESSAGE, yamlFileInDefaultYamlDirectory.toPath().toRealPath());
        String startingDirectoryMessage = String.format(YamlValidatorTask.STARTING_DIRECTORY_MESSAGE, defaultYamlDirectory.toPath().toRealPath());
        String fileSuccessMessage = String.format(YamlValidatorTask.FILE_SUCCESS_MESSAGE, yamlFileInDefaultYamlDirectory.toPath().toRealPath());

        String output = GradleRunner
                .create()
                .withProjectDir(testProjectDir.getRoot())
                .withPluginClasspath()
                .build()
                .getOutput();

        assertThat(output, not(containsString(startingDirectoryMessage)));
        assertThat(output, not(containsString(startingFileMessage)));
        assertThat(output, not(containsString(fileSuccessMessage)));
    }

    private void writeBuildFileWithoutProperties() {

        writeFile(
                "plugins { id 'at.zierler.yamlvalidator' }",
                buildFile);
    }

    private void writeBuildFileWithAnyYamlDirectoryAsOnlyDefinedSearchPath() {

        writeFile(
                "plugins { id 'at.zierler.yamlvalidator' }\n" +
                        "yamlValidator { searchPaths = ['" + ANY_YAML_DIRECTORY_RELATIVE_PATH + "'] }",
                buildFile);
    }


    private void writeBuildFileWhichDoesNotAllowDuplicateKeys() {

        writeFile(
                "plugins { id 'at.zierler.yamlvalidator' }\n" +
                        "yamlValidator { allowDuplicates = false }",
                buildFile);
    }

    private void writeBuildFileWhichDoesAllowDuplicateKeys() {

        writeFile(
                "plugins { id 'at.zierler.yamlvalidator' }\n" +
                        "yamlValidator { allowDuplicates = true }",
                buildFile);
    }


    private void writeBuildFileWhichDefinesTwoDirectories() {

        writeFile("plugins { id 'at.zierler.yamlvalidator' }\n" +
                        "yamlValidator { searchPaths = ['" + DEFAULT_YAML_DIRECTORY_RELATIVE_PATH + "','" + ANY_YAML_DIRECTORY_RELATIVE_PATH + "'] }",
                buildFile);
    }

    private void writeBuildFileWhichDefinesTwoDirectoriesAndActivatesRecursiveSearch() {

        writeFile("plugins { id 'at.zierler.yamlvalidator' }\n" +
                        "yamlValidator {\n" +
                        "searchPaths = ['" + DEFAULT_YAML_DIRECTORY_RELATIVE_PATH + "','" + ANY_YAML_DIRECTORY_RELATIVE_PATH + "']\n" +
                        "searchRecursive = true\n" +
                        "}",
                buildFile);
    }

    private void writeBuildFileWhichDefinesOneYamlFileAndOneDirectoryContainingAnohterYamlFile() {

        writeFile("plugins { id 'at.zierler.yamlvalidator' }\n" +
                        "yamlValidator { searchPaths = ['" + defaultYamlDirectory + "','" + yamlFileInAnyYamlDirectory + "'] }",
                buildFile);
    }

    private void writeBuildFileWithDefaultYamlDirectoryAndRecursiveSearchActivated() {

        writeFile("plugins { id 'at.zierler.yamlvalidator' }\n" +
                        "yamlValidator {\n" +
                        "\tsearchPaths = ['" + DEFAULT_YAML_DIRECTORY_RELATIVE_PATH + "']\n" +
                        "\tsearchRecursive = true\n" +
                        "}",
                buildFile);
    }

    private void writeBuildFileWithDefaultYamlDirectoryAndRecursiveSearchDeactivated() {

        writeFile("plugins { id 'at.zierler.yamlvalidator' }\n" +
                        "yamlValidator {\n" +
                        "\tsearchPaths = ['" + DEFAULT_YAML_DIRECTORY_RELATIVE_PATH + "']\n" +
                        "\tsearchRecursive = false\n" +
                        "}",
                buildFile);
    }

    private void writeBuildFileWithNonYamlFileAsSearchPath(File anyTxtFile) {
        writeFile("plugins { id 'at.zierler.yamlvalidator' }\n" +
                        "yamlValidator { searchPaths = ['" + anyTxtFile + "'] }",
                buildFile);
    }

    private void writeYamlFileWithDuplicateKey() {

        writeFile(
                "framework:\n" +
                        "  key: value\n" +
                        "\n" +
                        "framework:\n" +
                        "  other: value",
                yamlFileInDefaultYamlDirectory);
    }


    private void writeValidYamlFile() {

        writeFile(
                "framework:\n" +
                        "  key: value\n" +
                        "  other: value\n" +
                        "\n" +
                        "other:\n" +
                        "  other: value\n" +
                        "  key: value",
                yamlFileInDefaultYamlDirectory);
    }
    
    private void writeValidYamlFileWithMultipleDocuments() {

        writeFile(
                "framework:\n" +
                        "  key: value\n" +
                        "  other: value\n" +
                        "---\n" +
                        "other:\n" +
                        "  other: value\n" +
                        "  key: value",
                yamlFileInDefaultYamlDirectory);
    }
    
    private void writeInvalidValidYamlFileWithMultipleDocumentsWithErrorInFirstDocument() {
    	
        writeFile(
                "framework:\n" +
                        "  key: value\n" +
                        "  other: value\n" +
                        "    other: value\n" +
                        "---\n" +
                        "other:\n" +
                        "  other: value\n" +
                        "  key: value",
                yamlFileInDefaultYamlDirectory);
    }
    
    private void writeInvalidValidYamlFileWithMultipleDocumentsWithErrorInSecondDocument() {
    	
        writeFile(
                "framework:\n" +
                        "  key: value\n" +
                        "  other: value\n" +
                        "---\n" +
                        "other:\n" +
                        "  other: value\n" +
                        "  key: value\n" +
                        "    other: value\n" +
                        "---\n" +
                        "another:\n" +
                        "  key: value\n" +
                        "  other: value\n",
                yamlFileInDefaultYamlDirectory);
    }
    
    private void writeInvalidValidYamlFileWithMultipleDocumentsWithErrorInLastDocument() {
    	
        writeFile(
                "framework:\n" +
                        "  key: value\n" +
                        "  other: value\n" +
                        "---\n" +
                        "other:\n" +
                        "  other: value\n" +
                        "  key: value\n" +
                        "---\n" +
                        "another:\n" +
                        "  key: value\n" +
                        "  other: value\n" +
                        "    other: value\n",
                yamlFileInDefaultYamlDirectory);
    }

    private File createAndGetYamlFileInSubdirectoryOfDefaultYamlDirectory() throws IOException {

        String subdirectoryInDefaultYamlDirectoryRelativePath = DEFAULT_YAML_DIRECTORY_RELATIVE_PATH + "subdir/";
        testProjectDir.newFolder(subdirectoryInDefaultYamlDirectoryRelativePath.split("/"));
        return testProjectDir.newFile(subdirectoryInDefaultYamlDirectoryRelativePath + "file.yaml");
    }

    private File createAndGetNonYamlFileInDefaultYamlDirectory() throws IOException {

        String anyTxtFilePath = DEFAULT_YAML_DIRECTORY_RELATIVE_PATH + "file.txt";
        return testProjectDir.newFile(anyTxtFilePath);
    }

    private File createAndGetNonYamlFileInSubdirectoryOfDefaultYamlDirectory() throws IOException {

        String subdirectoryInDefaultYamlDirectoryRelativePath = DEFAULT_YAML_DIRECTORY_RELATIVE_PATH + "subdir/";
        testProjectDir.newFolder(subdirectoryInDefaultYamlDirectoryRelativePath.split("/"));
        return testProjectDir.newFile(subdirectoryInDefaultYamlDirectoryRelativePath + "file.txt");
    }

    private void expectBuildSuccessAndDirectorySearchStartMessage(File directory) throws IOException {

        String expectedLineInOutput = String.format(YamlValidatorTask.STARTING_DIRECTORY_MESSAGE, directory.toPath().toRealPath());

        expectBuildSuccessAndOutput(expectedLineInOutput);
    }


    private void expectBuildSuccessAndSuccessMessageForDefaultYamlFile() throws IOException {

        expectBuildSuccessAndSuccessMessageForFile(yamlFileInDefaultYamlDirectory);
    }

    private void expectBuildSuccessAndSuccessMessageForFile(File yamlFile) throws IOException {

        String expectedLineInOutput = String.format(YamlValidatorTask.FILE_SUCCESS_MESSAGE, yamlFile.toPath().toRealPath());

        expectBuildSuccessAndOutput(expectedLineInOutput);
    }

    private void expectBuildFailureAndFailureMessageForDefaultYamlFile() throws IOException {

        expectBuildFailureAndFailureMessageForFile(yamlFileInDefaultYamlDirectory);
    }

    private void expectBuildFailureAndFailureMessageForFile(File yamlFile) throws IOException {

        String expectedLineInOutput = String.format(YamlValidatorTask.FILE_FAILURE_MESSAGE, yamlFile.toPath().toRealPath());

        expectBuildFailureAndOutput(expectedLineInOutput);
    }

    private void expectBuildSuccessAndOutput(String expectedLineInOutput) {

        String output = runYamlValidateTaskAndGetOutput();

        assertThat(output, containsString(expectedLineInOutput));
    }

    private void expectBuildFailureAndOutput(String expectedLineInOutput) {

        String output = runYamlValidateTaskExpectedToFailAndGetOutput();

        assertThat(output, containsString(expectedLineInOutput));
    }

    private void expectBuildSuccessAndStartingMessageForBothDirectories() throws IOException {

        String expectedLineInOutput1 = String.format(YamlValidatorTask.STARTING_DIRECTORY_MESSAGE, defaultYamlDirectory.toPath().toRealPath());
        String expectedLineInOutput2 = String.format(YamlValidatorTask.STARTING_DIRECTORY_MESSAGE, anyYamlDirectory.toPath().toRealPath());

        expectBuildSuccessWithLinesInOutput(expectedLineInOutput1, expectedLineInOutput2);
    }

    private void expectBuildSuccessAndRecursiveStartingMessageForBothDirectories() throws IOException {

        String expectedLineInOutput1 = String.format(YamlValidatorTask.STARTING_DIRECTORY_RECURSIVE_MESSAGE, defaultYamlDirectory.toPath().toRealPath());
        String expectedLineInOutput2 = String.format(YamlValidatorTask.STARTING_DIRECTORY_RECURSIVE_MESSAGE, anyYamlDirectory.toPath().toRealPath());

        expectBuildSuccessWithLinesInOutput(expectedLineInOutput1, expectedLineInOutput2);
    }

    private void expectBuildSuccessAndSuccessMessageForBothFiles() throws IOException {

        String expectedLineInOutput1 = String.format(YamlValidatorTask.FILE_SUCCESS_MESSAGE, yamlFileInDefaultYamlDirectory.toPath().toRealPath());
        String expectedLineInOutput2 = String.format(YamlValidatorTask.FILE_SUCCESS_MESSAGE, yamlFileInAnyYamlDirectory.toPath().toRealPath());

        expectBuildSuccessWithLinesInOutput(expectedLineInOutput1, expectedLineInOutput2);
    }
    
    private void expectBuildSuccessAndSuccessMessageForYamlWithMulipleDocuments() throws IOException {

        String expectedLineInOutput1 = String.format(YamlValidatorTask.FILE_SUCCESS_MESSAGE, yamlFileInDefaultYamlDirectory.toPath().toRealPath());
        String expectedLineInOutput2 = String.format(YamlValidatorTask.DOCUMENT_SUCCESS_MESSAGE, 1, yamlFileInDefaultYamlDirectory.toPath().toRealPath());
        String expectedLineInOutput3 = String.format(YamlValidatorTask.DOCUMENT_SUCCESS_MESSAGE, 2, yamlFileInDefaultYamlDirectory.toPath().toRealPath());

        expectBuildSuccessWithLinesInOutput(expectedLineInOutput1, expectedLineInOutput2, expectedLineInOutput3);
    }
    
    private void expectBuildFailureAndFailureMessageForYamlWithMulipleDocumentsWithErrorInSecondDocument() throws IOException {

        String expectedLineInOutput1 = String.format(YamlValidatorTask.FILE_FAILURE_MESSAGE, yamlFileInDefaultYamlDirectory.toPath().toRealPath());
        String expectedLineInOutput2 = String.format(YamlValidatorTask.DOCUMENT_SUCCESS_MESSAGE, 1, yamlFileInDefaultYamlDirectory.toPath().toRealPath());

        expectBuildFailureWithLinesInOutput(expectedLineInOutput1, expectedLineInOutput2);
    }
    
    private void expectBuildFailureAndFailureMessageForYamlWithMulipleDocumentsWithErrorInLastDocument() throws IOException {

        String expectedLineInOutput1 = String.format(YamlValidatorTask.FILE_FAILURE_MESSAGE, yamlFileInDefaultYamlDirectory.toPath().toRealPath());
        String expectedLineInOutput2 = String.format(YamlValidatorTask.DOCUMENT_SUCCESS_MESSAGE, 1, yamlFileInDefaultYamlDirectory.toPath().toRealPath());
        String expectedLineInOutput3 = String.format(YamlValidatorTask.DOCUMENT_SUCCESS_MESSAGE, 2, yamlFileInDefaultYamlDirectory.toPath().toRealPath());

        expectBuildFailureWithLinesInOutput(expectedLineInOutput1, expectedLineInOutput2, expectedLineInOutput3);
    }

    private void expectBuildSuccessAndSuccessMessageForDefaultFileAndFileInSubdirectory(File yamlFileInSubdirectoryInDefaultYamlDirectory) throws IOException {

        String expectedLineInOutput1 = String.format(YamlValidatorTask.FILE_SUCCESS_MESSAGE, yamlFileInDefaultYamlDirectory.toPath().toRealPath());
        String expectedLineInOutput2 = String.format(YamlValidatorTask.FILE_SUCCESS_MESSAGE, yamlFileInSubdirectoryInDefaultYamlDirectory.toPath().toRealPath());

        expectBuildSuccessWithLinesInOutput(expectedLineInOutput1, expectedLineInOutput2);
    }

    private void expectBuildSuccessAndSuccessMessageForDefaultFileAndNoMessageForFileInSubdirectory(File yamlFileInSubdirectoryOfDefaultYamlDirectory) throws IOException {

        String expectedLineInOutput = String.format(YamlValidatorTask.FILE_SUCCESS_MESSAGE, yamlFileInDefaultYamlDirectory.toPath().toRealPath());
        String unexpectedLineInOutput = String.format(YamlValidatorTask.FILE_SUCCESS_MESSAGE, yamlFileInSubdirectoryOfDefaultYamlDirectory.toPath().toRealPath());

        expectBuildSuccessAndOutputButNotOtherOutput(expectedLineInOutput, unexpectedLineInOutput);
    }

    private void expectBuildSuccessAndNoStartingMessageForNonYamlFile(File nonYamlFile) throws IOException {

        String unexpectedLineInOutput = String.format(YamlValidatorTask.STARTING_FILE_MESSAGE, nonYamlFile.toPath().toRealPath());

        String output = runYamlValidateTaskAndGetOutput();

        assertThat(output, not(containsString(unexpectedLineInOutput)));
    }

    private void expectBuildSuccessAndStartingFileMessageForYamlFileButNoStartingMessageForNonYamlFile(File nonYamlFile) throws IOException {

        String expectedLineInOutput = String.format(YamlValidatorTask.STARTING_FILE_MESSAGE, yamlFileInDefaultYamlDirectory.toPath().toRealPath());
        String unexpectedLineInOutput = String.format(YamlValidatorTask.STARTING_FILE_MESSAGE, nonYamlFile.toPath().toRealPath());

        expectBuildSuccessAndOutputButNotOtherOutput(expectedLineInOutput, unexpectedLineInOutput);
    }

    private void expectBuildSuccessAndStartingFileMessageForYamlFileButNoStartingMessageForNonYamlFileInSubdirectory(File nonYamlFileInSubdirectoryOfDefaultYamlDirectory) throws IOException {

        String expectedLineInOutput = String.format(YamlValidatorTask.STARTING_FILE_MESSAGE, yamlFileInDefaultYamlDirectory.toPath().toRealPath());
        String unexpectedLineInOutput = String.format(YamlValidatorTask.STARTING_FILE_MESSAGE, nonYamlFileInSubdirectoryOfDefaultYamlDirectory.toPath().toRealPath());

        expectBuildSuccessAndOutputButNotOtherOutput(expectedLineInOutput, unexpectedLineInOutput);
    }

    private void expectBuildSuccessWithLinesInOutput(String... expectedLinesInOutput) {

        String output = runYamlValidateTaskAndGetOutput();

        expectLinesInOutput(output, expectedLinesInOutput);
    }
    
    private void expectBuildFailureWithLinesInOutput(String... expectedLinesInOutput) {

        String output = runYamlValidateTaskExpectedToFailAndGetOutput();

        expectLinesInOutput(output, expectedLinesInOutput);
    }

    private void expectLinesInOutput(String output, String[] expectedLinesInOutput) {

        Arrays.stream(expectedLinesInOutput)
                .forEach(expectedLineInOutput -> assertThat(output, containsString(expectedLineInOutput)));
    }

    private void expectBuildSuccessAndOutputButNotOtherOutput(String expectedLineInOutput, String unexpectedLineInOutput) {

        String output = runYamlValidateTaskAndGetOutput();

        assertThat(output, containsString(expectedLineInOutput));
        assertThat(output, not(containsString(unexpectedLineInOutput)));
    }

    private String runYamlValidateTaskAndGetOutput() {

        return createGradleRunnerForYamlValidateTask().build().getOutput();
    }


    private String runYamlValidateTaskExpectedToFailAndGetOutput() {

        return createGradleRunnerForYamlValidateTask().buildAndFail().getOutput();
    }

    private GradleRunner createGradleRunnerForYamlValidateTask() {

        return GradleRunner
                .create()
                .withProjectDir(testProjectDir.getRoot())
                .withPluginClasspath()
                .withArguments(VALIDATE_YAML_TASK_NAME, "--info");
    }

}
