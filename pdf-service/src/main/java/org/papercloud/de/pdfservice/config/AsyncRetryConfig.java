package org.papercloud.de.pdfservice.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@EnableAsync
@EnableRetry
public class AsyncRetryConfig {
}