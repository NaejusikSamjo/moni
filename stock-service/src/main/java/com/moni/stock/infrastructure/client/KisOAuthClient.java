package com.moni.stock.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.core.util.Json;
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

    public synchronized JsonNode getThemeInfo(String themeCode) {

        JsonNode body = RestClient.create(kisProperties.getRestUrl())
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/quotations/inquire-index-price")
                        .queryParam("FID_COND_MRKT_DIV_CODE", "U") // 업종(U) 고정
                        .queryParam("FID_INPUT_ISCD", themeCode) // 조회할 업종 코드
                        .build())
                .header("content-type", "application/json; charset=utf-8")
                .header("authorization", "Bearer " + getAccessToken())
                .header("appkey", kisProperties.getAppKey())
                .header("appsecret", kisProperties.getAppSecret())
                .header("tr_id", "FHPUP02100000")
                .header("custtype", "P")
                .retrieve()
                .body(JsonNode.class);

        log.debug("테마 : {}", body);
        return body;

    }

    public synchronized JsonNode getCandle(String ticker, String targetTime) {
        JsonNode body = RestClient.create(kisProperties.getRestUrl())
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/quotations/inquire-time-itemchartprice")
                        .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                        .queryParam("FID_INPUT_ISCD", ticker)
                        .queryParam("FID_INPUT_HOUR_1", targetTime)
                        .queryParam("FID_PW_DATA_INCU_YN", "N")
                        .queryParam("FID_ETC_CLS_CODE", "0")
                        .build())
                .header("content-type", "application/json; charset=utf-8")
                .header("authorization", "Bearer " + getAccessToken())
                .header("appkey", kisProperties.getAppKey())
                .header("appsecret", kisProperties.getAppSecret())
                .header("tr_id", "FHKST03010200")
                .header("custtype", "P")
                .retrieve()
                .body(JsonNode.class);

        log.info("candle : {}", body);
        return body;

    }

    public JsonNode getCurrentPrice(String ticker) {
        JsonNode body = RestClient.create(kisProperties.getRestUrl())
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/quotations/inquire-price")
                        .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                        .queryParam("FID_INPUT_ISCD", ticker)
                        .build())
                .header("content-type", "application/json; charset=utf-8")
                .header("authorization", "Bearer " + getAccessToken())
                .header("appkey", kisProperties.getAppKey())
                .header("appsecret", kisProperties.getAppSecret())
                .header("tr_id", "FHKST01010100")
                .header("custtype", "P")
                .retrieve()
                .body(JsonNode.class);

//        log.info("현재가 조회 - ticker: {}", ticker);
        return body;
    }

    public JsonNode getVolumeRank () {

        JsonNode body = RestClient.create(kisProperties.getRestUrl())
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/uapi/domestic-stock/v1/quotations/volume-rank")
                        .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                        .queryParam("FID_COND_SCR_DIV_CODE", "20171")
                        .queryParam("FID_INPUT_ISCD", "0000")
                        .queryParam("FID_DIV_CLS_CODE", "0")
                        .queryParam("FID_BLNG_CLS_CODE", "0")
                        .queryParam("FID_TRGT_CLS_CODE", "111111111")
                        .queryParam("FID_TRGT_EXLS_CLS_CODE", "0000000000")
                        .queryParam("FID_INPUT_PRICE_1", "")
                        .queryParam("FID_INPUT_PRICE_2", "")
                        .queryParam("FID_VOL_CNT", "")
                        .build()
                )
                .header("content-type", "application/json; charset=utf-8")
                .header("authorization", "Bearer " + getAccessToken())
                .header("appkey", kisProperties.getAppKey())
                .header("appsecret", kisProperties.getAppSecret())
                .header("tr_id", "FHPST01710000")
                .header("custtype", "P")
                .retrieve()
                .body(JsonNode.class);

        return body;
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