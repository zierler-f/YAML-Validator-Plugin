package at.zierler.gradle;

import lombok.Data;

@Data
public class ValidationProperties {

    private String directory = "src/main/resources/";
    private boolean allowDuplicates = true;

}
