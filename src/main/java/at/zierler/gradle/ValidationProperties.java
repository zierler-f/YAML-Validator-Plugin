package at.zierler.gradle;

import lombok.Data;

@Data
public class ValidationProperties {

    static final String DEFAULT_DIRECTORY = "src/main/resources/";

    private String directory = DEFAULT_DIRECTORY;
    private boolean allowDuplicates = false;

}
