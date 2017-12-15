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

    private static final String YAML_DIRECTORY = "src/test/resources/";

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder();

    @Before
    public void setupTestProject() throws IOException {

        File buildFile = testProjectDir.newFile("build.gradle");
        writeFile("plugins { id 'at.zierler.yamlvalidator' }\n" +
                "yamlValidator { directory = '" + YAML_DIRECTORY + "' }", buildFile);
    }

    @Test
    public void shouldSetYamlValidatorFileCorrectly() {

        BuildResult result = GradleRunner
                .create()
                .withProjectDir(testProjectDir.getRoot())
                .withPluginClasspath()
                .withArguments(TASK_NAME)
                .build();

        String output = result.getOutput();
        BuildTask task = result.task(":" + TASK_NAME);

        assertThat(output, containsString("Starting to validate yaml files in " + YAML_DIRECTORY + "."));
        assertThat(task.getOutcome(), is(TaskOutcome.SUCCESS));
    }

}
