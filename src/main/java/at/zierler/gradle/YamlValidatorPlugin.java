package at.zierler.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskContainer;

public class YamlValidatorPlugin implements Plugin<Project> {

    static final String VALIDATE_YAML_TASK_NAME = "validateYaml";

    private YamlValidatorTask yamlValidatorTask;

    @Override
    public void apply(Project project) {

        project.getExtensions().create("yamlValidator", ValidationProperties.class);

        TaskContainer tasks = project.getTasks();

        yamlValidatorTask = tasks.create(VALIDATE_YAML_TASK_NAME, YamlValidatorTask.class);

        tasks.whenTaskAdded(this::makeTaskDependOnYamlValidatorTaskIfTaskNameIsCheck);
    }

    private void makeTaskDependOnYamlValidatorTaskIfTaskNameIsCheck(Task task) {

        if ("check".equals(task.getName()) && yamlValidatorTask != null) {
            task.dependsOn(yamlValidatorTask);
        }
    }

}
