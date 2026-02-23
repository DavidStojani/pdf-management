package org.papercloud.de.pdfservice.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientInfoExtractor {

    private final HttpServletRequest request;

    public String getClientIp() {
        try {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            log.debug("Could not extract client IP", e);
            return null;
        }
    }

    public String getClientUserAgent() {
        try {
            return request.getHeader("User-Agent");
        } catch (Exception e) {
            log.debug("Could not extract User-Agent", e);
            return null;
        }
    }
}
