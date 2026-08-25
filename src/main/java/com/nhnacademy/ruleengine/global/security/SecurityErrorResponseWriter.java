package com.nhnacademy.ruleengine.global.security;

import com.nhnacademy.inventory.global.dto.ApiResponse;
import com.nhnacademy.inventory.global.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
@RequiredArgsConstructor
public class SecurityErrorResponseWriter {
    private final ObjectMapper objectMapper;

    public void write(
            HttpServletResponse response,
            ErrorCode errorCode
    ) throws IOException {
        write(null, response, errorCode, null);
    }

    public void write(
            HttpServletRequest request,
            HttpServletResponse response,
            ErrorCode errorCode,
            Exception exception
    ) throws IOException {
        log.warn(
                "event=security_error exceptionType={} errorCode={} code={} "
                        + "httpStatus={} method={} path={}",
                exception == null ? "none" : exception.getClass().getSimpleName(),
                errorCode.getName(),
                errorCode.getCode(),
                errorCode.getStatus().value(),
                request == null ? "unknown" : request.getMethod(),
                request == null ? "unknown" : request.getRequestURI()
        );

        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        objectMapper.writeValue(
                response.getOutputStream(),
                ApiResponse.error(errorCode.getCode(), errorCode.getMessage())
        );
    }
}
