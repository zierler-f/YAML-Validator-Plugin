package at.zierler.gradle;

import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class ValidationProperties {

    static final String DEFAULT_DIRECTORY = "src/main/resources/";

    private List<String> searchPaths = Collections.singletonList(DEFAULT_DIRECTORY);
    private boolean allowDuplicates = false;

}
