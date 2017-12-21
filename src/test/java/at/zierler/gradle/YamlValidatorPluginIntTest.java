package at.zierler.gradle;

import org.gradle.testkit.runner.GradleRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static at.zierler.gradle.YamlValidatorPlugin.TASK_NAME;
import static org.gradle.util.GFileUtils.writeFile;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;

public class YamlValidatorPluginIntTest {

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder();

    private File buildFile;
    private File yamlDirectory;
    private File yamlFile;

    @Before
    public void setupTestProject() throws IOException {

        this.buildFile = testProjectDir.newFile("build.gradle");
        String yamlDirectoryRelativePath = ValidationProperties.DEFAULT_DIRECTORY;
        this.yamlDirectory = testProjectDir.newFolder(yamlDirectoryRelativePath.split("/"));
        this.yamlFile = testProjectDir.newFile(yamlDirectoryRelativePath + "file.yaml");
    }

    @Test
    public void shouldUseDefaultSearchPathsWhenNotOverridden() throws IOException {

        writeDefaultBuildFileWithoutProperties();

        String expectedLineInOutput = String.format(YamlValidatorTask.STARTING_DIRECTORY_MESSAGE, yamlDirectory.toPath().toRealPath());

        expectBuildSuccessAndOutput(expectedLineInOutput);
    }

    @Test
    public void shouldUseDefinedSearchPathWhenOverridden() throws IOException {

        String overriddenYamlDirectoryPath = "src/test/resources/";
        File overridenYamlDirectory = testProjectDir.newFolder(overriddenYamlDirectoryPath.split("/"));
        writeFile("plugins { id 'at.zierler.yamlvalidator' }\n" +
                "yamlValidator { searchPaths = ['" + overriddenYamlDirectoryPath + "'] }", buildFile);

        String expectedLineInOutput = String.format(YamlValidatorTask.STARTING_DIRECTORY_MESSAGE, overridenYamlDirectory.toPath().toRealPath());

        expectBuildSuccessAndOutput(expectedLineInOutput);
    }

    @Test
    public void shouldAllowEmptyYaml() throws IOException {

        writeDefaultBuildFileWithoutProperties();

        String expectedLineInOutput = String.format(YamlValidatorTask.SUCCESS_MESSAGE, yamlFile.toPath().toRealPath());

        expectBuildSuccessAndOutput(expectedLineInOutput);
    }


    @Test
    public void shouldNotAllowYamlWithDuplicateKeyWhenDuplicationIsEnabled() throws IOException {

        writeDuplicateKeyYaml();
        writeFile("plugins { id 'at.zierler.yamlvalidator' }\n" +
                "yamlValidator {\n" +
                "\tallowDuplicates = false\n" +
                "}", buildFile);

        String expectedLineInOutput = String.format(YamlValidatorTask.FAILURE_MESSAGE, yamlFile.toPath().toRealPath());

        expectBuildFailureAndOutput(expectedLineInOutput);
    }

    @Test
    public void shouldAllowYamlWithDuplicateKeyWhenDuplicationIsDisabled() throws IOException {

        writeDuplicateKeyYaml();
        writeFile("plugins { id 'at.zierler.yamlvalidator' }\n" +
                "yamlValidator {\n" +
                "\tallowDuplicates = true\n" +
                "}", buildFile);

        String expectedLineInOutput = String.format(YamlValidatorTask.SUCCESS_MESSAGE, yamlFile.toPath().toRealPath());

        expectBuildSuccessAndOutput(expectedLineInOutput);
    }

    @Test
    public void shouldAllowValidYaml() throws IOException {

        writeDefaultBuildFileWithoutProperties();
        writeFile("framework:\n  key: value\n  other: value\n\nother:\n  other: value\n  key: value", yamlFile);

        String expectedLineInOutput = String.format(YamlValidatorTask.SUCCESS_MESSAGE, yamlFile.toPath().toRealPath());

        expectBuildSuccessAndOutput(expectedLineInOutput);
    }

    @Test
    public void shouldSearchInMultipleFoldersWhenDefined() throws IOException {

        String yamlDirectory1Path = "src/any/resources/";
        String yamlDirectory2Path = "src/other/resources/";
        File yamlDirectory1 = testProjectDir.newFolder(yamlDirectory1Path.split("/"));
        File yamlDirectory2 = testProjectDir.newFolder(yamlDirectory2Path.split("/"));
        writeFile("plugins { id 'at.zierler.yamlvalidator' }\n" +
                "yamlValidator { searchPaths = ['" + yamlDirectory1Path + "','" + yamlDirectory2Path + "'] }", buildFile);

        String expectedLineInOutput1 = String.format(YamlValidatorTask.STARTING_DIRECTORY_MESSAGE, yamlDirectory1.toPath().toRealPath());
        String expectedLineInOutput2 = String.format(YamlValidatorTask.STARTING_DIRECTORY_MESSAGE, yamlDirectory2.toPath().toRealPath());

        String output = runBuildAndGetOutput();

        assertThat(output, containsString(expectedLineInOutput1));
        assertThat(output, containsString(expectedLineInOutput2));
    }

    @Test
    public void shouldSearchInMultipleFoldersRecursivelyWhenDefined() throws IOException {

        String yamlDirectory1Path = "src/any/resources/";
        String yamlDirectory2Path = "src/other/resources/";
        File yamlDirectory1 = testProjectDir.newFolder(yamlDirectory1Path.split("/"));
        File yamlDirectory2 = testProjectDir.newFolder(yamlDirectory2Path.split("/"));
        writeFile("plugins { id 'at.zierler.yamlvalidator' }\n" +
                "yamlValidator {\n" +
                "searchPaths = ['" + yamlDirectory1Path + "','" + yamlDirectory2Path + "']\n" +
                "searchRecursive = true\n" +
                "}", buildFile);

        String expectedLineInOutput1 = String.format(YamlValidatorTask.STARTING_DIRECTORY_RECURSIVE_MESSAGE, yamlDirectory1.toPath().toRealPath());
        String expectedLineInOutput2 = String.format(YamlValidatorTask.STARTING_DIRECTORY_RECURSIVE_MESSAGE, yamlDirectory2.toPath().toRealPath());

        String output = runBuildAndGetOutput();

        assertThat(output, containsString(expectedLineInOutput1));
        assertThat(output, containsString(expectedLineInOutput2));
    }

    @Test
    public void shouldBeAbleToFindYamlsInFolderAsWellAsYamlsDefined() throws IOException {

        String yamlDirectory = "src/any/resources/";
        String yamlFileDirectory = "dir/";
        testProjectDir.newFolder(yamlDirectory.split("/"));
        testProjectDir.newFolder(yamlFileDirectory.split("/"));
        File yamlFile1 = testProjectDir.newFile(yamlDirectory + "file.yaml");
        File yamlFile2 = testProjectDir.newFile(yamlFileDirectory + "otherfile.yml");
        writeFile("plugins { id 'at.zierler.yamlvalidator' }\n" +
                "yamlValidator { searchPaths = ['" + yamlDirectory + "','" + yamlFile2 + "'] }", buildFile);

        String expectedLineInOutput1 = String.format(YamlValidatorTask.SUCCESS_MESSAGE, yamlFile1.toPath().toRealPath());
        String expectedLineInOutput2 = String.format(YamlValidatorTask.SUCCESS_MESSAGE, yamlFile2.toPath().toRealPath());

        String output = runBuildAndGetOutput();

        assertThat(output, containsString(expectedLineInOutput1));
        assertThat(output, containsString(expectedLineInOutput2));
    }

    @Test
    public void shouldBeAbleToFindYamlsInFoldersRecursivelyWhenActivated() throws IOException {

        String firstLevelDir = "first/";
        String secondLevelDir = "first/second/";
        testProjectDir.newFolder(firstLevelDir.split("/"));
        testProjectDir.newFolder(secondLevelDir.split("/"));
        File yamlFile1 = testProjectDir.newFile(firstLevelDir + "file.yaml");
        File yamlFile2 = testProjectDir.newFile(secondLevelDir + "otherfile.yml");
        writeFile("plugins { id 'at.zierler.yamlvalidator' }\n" +
                "yamlValidator {\n" +
                "\tsearchPaths = ['" + firstLevelDir + "']\n" +
                "\tsearchRecursive = true\n" +
                "}", buildFile);

        String expectedLineInOutput1 = String.format(YamlValidatorTask.SUCCESS_MESSAGE, yamlFile1.toPath().toRealPath());
        String expectedLineInOutput2 = String.format(YamlValidatorTask.SUCCESS_MESSAGE, yamlFile2.toPath().toRealPath());

        String output = runBuildAndGetOutput();

        assertThat(output, containsString(expectedLineInOutput1));
        assertThat(output, containsString(expectedLineInOutput2));
    }

    @Test
    public void shouldNotBeAbleToFindYamlsInFoldersRecursivelyWhenActivated() throws IOException {

        String firstLevelDir = "first/";
        String secondLevelDir = "first/second/";
        testProjectDir.newFolder(firstLevelDir.split("/"));
        testProjectDir.newFolder(secondLevelDir.split("/"));
        File yamlFile1 = testProjectDir.newFile(firstLevelDir + "file.yaml");
        File yamlFile2 = testProjectDir.newFile(secondLevelDir + "otherfile.yml");
        writeFile("plugins { id 'at.zierler.yamlvalidator' }\n" +
                "yamlValidator {\n" +
                "\tsearchPaths = ['" + firstLevelDir + "']\n" +
                "\tsearchRecursive = false\n" +
                "}", buildFile);

        String expectedLineInOutput = String.format(YamlValidatorTask.SUCCESS_MESSAGE, yamlFile1.toPath().toRealPath());
        String unexpectedLineInOutput = String.format(YamlValidatorTask.SUCCESS_MESSAGE, yamlFile2.toPath().toRealPath());

        expectBuildSuccessAndOutputButNotOtherOutput(expectedLineInOutput, unexpectedLineInOutput);
    }

    @Test
    public void shouldNotValidateFileWithNonYamlEnding() throws IOException {

        String directory = "src/test/resources/";
        testProjectDir.newFolder(directory.split("/"));
        String anyTxtFilePath = directory + "file.txt";
        File anyTxtFile = testProjectDir.newFile(anyTxtFilePath);
        writeFile("plugins { id 'at.zierler.yamlvalidator' }\n" +
                "yamlValidator { searchPaths = ['" + anyTxtFile + "'] }", buildFile);

        String unexpectedLineInOutput = String.format(YamlValidatorTask.STARTING_FILE_MESSAGE, anyTxtFile.toPath().toRealPath());

        String output = runBuildAndGetOutput();

        assertThat(output, not(containsString(unexpectedLineInOutput)));
    }

    @Test
    public void shouldNotValidateFileWithNonYamlEndingButValidateYamlFileInSameDirectory() throws IOException {

        String directory = "src/test/resources/";
        testProjectDir.newFolder(directory.split("/"));
        String anyTxtFilePath = directory + "file.jpg";
        File anyTxtFile = testProjectDir.newFile(anyTxtFilePath);
        String anyYamlFilePath = directory + "application.yml";
        File anyYamlFile = testProjectDir.newFile(anyYamlFilePath);
        writeFile("plugins { id 'at.zierler.yamlvalidator' }\n" +
                "yamlValidator { searchPaths = ['" + directory + "'] }", buildFile);

        String expectedLineInOutput = String.format(YamlValidatorTask.STARTING_FILE_MESSAGE, anyYamlFile.toPath().toRealPath());
        String unexpectedLineInOutput = String.format(YamlValidatorTask.STARTING_FILE_MESSAGE, anyTxtFile.toPath().toRealPath());

        expectBuildSuccessAndOutputButNotOtherOutput(expectedLineInOutput, unexpectedLineInOutput);
    }

    @Test
    public void shouldNotValidateNonYamlFileRecursivelyWhenActivated() throws IOException {

        String firstLevelDir = "first/";
        String secondLevelDir = "first/second/";
        testProjectDir.newFolder(firstLevelDir.split("/"));
        testProjectDir.newFolder(secondLevelDir.split("/"));
        File yamlFile = testProjectDir.newFile(firstLevelDir + "file.yaml");
        File nonYamlFile = testProjectDir.newFile(secondLevelDir + "otherfile.gradle");
        writeFile("plugins { id 'at.zierler.yamlvalidator' }\n" +
                "yamlValidator {\n" +
                "\tsearchPaths = ['" + firstLevelDir + "']\n" +
                "\tsearchRecursive = true\n" +
                "}", buildFile);

        String expectedLineInOutput = String.format(YamlValidatorTask.STARTING_FILE_MESSAGE, yamlFile.toPath().toRealPath());
        String unexpectedLineInOutput = String.format(YamlValidatorTask.STARTING_FILE_MESSAGE, nonYamlFile.toPath().toRealPath());

        expectBuildSuccessAndOutputButNotOtherOutput(expectedLineInOutput, unexpectedLineInOutput);
    }

    private void writeDefaultBuildFileWithoutProperties() {

        writeFile("plugins { id 'at.zierler.yamlvalidator' }", buildFile);
    }

    private void writeDuplicateKeyYaml() {

        writeFile("framework:\n  key: value\n\nframework:\n  other: value", yamlFile);
    }

    private void expectBuildSuccessAndOutput(String expectedLineInOutput) {

        String output = runBuildAndGetOutput();

        assertThat(output, containsString(expectedLineInOutput));
    }

    private void expectBuildFailureAndOutput(String expectedLineInOutput) {

        String output = runBuildExpectedToFailAndGetOutput();

        assertThat(output, containsString(expectedLineInOutput));
    }


    private void expectBuildSuccessAndOutputButNotOtherOutput(String expectedLineInOutput, String unexpectedLineInOutput) {

        String output = runBuildAndGetOutput();

        assertThat(output, containsString(expectedLineInOutput));
        assertThat(output, not(containsString(unexpectedLineInOutput)));
    }

    private String runBuildAndGetOutput() {

        return createGradleRunner().build().getOutput();
    }


    private String runBuildExpectedToFailAndGetOutput() {

        return createGradleRunner().buildAndFail().getOutput();
    }

    private GradleRunner createGradleRunner() {

        return GradleRunner
                .create()
                .withProjectDir(testProjectDir.getRoot())
                .withPluginClasspath()
                .withArguments(TASK_NAME);
    }

}
