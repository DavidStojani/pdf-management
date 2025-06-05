package org.papercloud.de.pdfservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "document.text-cleaning")
@Data
public class TextCleaningConfiguration {
    private String separatorPattern = ".*[|/\\\\_~^*]{3,}.*";
    private String nonStandardCharsPattern = "[^\\p{L}\\p{N}\\p{P}\\p{Z}]";
    private String multipleSpacesPattern = "\\s{2,}";
    private int minimumLineLength = 3;
}