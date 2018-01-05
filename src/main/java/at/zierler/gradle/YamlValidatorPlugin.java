package at.zierler.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskContainer;

public class YamlValidatorPlugin implements Plugin<Project> {

    static final String TASK_NAME = "validateYaml";

    @Override
    public void apply(Project project) {

        project.getExtensions().create("yamlValidator", ValidationProperties.class);

        TaskContainer tasks = project.getTasks();

        final YamlValidatorTask yamlValidatorTask = tasks.create(TASK_NAME, YamlValidatorTask.class);

        tasks.whenTaskAdded(task -> {
            if ("check".equals(task.getName())) {
                task.dependsOn(yamlValidatorTask);
            }
        });
    }

}
