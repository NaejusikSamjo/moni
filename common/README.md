# common 모듈 사용 가이드

## 개요

`common` 모듈은 각 서비스에서 공통으로 사용하는 클래스를 제공합니다.
`@Component` 등 빈 등록 어노테이션이 **없는 순수 클래스**로 작성되어 있으며,
**각 서비스가 직접 빈으로 등록**해서 사용합니다.

---

## 보안 (`com.moni.common.security`)

### GatewayHeaderAuthenticationFilter

API Gateway에서 주입한 `X-Gateway-Secret` 헤더를 검증하고,
`X-User-Id` / `X-User-Role` 헤더를 `SecurityContextHolder`에 등록하는 필터입니다.

- `X-User-Id` : 있으면 인증 세팅 (필수 조건)
- `X-User-Role` : 선택. 없으면 빈 authorities로 인증 — 서비스 간 Feign 호출처럼 role이 없는 경우에도 `.anyRequest().authenticated()` 통과 가능

**각 서비스 `SecurityConfig`에서 등록:**

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**", "/swagger-ui/**", "/v3/api-docs/**", "/actuator/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(new GatewayHeaderAuthenticationFilter(),
                UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

**환경변수 필요:** `GATEWAY_SECRET`

**`docker-compose.yml`에 추가:**

```yaml
environment:
  GATEWAY_SECRET: ${GATEWAY_SECRET}
```

### SecurityUtil

`SecurityContextHolder`에서 현재 인증된 사용자 정보를 꺼내는 정적 유틸입니다.

- `Optional<UUID> SecurityUtil.getCurrentUserId()`
- `Optional<String> SecurityUtil.getCurrentUserRole()`

---

## JPA Auditing (`com.moni.common.security.AuditorAwareImpl`)

`@CreatedBy` / `@LastModifiedBy` 필드에 현재 로그인한 사용자 UUID를 자동으로 채워줍니다.
`SecurityContextHolder` 기반이므로 `GatewayHeaderAuthenticationFilter`와 함께 사용해야 합니다.

**각 서비스에 `JpaConfig` 생성:**

```java
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return new AuditorAwareImpl();
    }
}
```

> `BaseEntity`의 `createdBy` / `updatedBy` 필드는 `String` 타입입니다.
> `AuditorAwareImpl`은 내부적으로 UUID를 `toString()`으로 변환해 반환하므로 반드시 `AuditorAware<String>`으로 선언해야 합니다.
> `AuditorAware<UUID>`로 선언하면 `ClassCastException`이 발생합니다.

---

## 에러 처리 (`com.moni.common.error`)

### ExceptionHandlerSupport

`CustomException` / `MethodArgumentNotValidException`을 `GlobalResponse` 형태로 변환하는 **정적 유틸**입니다.
빈 등록 없이 각 서비스의 `@RestControllerAdvice`에서 바로 호출합니다.

**각 서비스에 `GlobalExceptionHandler` 생성:**

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<GlobalResponse<Void>> handleCustomException(CustomException e) {
        return ExceptionHandlerSupport.handle(e);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GlobalResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        return ExceptionHandlerSupport.handle(e);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalResponse<Void>> handleException(Exception e) {
        return ExceptionHandlerSupport.handle(e);
    }
}
```

> `Exception.class` catch-all 핸들러를 반드시 포함해야 합니다.
> 이 핸들러가 없으면 예상치 못한 예외가 Spring의 `/error`로 포워드되고,
> Spring Security가 `/error`를 인증 없는 요청으로 판단해 `GlobalResponse` 형식 없이 403을 반환합니다.

### CustomException / ErrorCode

도메인별 에러 코드는 각 서비스에서 `ErrorCode` 인터페이스를 구현합니다.

```java
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
    USER_NOT_FOUND("USER-001", "존재하지 않는 사용자입니다.", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus status;
    // getter override
}
```

에러 발생 시:

```
throw new CustomException(UserErrorCode.USER_NOT_FOUND);
```

---

## 응답 형식 (`com.moni.common.response`)

### GlobalResponse

모든 API 응답을 `{ status, message, data, errors }` 형태로 통일하는 래퍼 클래스입니다.
컨트롤러에서 **정적 메서드로 직접 호출**합니다.

**성공 응답:**

```
return ResponseEntity.status(HttpStatus.CREATED)
        .body(GlobalResponse.success(HttpStatus.CREATED.value(), responseDto));
```

**에러 응답** (ExceptionHandlerSupport 내부에서 자동 처리):

```
GlobalResponse.failure(status, errorCode, errorResponse);
```