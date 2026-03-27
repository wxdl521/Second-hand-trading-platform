package com.syxs.common.exception;

import com.syxs.common.result.R;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusinessException(BusinessException ex) {
        return R.fail(ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldError() == null
            ? "Invalid request parameters"
            : ex.getBindingResult().getFieldError().getDefaultMessage();
        return R.fail(message);
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception ex) {
        return R.fail(ex.getMessage());
    }
}
