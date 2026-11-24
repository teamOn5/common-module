package com.msa.common.web.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.msa.common.core.error.CommonErrorCode;

import tools.jackson.databind.json.JsonMapper;

class ApiResponseTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void ok_는_200_과_성공_코드를_담는다() {
        ApiResponse<String> response = ApiResponse.ok("data");

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.code()).isEqualTo(SuccessCode.OK.code());
        assertThat(response.message()).isEqualTo(SuccessCode.OK.message());
        assertThat(response.data()).isEqualTo("data");
        assertThat(response.errors()).isNull();
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    void 데이터_없는_ok_는_data_가_null() {
        ApiResponse<Void> response = ApiResponse.ok();

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.data()).isNull();
        assertThat(response.errors()).isNull();
    }

    @Test
    void created_는_201_과_생성_코드를_담는다() {
        ApiResponse<String> response = ApiResponse.created("data");

        assertThat(response.status()).isEqualTo(201);
        assertThat(response.code()).isEqualTo(SuccessCode.CREATED.code());
        assertThat(response.message()).isEqualTo(SuccessCode.CREATED.message());
        assertThat(response.data()).isEqualTo("data");
    }

    @Test
    void fail_은_에러코드의_status_code_message_를_담는다() {
        ApiResponse<Void> response = ApiResponse.fail(CommonErrorCode.NOT_FOUND);

        assertThat(response.status()).isEqualTo(404);
        assertThat(response.code()).isEqualTo("COMMON4040");
        assertThat(response.message()).isEqualTo(CommonErrorCode.NOT_FOUND.message());
        assertThat(response.data()).isNull();
        assertThat(response.errors()).isNull();
    }

    @Test
    void fail_은_필드_에러를_담는다() {
        List<FieldErrorResponse> errors = List.of(FieldErrorResponse.of("discountRate", 150, "할인율은 100 이하여야 합니다."));

        ApiResponse<Void> response = ApiResponse.fail(CommonErrorCode.INVALID_INPUT, errors);

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.errors()).containsExactlyElementsOf(errors);
    }

    @Test
    void errors_가_null_이거나_비어있으면_null() {
        assertThat(ApiResponse.fail(CommonErrorCode.INVALID_INPUT, null).errors()).isNull();
        assertThat(ApiResponse.fail(CommonErrorCode.INVALID_INPUT, List.of()).errors()).isNull();
    }

    @Test
    void errors_는_불변_복사본이다() {
        List<FieldErrorResponse> source = new ArrayList<>(
                List.of(FieldErrorResponse.of("name", "", "이름은 필수입니다.")));

        ApiResponse<Void> response = ApiResponse.fail(CommonErrorCode.INVALID_INPUT, source);
        source.clear();

        assertThat(response.errors()).hasSize(1);
    }

    @Test
    void JSON_필드_순서가_고정된다() {
        String json = jsonMapper.writeValueAsString(ApiResponse.ok(Map.of("id", 1)));

        assertThat(json.indexOf("\"status\""))
                .isLessThan(json.indexOf("\"code\""));
        assertThat(json.indexOf("\"code\"")).isLessThan(json.indexOf("\"message\""));
        assertThat(json.indexOf("\"message\"")).isLessThan(json.indexOf("\"data\""));
        assertThat(json.indexOf("\"data\"")).isLessThan(json.indexOf("\"errors\""));
        assertThat(json.indexOf("\"errors\"")).isLessThan(json.indexOf("\"timestamp\""));
    }

    @Test
    void data_와_errors_가_null_이어도_JSON_에_포함된다() {
        String json = jsonMapper.writeValueAsString(ApiResponse.ok());

        assertThat(json).contains("\"data\":null");
        assertThat(json).contains("\"errors\":null");
    }

    @Test
    void timestamp_는_UTC_ISO_문자열로_직렬화된다() {
        String json = jsonMapper.writeValueAsString(ApiResponse.ok());

        assertThat(jsonMapper.readTree(json).get("timestamp").asString())
                .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z");
    }

    @Test
    void ApiResponses_는_HTTP_상태와_body_status_를_맞춘다() {
        ResponseEntity<ApiResponse<String>> ok = ApiResponses.ok("data");
        ResponseEntity<ApiResponse<Void>> okEmpty = ApiResponses.ok();
        ResponseEntity<ApiResponse<String>> created = ApiResponses.created("data");
        ResponseEntity<ApiResponse<Void>> fail = ApiResponses.fail(CommonErrorCode.CONFLICT);
        ResponseEntity<ApiResponse<Void>> failWithErrors = ApiResponses.fail(CommonErrorCode.INVALID_INPUT,
                List.of(FieldErrorResponse.of("name", "", "이름은 필수입니다.")));

        assertThat(ok.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(okEmpty.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(fail.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(failWithErrors.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        for (ResponseEntity<? extends ApiResponse<?>> entity : List.of(ok, okEmpty, created, fail, failWithErrors)) {
            assertThat(entity.getStatusCode().value()).isEqualTo(entity.getBody().status());
        }
        assertThat(failWithErrors.getBody().errors()).hasSize(1);
    }
}
