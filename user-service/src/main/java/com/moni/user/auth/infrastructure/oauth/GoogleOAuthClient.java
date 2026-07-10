package com.moni.user.auth.infrastructure.oauth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.moni.common.error.exception.CustomException;
import com.moni.user.auth.application.oauth.OAuthUserInfo;
import com.moni.user.auth.domain.exception.AuthErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
public class GoogleOAuthClient extends AbstractOAuthClient {

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";
    private static final String AUTH_BASE_URL = "https://accounts.google.com/o/oauth2/v2/auth";

    public GoogleOAuthClient(
            @Value("${oauth.google.client-id}") String clientId,
            @Value("${oauth.google.client-secret}") String clientSecret,
            @Value("${oauth.google.redirect-uri}") String redirectUri,
            RestClient restClient) {
        super(clientId, clientSecret, redirectUri, restClient);
    }

    @Override
    public OAuthUserInfo exchangeCodeForUser(String code, String codeVerifier) {
        GoogleTokenResponse tokenResponse = exchangeCode(code, codeVerifier);
        return fetchUserInfo(tokenResponse.accessToken());
    }

    @Override
    public String buildLoginUrl(String codeChallenge, String state) {
        return UriComponentsBuilder.fromUriString(AUTH_BASE_URL)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email profile")
                .queryParam("state", state)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256")
                .build()
                .toUriString();
    }

    private GoogleTokenResponse exchangeCode(String code, String codeVerifier) {
        try {
            return restClient.post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(buildTokenExchangeBody(code, codeVerifier))
                    .retrieve()
                    .body(GoogleTokenResponse.class);
        } catch (RestClientException e) {
            log.warn("[OAUTH] 구글 토큰 교환 실패 - {}", e.getMessage());
            throw new CustomException(AuthErrorCode.OAUTH_EXCHANGE_FAILED);
        }
    }

    private OAuthUserInfo fetchUserInfo(String accessToken) {
        try {
            GoogleUserInfoResponse response = restClient.get()
                    .uri(USERINFO_URL)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(GoogleUserInfoResponse.class);

            if (response == null || response.sub() == null || response.email() == null) {
                throw new CustomException(AuthErrorCode.OAUTH_EXCHANGE_FAILED);
            }
            if (!Boolean.TRUE.equals(response.emailVerified())) {
                throw new CustomException(AuthErrorCode.OAUTH_EXCHANGE_FAILED);
            }

            return new OAuthUserInfo(response.sub(), response.email(), response.name(), null);
        } catch (RestClientException e) {
            log.warn("[OAUTH] 구글 사용자 정보 조회 실패 - {}", e.getMessage());
            throw new CustomException(AuthErrorCode.OAUTH_EXCHANGE_FAILED);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    private record GoogleTokenResponse(String accessToken) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    private record GoogleUserInfoResponse(String sub, String email, Boolean emailVerified, String name) {}
}
