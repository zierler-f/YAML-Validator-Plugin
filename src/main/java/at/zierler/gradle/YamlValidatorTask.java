package at.zierler.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

public abstract class YamlValidatorTask extends DefaultTask {

    @TaskAction
    public abstract void validateYaml() throws Exception;

}
