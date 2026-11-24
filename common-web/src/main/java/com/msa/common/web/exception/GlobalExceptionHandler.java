package com.msa.common.web.exception;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.msa.common.core.error.BusinessException;
import com.msa.common.core.error.CommonErrorCode;
import com.msa.common.core.error.ErrorCode;
import com.msa.common.web.response.ApiResponse;
import com.msa.common.web.response.ApiResponses;
import com.msa.common.web.response.FieldErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e, HttpServletRequest request) {
        return respond(e.getErrorCode(), List.of(), e, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
                                                                          HttpServletRequest request) {
        return respond(CommonErrorCode.INVALID_INPUT, toFieldErrors(e.getFieldErrors()), e, request);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBind(BindException e, HttpServletRequest request) {
        return respond(CommonErrorCode.INVALID_INPUT, toFieldErrors(e.getFieldErrors()), e, request);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleHandlerMethodValidation(HandlerMethodValidationException e,
                                                                           HttpServletRequest request) {
        List<FieldErrorResponse> errors = new ArrayList<>();
        for (ParameterValidationResult result : e.getParameterValidationResults()) {
            if (result instanceof ParameterErrors parameterErrors) {
                errors.addAll(toFieldErrors(parameterErrors.getFieldErrors()));
                continue;
            }
            String name = result.getMethodParameter().getParameterName();
            result.getResolvableErrors().forEach(error ->
                    errors.add(FieldErrorResponse.of(name, result.getArgument(), error.getDefaultMessage())));
        }
        return respond(CommonErrorCode.INVALID_INPUT, errors, e, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException e,
                                                                       HttpServletRequest request) {
        List<FieldErrorResponse> errors = e.getConstraintViolations().stream()
                .map(violation -> FieldErrorResponse.of(lastNodeName(violation), violation.getInvalidValue(),
                        violation.getMessage()))
                .toList();
        return respond(CommonErrorCode.INVALID_INPUT, errors, e, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e,
                                                                HttpServletRequest request) {
        String requiredType = e.getRequiredType() == null ? "알 수 없음" : e.getRequiredType().getSimpleName();
        FieldErrorResponse error = FieldErrorResponse.of(e.getName(), e.getValue(),
                requiredType + " 타입이어야 합니다.");
        return respond(CommonErrorCode.INVALID_TYPE_VALUE, List.of(error), e, request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException e,
                                                                    HttpServletRequest request) {
        FieldErrorResponse error = FieldErrorResponse.of(e.getParameterName(), null, "필수 파라미터입니다.");
        return respond(CommonErrorCode.MISSING_PARAMETER, List.of(error), e, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException e,
                                                               HttpServletRequest request) {
        return respond(CommonErrorCode.INVALID_JSON, List.of(), e, request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e,
                                                                      HttpServletRequest request) {
        return respond(CommonErrorCode.METHOD_NOT_ALLOWED, List.of(), e, request);
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleNotFound(Exception e, HttpServletRequest request) {
        return respond(CommonErrorCode.NOT_FOUND, List.of(), e, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e, HttpServletRequest request) {
        return respond(CommonErrorCode.INTERNAL_SERVER_ERROR, List.of(), e, request);
    }

    private ResponseEntity<ApiResponse<Void>> respond(ErrorCode errorCode, List<FieldErrorResponse> errors,
                                                      Exception e, HttpServletRequest request) {
        if (errorCode.status().is5xxServerError()) {
            log.error("[{}] {} {}", errorCode.code(), request.getMethod(), request.getRequestURI(), e);
        } else {
            log.warn("[{}] {} {} - {}", errorCode.code(), request.getMethod(), request.getRequestURI(),
                    e.getMessage());
        }
        return ApiResponses.fail(errorCode, errors);
    }

    private List<FieldErrorResponse> toFieldErrors(List<FieldError> fieldErrors) {
        return fieldErrors.stream()
                .map(error -> FieldErrorResponse.of(error.getField(), error.getRejectedValue(),
                        error.getDefaultMessage()))
                .toList();
    }

    private String lastNodeName(ConstraintViolation<?> violation) {
        String name = null;
        for (Path.Node node : violation.getPropertyPath()) {
            name = node.getName();
        }
        return name;
    }
}
