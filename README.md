# common-module

MSA 서비스들이 공통으로 사용하는 모듈 모음입니다.
각 모듈은 독립된 라이브러리로 GitHub Packages 에 배포됩니다.

- Spring Boot 4.1.2 (BOM 으로 의존성 버전 관리)
- Java 17
- Gradle (Groovy DSL), group `com.teamon5`, 패키지 루트 `com.msa.common`

## 모듈 구성

| 모듈 | 설명 |
|---|---|
| `common-module` (루트) | 공통 모듈 |
| `common-core` | 에러 코드, 공통 예외, 공통 enum, 페이지 결과 |
| `common-web` | 공통 응답 객체, 전역 예외 처리 |
| `common-monitoring` | Actuator, Micrometer, Prometheus 기본 설정 |

> 모든 하위 모듈은 `java-library` 로 빌드되며 Spring Boot 플러그인을 적용하지 않습니다 (bootJar 생성 X).

---

## 서비스에서 사용하기

### 1. GitHub Packages 저장소 추가

GitHub Packages 는 **읽을 때도 인증이 필요**합니다.

인증 정보는 환경변수 또는 `~/.gradle/gradle.properties` 중 하나로 지정합니다 (환경변수가 우선).

환경변수 (CI 등)

```bash
GITHUB_USERNAME=본인-github-id
GITHUB_TOKEN=ghp_xxx   # read:packages 권한 토큰
```

또는 `~/.gradle/gradle.properties` (커밋하지 말 것)

```properties
githubUsername=본인-github-id
githubToken=ghp_xxx   # read:packages 권한 토큰
```

서비스 `build.gradle`

```groovy
repositories {
    mavenCentral()
    maven {
        name = "GithubPackages"
        url = uri("https://maven.pkg.github.com/teamon5/common-module")
        credentials {
            username = System.getenv("GITHUB_USERNAME") ?: findProperty("githubUsername")
            password = System.getenv("GITHUB_TOKEN") ?: findProperty("githubToken")
        }
    }
}
```

### 2. 필요한 모듈 의존성 추가

```groovy
dependencies {
    implementation 'com.teamon5:common-monitoring:<버전>'
}
```

버전은 저장소의 Releases 또는 태그(`common-monitoring-v*`)에서 확인합니다.

---

## common-core

모든 계층(domain, application, adapter)과 모든 서비스가 사용하는 최하위 모듈입니다.
외부 의존성은 `spring-web` (`HttpStatus`) 하나만 사용합니다 (`api` 로 전이).

| 패키지 | 내용 |
|---|---|
| `com.msa.common.core.error` | `ErrorCode`, `CommonErrorCode`, `BusinessException`, `ErrorCodeValidator` |
| `com.msa.common.core.type` | `UserRole`, `ShopRole`, `MediaType` |
| `com.msa.common.core.page` | `PageConstants`, `PageResult`, `CursorResult` |

### 에러 코드 규칙

| 구분 | 예시 | 용도 |
|---|---|---|
| enum 상수명 | `COUPON_NOT_FOUND` | 코드에서 참조하는 이름 |
| `code()` | `"COUPON4040"` | 응답 JSON 의 `code` 필드 |
| `status()` | `HttpStatus.NOT_FOUND` | HTTP 상태 코드, 응답 JSON 의 `status` 필드 (`value()` 로 404) |

- code 형식: `{도메인 접두사}{HTTP 상태 3자리}{순번 1자리}`, 전부 대문자 (`^[A-Z]+\d{4}$`)
- code 안의 상태 3자리는 `status().value()` 와 같아야 합니다
- `HttpStatus` 는 Spring 의 `org.springframework.http.HttpStatus` 입니다

각 서비스는 자기 에러 코드 enum 을 만들고, 테스트에서 규칙을 검증합니다.

```java
public enum CouponErrorCode implements ErrorCode {
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "COUPON4040", "쿠폰을 찾을 수 없습니다."),
    COUPON_EXPIRED(HttpStatus.BAD_REQUEST, "COUPON4000", "만료된 쿠폰입니다.");
    // 생성자, status(), code(), message()
}

@Test
void 에러_코드_규칙을_지킨다() {
    ErrorCodeValidator.validate(CouponErrorCode.values());
}
```

### 예외

예외는 `BusinessException` 하나로 통일합니다. detail 은 **로그용**이고, 사용자 응답에는 `errorCode.message()` 를 씁니다.

```java
throw new BusinessException(CouponErrorCode.COUPON_NOT_FOUND, "couponId=" + couponId);
```

---

## common-web

웹 어댑터(Controller)에서 사용하는 공통 응답 객체와 전역 예외 처리입니다.
의존성만 추가하면 `GlobalExceptionHandler` 가 자동 등록됩니다 (서블릿 웹 애플리케이션).

```groovy
dependencies {
    implementation 'com.teamon5:common-web:<버전>'
}
```

Spring Web, Validation 은 `compileOnly` 라서 서비스에 `spring-boot-starter-webmvc`(또는 `-web`), `spring-boot-starter-validation` 이 있어야 합니다.

### 응답 형식

```json
{
  "status": 400,
  "code": "COMMON4000",
  "message": "입력값이 올바르지 않습니다.",
  "data": null,
  "errors": [
    { "field": "discountRate", "value": 150, "reason": "할인율은 100 이하여야 합니다." }
  ],
  "timestamp": "2026-11-24T10:00:00.000Z"
}
```

- 필드 순서는 고정이고, null 필드도 포함됩니다.
- `errors` 는 필드 에러가 없으면 `null` 입니다.
- `password`, `token`, `secret` 이 들어간 필드의 `value` 는 `null` 로 마스킹됩니다.

### Controller 에서 사용

HTTP 상태 코드와 body 의 `status` 가 항상 같도록 `ApiResponses` 로 응답합니다.

```java
@GetMapping("/coupons/{id}")
public ResponseEntity<ApiResponse<CouponResponse>> get(@PathVariable Long id) {
    return ApiResponses.ok(couponService.get(id));
}

@PostMapping("/coupons")
public ResponseEntity<ApiResponse<CouponResponse>> create(@Valid @RequestBody CouponRequest request) {
    return ApiResponses.created(couponService.create(request));
}
```

### 예외 처리

| 예외 | 에러 코드 |
|---|---|
| `BusinessException` | 예외의 errorCode |
| `MethodArgumentNotValidException`, `BindException`, `HandlerMethodValidationException`, `ConstraintViolationException` | `INVALID_INPUT` (필드별 errors) |
| `MethodArgumentTypeMismatchException` | `INVALID_TYPE_VALUE` |
| `MissingServletRequestParameterException` | `MISSING_PARAMETER` |
| `HttpMessageNotReadableException` | `INVALID_JSON` |
| `HttpRequestMethodNotSupportedException` | `METHOD_NOT_ALLOWED` |
| `NoResourceFoundException`, `NoHandlerFoundException` | `NOT_FOUND` |
| 그 외 `Exception` | `INTERNAL_SERVER_ERROR` (내부 메시지 노출 안 함) |

직접 처리하고 싶으면 `GlobalExceptionHandler` 를 상속한 빈을 등록하면 기본 핸들러는 등록되지 않습니다.

---

## common-monitoring

의존성만 추가하면 별도 코드나 설정 없이 아래 기본값이 적용됩니다.

| 설정 | 기본값 | 설명 |
|---|---|---|
| `management.endpoints.web.exposure.include` | `health,info,prometheus` | Actuator 엔드포인트 노출 |
| `management.metrics.tags.application` | `${spring.application.name:unknown}` | 모든 메트릭에 서비스 이름 태그 부여 |
| `management.metrics.distribution.percentiles-histogram.http.server.requests` | `true` | p95/p99 레이턴시 집계용 히스토그램 |
| `eureka.instance.metadata-map[prometheus.scrape]` | `true` | Prometheus `eureka_sd_configs` 수집 대상 표시 |
| `eureka.instance.metadata-map[prometheus.path]` | `/actuator/prometheus` | Prometheus 수집 경로 |
| `eureka.instance.prefer-ip-address` | `true` | 컨테이너 환경에서 IP 로 등록 |

포함된 의존성 (`api` 로 전이): `spring-boot-starter-actuator`, `micrometer-registry-prometheus`

### 사용 방법

```yaml
# application.yml
spring:
  application:
    name: order-service
```

서비스 실행 후 `http://localhost:8080/actuator/prometheus` 에서 메트릭에
`application="order-service"` 태그가 붙어 있으면 정상입니다.

### 기본값 변경

기본값은 **가장 낮은 우선순위**로 등록되므로 서비스 설정에 같은 키를 쓰면 그 값이 적용됩니다.

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,prometheus   # info 제외
  metrics:
    tags:
      application: custom-name
```

### 동작 원리

`MonitoringEnvironmentPostProcessor` 가 `META-INF/spring.factories` 로 등록되어,
애플리케이션 시작 시 `commonMonitoringDefaults` 라는 property source 를 맨 뒤(최저 우선순위)에 추가합니다.

Spring Boot 3 서비스에서도 동작합니다. Boot 3 는 `org.springframework.boot.env.EnvironmentPostProcessor` 키로
등록된 `LegacyMonitoringEnvironmentPostProcessor` 를, Boot 4 는 `MonitoringEnvironmentPostProcessor` 를 사용하며,
두 키가 모두 읽혀도 기본값은 한 번만 등록됩니다.

---

## 배포 (CI/CD)

`main` 에 push 되면 **모듈별로 변경사항을 감지해 바뀐 모듈만** 배포합니다.
워크플로: `.github/workflows/publish.yml`, 배포 계획 계산: `.github/scripts/release-plan.sh`

### 태그 규칙

| 모듈 | 태그 형식 | 예 |
|---|---|---|
| 루트 (`common-module`) | `v<버전>` | `v0.0.1` |
| 하위 모듈 | `<모듈명>-v<버전>` | `common-monitoring-v0.1.0` |

### 변경 감지 기준

모듈별 마지막 태그 이후의 커밋 중, 아래 경로를 건드린 커밋만 봅니다.

- 해당 모듈 디렉토리 (루트는 `src/`, 하위 모듈은 `common-xxx/`)
- 공통 빌드 파일: `build.gradle`, `settings.gradle`, `gradle.properties`, `gradle/`, `gradlew`
  → 이 파일들이 바뀌면 **모든 모듈**이 대상이 됩니다

모듈 목록은 `settings.gradle` 의 `include` 에서 자동으로 읽으므로, 모듈을 추가해도 CI 를 수정할 필요가 없습니다.

### 버전 결정 (Conventional Commits)

| 커밋 메시지 | 버전 |
|---|---|
| `feat!:`, `fix!:` 등 또는 본문에 `BREAKING CHANGE:` | major |
| `feat:` | minor |
| `fix:`, `perf:` | patch |
| `docs:`, `chore:`, `refactor:`, `test:`, `ci:` 등 | 배포 안 함 |

콜론 앞 공백도 허용합니다 (`feat : 공통 예외 및 에러코드 추가` 도 minor).

배포 대상은 커밋 메시지의 scope 가 아니라 **변경된 파일 경로**로 결정됩니다.
scope 는 이력을 읽기 쉽게 하는 용도로 쓰면 됩니다.

```
feat(monitoring): 공통 태그에 env 추가          → common-monitoring 만 minor
fix(core): ErrorCode 메시지 오타 수정            → common-core 만 patch
chore: 의존성 정리 (build.gradle 수정)           → 배포 안 함
fix: Spring Boot 업그레이드 (build.gradle 수정)  → 모든 모듈 patch
```

### 배포 흐름

1. 모듈별 배포 계획 계산 (새 버전, 태그)
2. `./gradlew build` 로 전체 빌드와 테스트
3. 바뀐 모듈만 `publish`
4. 배포에 성공한 모듈만 태그와 GitHub Release 생성 (노트에는 해당 모듈 경로의 커밋만 포함)

배포하지 않는 모듈도 현재 버전을 Gradle 에 전달하므로, 모듈 간 의존(`api project(':common-core')`)이
POM 에 올바른 버전으로 기록됩니다.

### 수동 배포

GitHub → Actions → **Publish packages to GitHub Packages** → Run workflow

| 입력 | 설명 |
|---|---|
| `bump` | `auto` (커밋 메시지로 결정) / `patch` / `minor` / `major` (강제) |
| `modules` | 대상 모듈. 쉼표 구분, 비우면 전체 (예: `common-monitoring,common-core`) |

예: `bump=patch`, `modules=common-monitoring` → 변경이 없어도 `common-monitoring` 만 patch 배포

### 로컬에서 배포 계획 미리 보기

Git Bash 에서:

```bash
bash .github/scripts/release-plan.sh auto
cat release-plan.txt gradle-args.txt
rm release-plan.txt gradle-args.txt
```

로컬 Maven 저장소에 배포해서 서비스에서 테스트하려면:

```bash
./gradlew :common-monitoring:publishToMavenLocal
```

서비스 `repositories` 에 `mavenLocal()` 을 추가하고 `0.0.1-SNAPSHOT` 으로 받으면 됩니다.

### 주의

- GitHub Packages 는 **같은 버전을 다시 올릴 수 없습니다**. 태그를 지우고 재배포하면 실패합니다.
- 아직 한 번도 배포되지 않은 모듈에 다른 모듈이 의존하면 POM 이 `0.0.0` 을 가리킬 수 있습니다
  (스크립트가 경고를 출력합니다). 처음에는 관련 모듈을 함께 배포하세요.

---

## 로컬 빌드

```bash
./gradlew build
```

모듈별 버전을 지정해서 빌드하려면:

```bash
./gradlew build -Pversion.common-monitoring=1.0.0
```

## 새 모듈 추가하기

1. `settings.gradle` 에 `include 'common-xxx'` 추가
2. `common-xxx/build.gradle` 생성 (Java toolchain, Lombok, 테스트 의존성, 배포 설정은 루트 `subprojects` 블록에서 자동 적용)
3. 의존성 버전은 명시하지 않음 (Spring Boot BOM 사용)
4. `feat(xxx): ...` 커밋으로 push 하면 `common-xxx-v0.1.0` 으로 첫 배포
