package at.zierler.gradle;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
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

    @Before
    public void setupTestProject() throws IOException {

        File buildFile = testProjectDir.newFile("build.gradle");
        testProjectDir.newFolder(yamlDirectory.split("/"));
        yamlFile = testProjectDir.newFile(yamlDirectory + "file.yaml");
        writeFile("plugins { id 'at.zierler.yamlvalidator' }\n" +
                "yamlValidator { directory = '" + yamlDirectory + "' }", buildFile);
    }

    @Test
    public void shouldSetYamlValidatorFileCorrectly() {

        BuildResult buildResult = GradleRunner
                .create()
                .withProjectDir(testProjectDir.getRoot())
                .withPluginClasspath()
                .withArguments(TASK_NAME)
                .build();

        String output = buildResult.getOutput();
        BuildTask task = buildResult.task(":" + TASK_NAME);

        assertThat(output, containsString("Starting to validate yaml files in " + yamlDirectory + "."));
        assertThat(task.getOutcome(), is(TaskOutcome.SUCCESS));
    }

    @Test
    public void shouldAllowEmptyYaml() {

        BuildResult buildResult = GradleRunner
                .create()
                .withProjectDir(testProjectDir.getRoot())
                .withPluginClasspath()
                .withArguments(TASK_NAME)
                .build();

        String output = buildResult.getOutput();
        BuildTask task = buildResult.task(":" + TASK_NAME);

        assertThat(output, containsString(yamlFile.getAbsolutePath() + " is valid."));
        assertThat(task.getOutcome(), is(TaskOutcome.SUCCESS));
    }

    @Test
    public void shouldNotAllowYamlWithDuplicateKey() {

        writeFile("framework:\n  key: value\n\nframework:\n  other: value", yamlFile);

        GradleRunner
                .create()
                .withProjectDir(testProjectDir.getRoot())
                .withPluginClasspath()
                .withArguments(TASK_NAME)
                .buildAndFail();
    }

    @Test
    public void shouldAllowValidYaml() {

        writeFile("framework:\n  key: value\n  other: value\n\nother:\n  other: value\n  key: value", yamlFile);

        System.out.println();

        GradleRunner
                .create()
                .withProjectDir(testProjectDir.getRoot())
                .withPluginClasspath()
                .withArguments(TASK_NAME)
                .build();
    }

}
