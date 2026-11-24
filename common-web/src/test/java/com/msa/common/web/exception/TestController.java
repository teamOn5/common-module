package com.msa.common.web.exception;

import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.msa.common.core.error.BusinessException;
import com.msa.common.core.error.CommonErrorCode;
import com.msa.common.web.response.ApiResponse;
import com.msa.common.web.response.ApiResponses;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/test")
class TestController {

    record CouponRequest(
            @NotBlank(message = "이름은 필수입니다.") String name,
            @Max(value = 100, message = "할인율은 100 이하여야 합니다.") Integer discountRate,
            @NotBlank(message = "비밀번호는 필수입니다.") String password
    ) {
    }

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @GetMapping("/ok")
    ResponseEntity<ApiResponse<Map<String, Object>>> ok() {
        return ApiResponses.ok(Map.of("couponId", 1));
    }

    @PostMapping("/created")
    ResponseEntity<ApiResponse<Map<String, Object>>> created() {
        return ApiResponses.created(Map.of("couponId", 1));
    }

    @GetMapping("/business")
    ResponseEntity<ApiResponse<Void>> business() {
        throw new BusinessException(CommonErrorCode.ALREADY_DELETED, "couponId=1");
    }

    @GetMapping("/business-5xx")
    ResponseEntity<ApiResponse<Void>> business5xx() {
        throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_ERROR, "payment-service timeout");
    }

    @PostMapping("/body")
    ResponseEntity<ApiResponse<Void>> body(@Valid @RequestBody CouponRequest request) {
        return ApiResponses.ok();
    }

    @GetMapping("/model")
    ResponseEntity<ApiResponse<Void>> model(@Valid @ModelAttribute CouponRequest request) {
        return ApiResponses.ok();
    }

    @GetMapping("/method-validation")
    ResponseEntity<ApiResponse<Void>> methodValidation(
            @RequestParam @Min(value = 1, message = "size 는 1 이상이어야 합니다.") int size) {
        return ApiResponses.ok();
    }

    @GetMapping("/constraint")
    ResponseEntity<ApiResponse<Void>> constraint() {
        Set<ConstraintViolation<CouponRequest>> violations =
                validator.validate(new CouponRequest("쿠폰", 150, "pw"));
        throw new ConstraintViolationException(violations);
    }

    @GetMapping("/type")
    ResponseEntity<ApiResponse<Void>> type(@RequestParam Long id) {
        return ApiResponses.ok();
    }

    @GetMapping("/missing")
    ResponseEntity<ApiResponse<Void>> missing(@RequestParam String keyword) {
        return ApiResponses.ok();
    }

    @GetMapping("/no-resource")
    ResponseEntity<ApiResponse<Void>> noResource() throws NoResourceFoundException {
        throw new NoResourceFoundException(HttpMethod.GET, "/test/no-resource", "no-resource");
    }

    @GetMapping("/no-handler")
    ResponseEntity<ApiResponse<Void>> noHandler() throws NoHandlerFoundException {
        throw new NoHandlerFoundException("GET", "/test/no-handler", HttpHeaders.EMPTY);
    }

    @GetMapping("/error")
    ResponseEntity<ApiResponse<Void>> error() {
        throw new IllegalStateException("DB password=1234 내부 오류");
    }
}
