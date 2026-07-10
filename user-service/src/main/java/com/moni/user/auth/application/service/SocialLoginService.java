package com.moni.user.auth.application.service;

import com.moni.common.error.exception.CustomException;
import com.moni.user.auth.application.oauth.OAuthUserInfo;
import com.moni.user.auth.domain.exception.AuthErrorCode;
import com.moni.user.auth.application.oauth.OAuthClient;
import com.moni.user.auth.presentation.dto.response.LoginResponse;
import com.moni.user.global.util.NicknameGenerator;
import com.moni.user.user.domain.entity.User;
import com.moni.user.user.domain.enums.OAuthProvider;
import com.moni.user.user.domain.enums.UserStatus;
import com.moni.user.user.domain.repository.UserRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SocialLoginService {

    private final OAuthClient googleOAuthClient;
    private final OAuthClient kakaoOAuthClient;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final NicknameGenerator nicknameGenerator;

    public SocialLoginService(
            @Qualifier("googleOAuthClient") OAuthClient googleOAuthClient,
            @Qualifier("kakaoOAuthClient") OAuthClient kakaoOAuthClient,
            UserRepository userRepository,
            TokenService tokenService,
            NicknameGenerator nicknameGenerator) {
        this.googleOAuthClient = googleOAuthClient;
        this.kakaoOAuthClient = kakaoOAuthClient;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.nicknameGenerator = nicknameGenerator;
    }

    @Transactional
    public LoginResponse login(String providerName, String code, String codeVerifier) {
        OAuthProvider provider = parseProvider(providerName);
        OAuthUserInfo userInfo = resolveClient(provider).exchangeCodeForUser(code, codeVerifier);

        User user = userRepository.findByOauthProviderAndOauthId(provider, userInfo.oauthId())
                .orElseGet(() -> registerOAuthUser(userInfo, provider));

        validateStatus(user);
        return tokenService.issueTokens(user);
    }

    public String getLoginUrl(String providerName, String codeChallenge, String state) {
        OAuthProvider provider = parseProvider(providerName);
        return resolveClient(provider).buildLoginUrl(codeChallenge, state);
    }

    private User registerOAuthUser(OAuthUserInfo userInfo, OAuthProvider provider) {
        if (userRepository.existsByEmail(userInfo.email())) {
            throw new CustomException(AuthErrorCode.OAUTH_PROVIDER_MISMATCH);
        }
        String name = userInfo.name() != null ? userInfo.name() : "사용자";
        User user = User.createOAuth(
                userInfo.email(),
                name,
                nicknameGenerator.generate(),
                provider,
                userInfo.oauthId(),
                userInfo.phone()
        );
        return userRepository.save(user);
    }

    private void validateStatus(User user) {
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new CustomException(AuthErrorCode.USER_SUSPENDED);
        }
        if (user.getStatus() == UserStatus.DELETED) {
            throw new CustomException(AuthErrorCode.USER_DELETED);
        }
    }

    private OAuthProvider parseProvider(String providerName) {
        try {
            return OAuthProvider.valueOf(providerName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomException(AuthErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
        }
    }

    private OAuthClient resolveClient(OAuthProvider provider) {
        return switch (provider) {
            case GOOGLE -> googleOAuthClient;
            case KAKAO -> kakaoOAuthClient;
        };
    }
}
