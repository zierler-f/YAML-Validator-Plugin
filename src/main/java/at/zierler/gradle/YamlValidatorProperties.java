package at.zierler.gradle;

import lombok.Data;

@Data
public class YamlValidatorProperties {

    public static final YamlValidatorProperties DEFAULT = new YamlValidatorProperties();

    private String directory = "src/main/resources/";
    private boolean allowDuplicates = true;

}
