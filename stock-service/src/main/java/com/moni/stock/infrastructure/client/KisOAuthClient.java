package com.moni.stock.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class KisOAuthClient {

    private final KisProperties kisProperties;

    private String approvalKey;
    private String accessToken;
    private LocalDateTime tokenExpiry = LocalDateTime.MIN;

    public synchronized void resetApprovalKey() {
        this.approvalKey = null;
        log.info("KIS approval_key 초기화");
    }

    // WebSocket 구독에 사용하는 approval_key (REST API 토큰과 별개)
    public synchronized String getApprovalKey() {
        if (approvalKey != null) return approvalKey;

        ApprovalKeyResponse response = RestClient.create(kisProperties.getRestUrl())
                .post()
                .uri("/oauth2/Approval")
                .header("content-type", "application/json")
                .body(new ApprovalKeyRequest(kisProperties.getAppKey(), kisProperties.getAppSecret()))
                .retrieve()
                .body(ApprovalKeyResponse.class);

        approvalKey = response.approvalKey();
        log.info("KIS approval_key 발급 완료");
        return approvalKey;
    }

    // REST API 호출에 사용하는 access_token
    public synchronized String getAccessToken() {
        if (accessToken != null && LocalDateTime.now().isBefore(tokenExpiry)) return accessToken;

        AccessTokenResponse response = RestClient.create(kisProperties.getRestUrl())
                .post()
                .uri("/oauth2/tokenP")
                .header("content-type", "application/json")
                .body(new AccessTokenRequest(kisProperties.getAppKey(), kisProperties.getAppSecret()))
                .retrieve()
                .body(AccessTokenResponse.class);

        accessToken = response.accessToken();
        tokenExpiry = LocalDateTime.now().plusSeconds(response.expiresIn() - 60);
        log.info("KIS access_token 발급 완료 (만료: {})", tokenExpiry);
        return accessToken;
    }

    record ApprovalKeyRequest(
            @JsonProperty("grant_type") String grantType,
            @JsonProperty("appkey") String appKey,
            @JsonProperty("secretkey") String secretKey
    ) {
        ApprovalKeyRequest(String appKey, String secretKey) {
            this("client_credentials", appKey, secretKey);
        }
    }

    record ApprovalKeyResponse(
            @JsonProperty("approval_key") String approvalKey
    ) {}

    record AccessTokenRequest(
            @JsonProperty("grant_type") String grantType,
            @JsonProperty("appkey") String appKey,
            @JsonProperty("appsecret") String appSecret
    ) {
        AccessTokenRequest(String appKey, String appSecret) {
            this("client_credentials", appKey, appSecret);
        }
    }

    record AccessTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expiresIn
    ) {}
}