package com.team4.rental.exceptions;

import com.team4.rental.dtos.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import tools.jackson.databind.ObjectMapper;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final ObjectMapper objectMapper;

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(AppException exception) {
        var error = exception.getErrorCode();
        return ResponseEntity.status(error.getStatus()).body(ApiResponse.<Void>builder()
                .code(error.getCode()).message(error.getMessage()).build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
        var errors = new LinkedHashMap<String, String>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(ApiResponse.<Void>builder()
                .code(ErrorCode.INVALID_REQUEST.getCode()).message("Dữ liệu không hợp lệ")
                .errors(errors).build());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            HandlerMethodValidationException.class})
    public ResponseEntity<ApiResponse<Void>> handleInvalidInput(Exception exception) {
        return ResponseEntity.badRequest().body(ApiResponse.<Void>builder()
                .code(ErrorCode.INVALID_REQUEST.getCode()).message("Dữ liệu không hợp lệ").build());
    }

    @ExceptionHandler({DataIntegrityViolationException.class, PessimisticLockingFailureException.class})
    public ResponseEntity<ApiResponse<Void>> handleConflict(Exception exception) {
        return ResponseEntity.status(409).body(ApiResponse.<Void>builder()
                .code(ErrorCode.DATA_CONFLICT.getCode())
                .message(ErrorCode.DATA_CONFLICT.getMessage()).build());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(AccessDeniedException exception) {
        return ResponseEntity.status(403).body(ApiResponse.<Void>builder()
                .code(ErrorCode.ACCESS_DENIED.getCode()).message(ErrorCode.ACCESS_DENIED.getMessage()).build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
        log.error("Unhandled rental error", exception);
        return ResponseEntity.internalServerError().body(ApiResponse.<Void>builder()
                .code(ErrorCode.INTERNAL_ERROR.getCode()).message(ErrorCode.INTERNAL_ERROR.getMessage()).build());
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        response.setStatus(401);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.<Void>builder()
                .code(ErrorCode.UNAUTHENTICATED.getCode()).message(ErrorCode.UNAUTHENTICATED.getMessage()).build());
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        response.setStatus(403);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.<Void>builder()
                .code(ErrorCode.ACCESS_DENIED.getCode()).message(ErrorCode.ACCESS_DENIED.getMessage()).build());
    }
}
