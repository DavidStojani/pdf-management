package org.papercloud.de.pdfapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "org.papercloud.de.pdfdatabase.repository")
@EntityScan(basePackages = "org.papercloud.de.pdfdatabase.entity")
@ComponentScan(basePackages = "org.papercloud.de")
public class PdfApiApplication {
  public static void main(String[] args) {
    SpringApplication.run(PdfApiApplication.class, args);
  }
}
