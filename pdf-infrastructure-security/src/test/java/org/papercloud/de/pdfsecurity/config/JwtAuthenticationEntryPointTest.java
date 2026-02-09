package org.papercloud.de.pdfsecurity.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import org.springframework.http.MediaType;

class JwtAuthenticationEntryPointTest {

    private final JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void commence_shouldReturn401Status() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AuthenticationException authException = new BadCredentialsException("Bad credentials");

        when(request.getServletPath()).thenReturn("/api/test");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ServletOutputStream sos = new ServletOutputStream() {
            @Override
            public boolean isReady() { return true; }
            @Override
            public void setWriteListener(WriteListener writeListener) {}
            @Override
            public void write(int b) { baos.write(b); }
        };
        when(response.getOutputStream()).thenReturn(sos);

        entryPoint.commence(request, response, authException);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
    }

    @Test
    void commence_shouldReturnJsonBodyWithErrorDetails() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AuthenticationException authException = new BadCredentialsException("Invalid token");

        when(request.getServletPath()).thenReturn("/api/documents");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ServletOutputStream sos = new ServletOutputStream() {
            @Override
            public boolean isReady() { return true; }
            @Override
            public void setWriteListener(WriteListener writeListener) {}
            @Override
            public void write(int b) { baos.write(b); }
        };
        when(response.getOutputStream()).thenReturn(sos);

        entryPoint.commence(request, response, authException);

        Map<String, Object> body = objectMapper.readValue(baos.toByteArray(), Map.class);

        assertEquals(401, body.get("status"));
        assertEquals("Unauthorized", body.get("error"));
        assertEquals("Invalid token", body.get("message"));
        assertEquals("/api/documents", body.get("path"));
    }

    @Test
    void commence_shouldIncludeCorrectServletPath() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AuthenticationException authException = new BadCredentialsException("Unauthorized");

        when(request.getServletPath()).thenReturn("/api/users/me");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ServletOutputStream sos = new ServletOutputStream() {
            @Override
            public boolean isReady() { return true; }
            @Override
            public void setWriteListener(WriteListener writeListener) {}
            @Override
            public void write(int b) { baos.write(b); }
        };
        when(response.getOutputStream()).thenReturn(sos);

        entryPoint.commence(request, response, authException);

        Map<String, Object> body = objectMapper.readValue(baos.toByteArray(), Map.class);
        assertEquals("/api/users/me", body.get("path"));
    }
}
