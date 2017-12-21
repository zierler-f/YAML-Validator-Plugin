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

public class YamlValidatorPluginIntTest {

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder();

    private File buildFile;
    private String yamlDirectory = ValidationProperties.DEFAULT_DIRECTORY;
    private File yamlFile;

    @Before
    public void setupTestProject() throws IOException {

        this.buildFile = testProjectDir.newFile("build.gradle");
        testProjectDir.newFolder(yamlDirectory.split("/"));
        this.yamlFile = testProjectDir.newFile(yamlDirectory + "file.yaml");
    }

    @Test
    public void shouldUseDefaultSearchPathsWhenNotOverridden() {

        writeDefaultBuildFileWithoutProperties();

        String expectedLineInOutput = "Starting to validate yaml files in " + yamlDirectory + ".";

        expectBuildSuccessAndOutput(expectedLineInOutput);
    }

    @Test
    public void shouldUseDefinedSearchPathWhenOverridden() throws IOException {

        String overriddenYamlDirectory = "src/test/resources/";
        testProjectDir.newFolder(overriddenYamlDirectory.split("/"));
        writeFile("plugins { id 'at.zierler.yamlvalidator' }\n" +
                "yamlValidator { searchPaths = ['" + overriddenYamlDirectory + "'] }", buildFile);

        String expectedLineInOutput = "Starting to validate yaml files in " + overriddenYamlDirectory + ".";

        expectBuildSuccessAndOutput(expectedLineInOutput);
    }

    @Test
    public void shouldAllowEmptyYaml() {

        writeDefaultBuildFileWithoutProperties();

        String expectedLineInOutput = yamlFile.getAbsolutePath() + " is valid.";

        expectBuildSuccessAndOutput(expectedLineInOutput);
    }


    @Test
    public void shouldNotAllowYamlWithDuplicateKeyWhenDuplicationIsEnabled() {

        writeDuplicateKeyYaml();
        writeFile("plugins { id 'at.zierler.yamlvalidator' }\n" +
                "yamlValidator {\n" +
                "\tallowDuplicates = false\n" +
                "}", buildFile);

        String expectedLineInOutput = yamlFile.getAbsolutePath() + " is not valid.";

        expectBuildFailureAndOutput(expectedLineInOutput);
    }

    @Test
    public void shouldAllowYamlWithDuplicateKeyWhenDuplicationIsDisabled() {

        writeDuplicateKeyYaml();
        writeFile("plugins { id 'at.zierler.yamlvalidator' }\n" +
                "yamlValidator {\n" +
                "\tallowDuplicates = true\n" +
                "}", buildFile);

        String expectedLineInOutput = yamlFile.getAbsolutePath() + " is valid.";

        expectBuildSuccessAndOutput(expectedLineInOutput);
    }

    @Test
    public void shouldAllowValidYaml() {

        writeDefaultBuildFileWithoutProperties();
        writeFile("framework:\n  key: value\n  other: value\n\nother:\n  other: value\n  key: value", yamlFile);

        String expectedLineInOutput = yamlFile.getAbsolutePath() + " is valid.";

        expectBuildSuccessAndOutput(expectedLineInOutput);
    }

    @Test
    public void shouldSearchInMultipleFolderWhenDefined() throws IOException {

        String yamlDirectory1 = "src/any/resources/";
        String yamlDirectory2 = "src/other/resources/";
        testProjectDir.newFolder(yamlDirectory1.split("/"));
        testProjectDir.newFolder(yamlDirectory2.split("/"));
        writeFile("plugins { id 'at.zierler.yamlvalidator' }\n" +
                "yamlValidator { searchPaths = ['" + yamlDirectory1 + "','" + yamlDirectory2 + "'] }", buildFile);

        String expectedLineInOutput1 = "Starting to validate yaml files in " + yamlDirectory1 + ".";
        String expectedLineInOutput2 = "Starting to validate yaml files in " + yamlDirectory2 + ".";

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

        String expectedLineInOutput1 = yamlFile1.getAbsolutePath() + " is valid.";
        String expectedLineInOutput2 = yamlFile2.getAbsolutePath() + " is valid.";

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

        String expectedLineInOutput1 = yamlFile1.getAbsolutePath() + " is valid.";
        String expectedLineInOutput2 = yamlFile2.getAbsolutePath() + " is valid.";

        String output = runBuildAndGetOutput();

        assertThat(output, containsString(expectedLineInOutput1));
        assertThat(output, containsString(expectedLineInOutput2));
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
