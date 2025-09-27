package com.laioffer.deliver.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 捕获业务异常
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(
            BusinessException ex, HttpServletRequest req
    ) {
        return buildResponse(ex.getCode(), ex.getMessage(), ex.getStatus(), req);
    }

    // 捕获未预料的异常
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleOtherExceptions(
            Exception ex, HttpServletRequest req
    ) {
        ex.printStackTrace();
        return buildResponse("INTERNAL_ERROR", "服务器内部错误", HttpStatus.INTERNAL_SERVER_ERROR, req);
    }

    private ResponseEntity<Map<String, Object>> buildResponse(
            String code, String message, HttpStatus status, HttpServletRequest req
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("status", status.value());  // 👈 新增 status
        body.put("path", req.getRequestURI());
        body.put("timestamp", OffsetDateTime.now().toString());
        return ResponseEntity.status(status).body(body);
    }
}
