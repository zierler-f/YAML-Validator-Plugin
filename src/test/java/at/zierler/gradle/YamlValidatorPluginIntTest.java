package at.zierler.gradle;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static at.zierler.gradle.YamlValidatorPlugin.TASK_NAME;
import static org.gradle.util.GFileUtils.writeFile;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class YamlValidatorPluginIntTest {

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder();
    private String yamlDirectory = "src/test/resources/";
    private File yamlFile;
    private BuildResult buildResult;

    @Before
    public void setupTestProject() throws IOException {

        File buildFile = testProjectDir.newFile("build.gradle");
        testProjectDir.newFolder(yamlDirectory.split("/"));
        yamlFile = testProjectDir.newFile(yamlDirectory + "file.yaml");
        writeFile("plugins { id 'at.zierler.yamlvalidator' }\n" +
                "yamlValidator { directory = '" + yamlDirectory + "' }", buildFile);

        buildResult = GradleRunner
                .create()
                .withProjectDir(testProjectDir.getRoot())
                .withPluginClasspath()
                .withArguments(TASK_NAME)
                .build();
    }

    @After
    public void printOutput() {

        System.out.println(buildResult.getOutput());
    }

    @Test
    public void shouldSetYamlValidatorFileCorrectly() {

        String output = buildResult.getOutput();
        BuildTask task = buildResult.task(":" + TASK_NAME);

        assertThat(output, containsString("Starting to validate yaml files in " + yamlDirectory + "."));
        assertThat(task.getOutcome(), is(TaskOutcome.SUCCESS));
    }

    @Test
    public void shouldAllowValidYaml() {

        String output = buildResult.getOutput();
        BuildTask task = buildResult.task(":" + TASK_NAME);

        assertThat(output, containsString(yamlFile.getAbsolutePath() + " is valid."));
        assertThat(task.getOutcome(), is(TaskOutcome.SUCCESS));
    }

}
