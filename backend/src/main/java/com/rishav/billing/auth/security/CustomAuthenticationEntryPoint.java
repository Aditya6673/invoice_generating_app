package com.rishav.billing.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rishav.billing.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public CustomAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        String message = (authException != null && authException.getMessage() != null)
                ? authException.getMessage()
                : "Full authentication is required to access this resource";

        ApiResponse<Void> errorResponse = ApiResponse.error("Unauthorized: " + message);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
