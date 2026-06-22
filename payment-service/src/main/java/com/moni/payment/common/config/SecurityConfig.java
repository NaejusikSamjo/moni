package com.moni.payment.common.config;

import com.moni.common.security.GatewayHeaderAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@Profile("!local")
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/api/v1/auth/**",
                "/swagger-ui/**", "/v3/api-docs/**",
                "/actuator/**",
                "/toss-billing-test.html", "/toss-success.html", "/toss-fail.html"
            )
            .permitAll()
            .anyRequest().authenticated()
        )
        .addFilterBefore(new GatewayHeaderAuthenticationFilter() {
          @Override
          protected boolean shouldNotFilter(HttpServletRequest request) {
            String path = request.getRequestURI();
            if (path.equals("/toss-billing-test.html")
                || path.equals("/toss-success.html")
                || path.equals("/toss-fail.html")) {
              return true;
            }
            return super.shouldNotFilter(request);
          }
        }, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}
