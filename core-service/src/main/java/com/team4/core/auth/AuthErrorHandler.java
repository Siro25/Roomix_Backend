package com.team4.core.auth;

import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.team4.core.face.FaceAuthController;
import com.team4.core.face.FaceAuthFailure;

@org.springframework.core.annotation.Order(0)
@RestControllerAdvice(assignableTypes = FaceAuthController.class)
public class AuthErrorHandler {
    @ExceptionHandler(FaceAuthFailure.class)
    public ResponseEntity<Map<String, String>> faceFailed(FaceAuthFailure failure) {
        return error(failure.status(), failure.getMessage());
    }
    @ExceptionHandler(AuthFailure.class)
    public ResponseEntity<Map<String, String>> authenticationFailed(AuthFailure failure) {
        return error(failure.status(), failure.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<Map<String, String>> invalidInput(Exception exception) {
        return error(400, "INVALID_REQUEST");
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> databaseUnavailable(DataAccessException exception) {
        return error(503, "DATABASE_UNAVAILABLE");
    }

    private ResponseEntity<Map<String, String>> error(int status, String code) {
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(Map.of("code", code));
    }
}
