package org.papercloud.de.pdfservice.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
@Configuration
@ComponentScan(basePackages = {
        "org.papercloud.de.pdfocr",
        "org.papercloud.de.pdfsecurity"
})
public class PdfServiceAutoConfiguration {
}
