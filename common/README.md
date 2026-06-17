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
    public AuditorAware<UUID> auditorProvider() {
        return new AuditorAwareImpl();
    }
}
```

> `BaseEntity`의 `createdBy` / `updatedBy` 필드는 `String` 타입이며,
> UUID는 Spring의 타입 변환을 통해 문자열로 자동 저장됩니다.

---