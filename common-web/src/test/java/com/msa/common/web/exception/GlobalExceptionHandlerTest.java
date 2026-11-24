package com.msa.common.web.exception;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.msa.common.core.error.CommonErrorCode;
import com.msa.common.core.error.ErrorCode;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static void assertError(ResultActions result, ErrorCode errorCode) throws Exception {
        result.andExpect(status().is(errorCode.status().value()))
                .andExpect(jsonPath("$.status").value(errorCode.status().value()))
                .andExpect(jsonPath("$.code").value(errorCode.code()))
                .andExpect(jsonPath("$.message").value(errorCode.message()))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void BusinessException_은_예외의_에러코드로_응답한다() throws Exception {
        ResultActions result = mockMvc.perform(get("/test/business"));

        assertError(result, CommonErrorCode.ALREADY_DELETED);
        result.andExpect(jsonPath("$.errors").value(nullValue()))
                .andExpect(content().string(not(containsString("couponId=1"))));
    }

    @Test
    void BusinessException_이_5xx_면_해당_상태로_응답한다() throws Exception {
        assertError(mockMvc.perform(get("/test/business-5xx")), CommonErrorCode.EXTERNAL_SERVICE_ERROR);
    }

    @Test
    void RequestBody_검증_실패는_필드별_에러를_담는다() throws Exception {
        ResultActions result = mockMvc.perform(post("/test/body")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "", "discountRate": 150, "password": " "}
                        """));

        assertError(result, CommonErrorCode.INVALID_INPUT);
        result.andExpect(jsonPath("$.errors", hasSize(3)))
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("name", "discountRate", "password")))
                .andExpect(jsonPath("$.errors[?(@.field == 'discountRate')].value").value(150))
                .andExpect(jsonPath("$.errors[?(@.field == 'discountRate')].reason").value("할인율은 100 이하여야 합니다."))
                .andExpect(jsonPath("$.errors[?(@.field == 'password')].value", contains(nullValue())));
    }

    @Test
    void ModelAttribute_검증_실패는_필드별_에러를_담는다() throws Exception {
        ResultActions result = mockMvc.perform(get("/test/model")
                .param("name", "쿠폰")
                .param("discountRate", "150")
                .param("password", "pw"));

        assertError(result, CommonErrorCode.INVALID_INPUT);
        result.andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].field").value("discountRate"))
                .andExpect(jsonPath("$.errors[0].reason").value("할인율은 100 이하여야 합니다."));
    }

    @Test
    void 메서드_파라미터_검증_실패는_파라미터별_에러를_담는다() throws Exception {
        ResultActions result = mockMvc.perform(get("/test/method-validation").param("size", "0"));

        assertError(result, CommonErrorCode.INVALID_INPUT);
        result.andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].field").value("size"))
                .andExpect(jsonPath("$.errors[0].value").value(0))
                .andExpect(jsonPath("$.errors[0].reason").value("size 는 1 이상이어야 합니다."));
    }

    @Test
    void ConstraintViolationException_은_경로의_마지막_노드를_field_로_쓴다() throws Exception {
        ResultActions result = mockMvc.perform(get("/test/constraint"));

        assertError(result, CommonErrorCode.INVALID_INPUT);
        result.andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].field").value("discountRate"))
                .andExpect(jsonPath("$.errors[0].value").value(150))
                .andExpect(jsonPath("$.errors[0].reason").value("할인율은 100 이하여야 합니다."));
    }

    @Test
    void 타입_변환_실패는_INVALID_TYPE_VALUE() throws Exception {
        ResultActions result = mockMvc.perform(get("/test/type").param("id", "abc"));

        assertError(result, CommonErrorCode.INVALID_TYPE_VALUE);
        result.andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].field").value("id"))
                .andExpect(jsonPath("$.errors[0].value").value("abc"));
    }

    @Test
    void 필수_파라미터_누락은_MISSING_PARAMETER() throws Exception {
        ResultActions result = mockMvc.perform(get("/test/missing"));

        assertError(result, CommonErrorCode.MISSING_PARAMETER);
        result.andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].field").value("keyword"));
    }

    @Test
    void 읽을_수_없는_JSON_은_INVALID_JSON() throws Exception {
        ResultActions result = mockMvc.perform(post("/test/body")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ invalid"));

        assertError(result, CommonErrorCode.INVALID_JSON);
        result.andExpect(jsonPath("$.errors").value(nullValue()));
    }

    @Test
    void 지원하지_않는_메서드는_METHOD_NOT_ALLOWED() throws Exception {
        ResultActions result = mockMvc.perform(delete("/test/business"));

        assertError(result, CommonErrorCode.METHOD_NOT_ALLOWED);
        result.andExpect(jsonPath("$.errors").value(nullValue()));
    }

    @Test
    void NoResourceFoundException_은_NOT_FOUND() throws Exception {
        assertError(mockMvc.perform(get("/test/no-resource")), CommonErrorCode.NOT_FOUND);
    }

    @Test
    void NoHandlerFoundException_은_NOT_FOUND() throws Exception {
        assertError(mockMvc.perform(get("/test/no-handler")), CommonErrorCode.NOT_FOUND);
    }

    @Test
    void 그_외_예외는_INTERNAL_SERVER_ERROR_이고_내부_메시지를_노출하지_않는다() throws Exception {
        ResultActions result = mockMvc.perform(get("/test/error"));

        assertError(result, CommonErrorCode.INTERNAL_SERVER_ERROR);
        result.andExpect(jsonPath("$.errors").value(nullValue()))
                .andExpect(content().string(not(containsString("password=1234"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }

    @Test
    void 성공_응답도_HTTP_상태와_body_status_가_같다() throws Exception {
        mockMvc.perform(get("/test/ok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("COMMON2000"))
                .andExpect(jsonPath("$.data.couponId").value(1));

        mockMvc.perform(post("/test/created"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.code").value("COMMON2010"));
    }
}
