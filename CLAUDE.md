# CLAUDE.md

이 레포에서 작업할 때 따라야 할 지침입니다.
사용법과 배포 규칙의 상세 설명은 `README.md` 를 참고합니다.

## 작업 방식

- 응답과 문서는 한글로 작성한다.
- 작업 전에 기존 설정(`settings.gradle`, `build.gradle`, `gradle.properties`, 기존 모듈)을 먼저 확인하고 그대로 따른다.
  요청 내용이 기존 설정과 다르면 진행 전에 차이를 표로 보여주고 확인받는다.
- 요청 범위만 작업한다. 요청에 없는 모듈, 유틸, 리팩터링은 추가하지 않는다.
- 사용자가 직접 수정한 내용은 되돌리지 않고 현재 상태로 받아들인다.

## 프로젝트 기준

| 항목 | 값 |
|---|---|
| 빌드 | Gradle (Groovy DSL) |
| Java | 17 |
| Spring Boot | 4.1.2 (BOM 으로 버전 관리) |
| group | `com.teamon5` |
| 패키지 루트 | `com.msa.common.<모듈>` (예: `com.msa.common.core`) |
| 배포 | GitHub Packages (`teamOn5/common-module`) |

## 모듈 규칙

- 모듈은 레포 루트 바로 아래에 둔다: `include 'common-xxx'`, 디렉토리명 = 모듈명.
  (CI 가 디렉토리명으로 변경을 감지하고 `<모듈명>-v*` 태그를 만든다)
- 모든 하위 모듈은 `java-library` 로만 빌드한다. Spring Boot 플러그인 적용 금지 (bootJar 생성 X).
- 의존성 버전은 명시하지 않는다 (Spring Boot BOM 사용).
- 모듈 간 의존은 `api` 로 전이한다.
- 공통 설정(toolchain, Lombok, JUnit/AssertJ, 배포)은 루트 `build.gradle` 의 `subprojects` 블록에 있다.
  루트 `build.gradle`, `settings.gradle`, `gradle.properties`, `gradle/` 은 공통 빌드 파일이라 수정하면 CI 가 **모든 모듈을 배포**한다.
- JPA 관련 코드는 공통 모듈에 두지 않는다 (각 서비스의 adapter/out/persistence).

### common-core

- 외부 의존성은 `spring-web` (`HttpStatus`) 하나만 `api` 로 둔다. Lombok 은 제외한다.
- 에러 코드
  - `ErrorCode`: `HttpStatus status()` (Spring), `String code()`, `String message()`
  - code 형식: `{도메인 접두사}{HTTP 상태 3자리}{순번 1자리}`, 전부 대문자, `^[A-Z]+\d{4}$`
  - code 안의 상태 3자리 = `status().value()`, status 는 400~599
  - 각 서비스의 ErrorCode enum 테스트에서 `ErrorCodeValidator.validate(XxxErrorCode.values())` 로 검증한다.
- 예외는 `BusinessException` 하나로 통일한다 (종류별 예외 클래스 금지).
  detail 은 로그용, 사용자 응답에는 `errorCode.message()` 를 쓴다.
- 만들지 않는 것: ApiResponse, 예외 핸들러, 필드 검증 에러 객체 (common-web), 도메인별 에러 코드 (각 서비스).

### common-monitoring

### common-web

- HTTP 응답과 예외 변환만 담당한다. Spring 의존성은 `compileOnly` (`spring-boot-starter-webmvc`, `-validation`, `spring-boot-autoconfigure`).
- 응답은 `ApiResponse` (status, code, message, data, errors, timestamp 순서). null 필드도 JSON 에 포함한다.
  errors 는 필드 에러가 없으면 null, 있을 때만 리스트다.
- Controller 응답은 `ApiResponses` 로 만들어 HTTP 상태 코드와 body 의 status 를 항상 일치시킨다.
- 500 응답에는 내부 예외 메시지를 노출하지 않는다. 로그는 4xx WARN (스택트레이스 없음), 5xx ERROR (스택트레이스 포함).
- `GlobalExceptionHandler` 는 `CommonWebAutoConfiguration` 에서 `@ConditionalOnMissingBean` 으로 등록한다.
- 만들지 않는 것: `@CurrentUser`/ArgumentResolver, Feign 설정, traceId, ObjectMapper 전역 설정 변경.

### common-monitoring

- `spring-boot-starter-actuator`, `micrometer-registry-prometheus` 를 `api` 로 노출한다.
- 기본 설정은 `EnvironmentPostProcessor` 로 **최저 우선순위**에 등록해 서비스 설정이 항상 우선하게 한다.
- Boot 4 는 `org.springframework.boot.EnvironmentPostProcessor`, Boot 3 는 `org.springframework.boot.env.EnvironmentPostProcessor` 키를 쓴다.
  두 키 모두 등록하고, 기본값은 한 번만 등록되게 한다.
- Spring Boot 4 는 모듈이 분리되어 클래스 패키지가 바뀐 경우가 많다. 사용 전에 실제 jar 에서 패키지를 확인한다.

## 코드 스타일

- 주석은 최소한으로 쓴다. 설명조의 긴 주석, 상수별 Javadoc, `@param` 나열, 빌드 파일/스크립트의 설명 주석을 넣지 않는다.
  요구사항에 명시된 Javadoc 만 짧게 남기고, TODO 주석은 넣지 않는다.
- 테스트는 JUnit 5 + AssertJ, 메서드명은 한글 + 언더스코어 (예: `code_가_중복되면_예외`).
- `README.md` 는 사용법 문서다. 변경 내역, 작업 기록은 넣지 않는다.

## Git 규칙

- 기본 브랜치: 이 레포는 develop 없이 `main` 에서 작업 브랜치를 만들고 `main` 으로 PR 한다. `main` 에 직접 push 하지 않는다.
- 작업 순서: 이슈 생성 → 브랜치 생성 → 작업 → 테스트 → 커밋 → push → PR (`main` 대상)
- 이슈는 담당자를 요청자로 지정하고, 조직 프로젝트 "O(N) 개발 프로젝트" 에 연결한다.
- 브랜치명: `{커밋 타입}/#{이슈번호}-{브랜치명}` (예: `feat/#3-common-core`, `fix/#5-error-code`)
  커밋 타입은 `feat`, `fix`, `chore`, `docs`, `refactor`, `test`, `ci` 등이다 (동사 아님).
- 커밋 메시지: 한글, `{타입} : {메시지} (#{이슈번호})` (예: `feat : 공통 예외 및 에러코드 추가 (#3)`)
- **커밋 전에 반드시 변경 파일을 표(번호, 파일, 상태, 변경 내용)로 먼저 보여준다.**
- 테스트가 통과한 경우에만 커밋한다. 캐시된 결과가 아닌지 확인한다 (`--rerun-tasks`).
- PR 하나의 변경 파일은 20개 이하로 유지한다. 넘을 것 같으면 먼저 알린다.
- `main` 에 머지되면 배포가 실행되므로, 배포하지 않을 커밋을 `main` 에 올릴 때는 메시지에 `[skip ci]` 를 붙인다.

## PR 템플릿

```markdown
### summary
### changes
### background (or etc.)
### AI 활용 내역
- **AI 작업:**
- **개발자 작업:**
- **검증 및 리뷰:**
- **토큰 사용량:** (표)
```

- AI 활용 내역은 모두 채운다.
- 토큰 사용량은 세션 기록(`~/.claude/projects/<프로젝트>/<세션ID>.jsonl`)의 assistant 메시지 `usage` 를
  해당 기능 요청 시점부터 메시지 id 기준으로 중복 없이 합산해 아래 표로 적는다.

  | 구분 | 토큰 |
  |---|---|
  | 입력 | |
  | 출력 | |
  | 캐시 쓰기 | |
  | 캐시 읽기 | |
  | **합계** | |

## 배포 (CI/CD)

- `main` 에 push 되면 모듈별로 마지막 태그 이후 변경을 감지해 바뀐 모듈만 배포한다 (`.github/scripts/release-plan.sh`).
- 태그: 루트는 `v*`, 하위 모듈은 `<모듈명>-v*`
- 버전: `feat!`/`BREAKING CHANGE` → major, `feat` → minor, `fix`/`perf` → patch, 그 외 → 배포 안 함 (콜론 앞 공백 허용)
- 배포 대상은 커밋 scope 가 아니라 **변경된 파일 경로**로 결정된다.
- GitHub Packages 는 같은 버전을 다시 올릴 수 없다.

## 서비스에서 사용하는 저장소 설정

```groovy
maven {
    name = "GithubPackages"
    url = uri("https://maven.pkg.github.com/teamon5/common-module")
    credentials {
        username = System.getenv("GITHUB_USERNAME") ?: findProperty("githubUsername")
        password = System.getenv("GITHUB_TOKEN") ?: findProperty("githubToken")
    }
}
```

## 환경 주의 (Windows)

- PowerShell 5.1 에서 `gh issue/pr create` 의 여러 줄 본문은 따옴표가 깨질 수 있으므로 `--body-file` 로 전달한다.
- bash 스크립트는 LF 로 저장한다 (`.sh` 는 레포에 LF 로 커밋됨).
