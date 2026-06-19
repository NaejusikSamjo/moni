package com.moni.user.auth.infrastructure.oauth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.moni.common.error.exception.CustomException;
import com.moni.user.auth.application.OAuthUserInfo;
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
public class KakaoOAuthClient extends AbstractOAuthClient {

    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USERINFO_URL = "https://kapi.kakao.com/v2/user/me";
    private static final String AUTH_BASE_URL = "https://kauth.kakao.com/oauth/authorize";

    public KakaoOAuthClient(
            @Value("${oauth.kakao.client-id}") String clientId,
            @Value("${oauth.kakao.client-secret}") String clientSecret,
            @Value("${oauth.kakao.redirect-uri}") String redirectUri,
            RestClient restClient) {
        super(clientId, clientSecret, redirectUri, restClient);
    }

    @Override
    public OAuthUserInfo exchangeCodeForUser(String code, String codeVerifier) {
        KakaoTokenResponse tokenResponse = exchangeCode(code, codeVerifier);
        return fetchUserInfo(tokenResponse.accessToken());
    }

    @Override
    public String buildLoginUrl(String codeChallenge, String state) {
        return UriComponentsBuilder.fromUriString(AUTH_BASE_URL)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("state", state)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256")
                .build()
                .toUriString();
    }

    private KakaoTokenResponse exchangeCode(String code, String codeVerifier) {
        try {
            return restClient.post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(buildTokenExchangeBody(code, codeVerifier))
                    .retrieve()
                    .body(KakaoTokenResponse.class);
        } catch (RestClientException e) {
            log.warn("[OAUTH] 카카오 토큰 교환 실패 - {}", e.getMessage());
            throw new CustomException(AuthErrorCode.OAUTH_EXCHANGE_FAILED);
        }
    }

    private OAuthUserInfo fetchUserInfo(String accessToken) {
        try {
            KakaoUserInfoResponse response = restClient.get()
                    .uri(USERINFO_URL)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(KakaoUserInfoResponse.class);

            if (response == null || response.id() == null || response.kakaoAccount() == null) {
                throw new CustomException(AuthErrorCode.OAUTH_EXCHANGE_FAILED);
            }

            KakaoUserInfoResponse.KakaoAccount account = response.kakaoAccount();
            if (account.email() == null
                    || Boolean.TRUE.equals(account.emailNeedsAgreement())
                    || !Boolean.TRUE.equals(account.isEmailValid())
                    || !Boolean.TRUE.equals(account.isEmailVerified())) {
                throw new CustomException(AuthErrorCode.OAUTH_EXCHANGE_FAILED);
            }

            String name = account.profile() != null ? account.profile().nickname() : null;
            return new OAuthUserInfo(String.valueOf(response.id()), account.email(), name);
        } catch (RestClientException e) {
            log.warn("[OAUTH] 카카오 사용자 정보 조회 실패 - {}", e.getMessage());
            throw new CustomException(AuthErrorCode.OAUTH_EXCHANGE_FAILED);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    private record KakaoTokenResponse(String accessToken) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    private record KakaoUserInfoResponse(Long id, KakaoAccount kakaoAccount) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        private record KakaoAccount(
                String email,
                Boolean isEmailValid,
                Boolean isEmailVerified,
                Boolean emailNeedsAgreement,
                Profile profile) {

            @JsonIgnoreProperties(ignoreUnknown = true)
            private record Profile(String nickname) {}
        }
    }
}
