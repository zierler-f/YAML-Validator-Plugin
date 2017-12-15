package at.zierler.gradle.internal;

import at.zierler.gradle.YamlValidatorExtension;
import at.zierler.gradle.YamlValidatorTask;
import org.gradle.api.tasks.TaskAction;

public class YamlValidatorTaskImpl extends YamlValidatorTask {

    @Override
    @TaskAction
    public void validateYaml() {

        YamlValidatorExtension extension = getProject().getExtensions().findByType(YamlValidatorExtension.class);
        if (extension == null) {
            extension = new YamlValidatorExtension();
        }
        System.out.printf("Starting to validate yaml files in %s.", extension.getDirectory());
    }

}
