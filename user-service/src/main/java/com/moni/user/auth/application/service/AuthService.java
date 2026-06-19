package com.moni.user.auth.application.service;

import com.moni.common.error.exception.CustomException;
import com.moni.user.auth.domain.exception.AuthErrorCode;
import com.moni.user.user.domain.repository.UserRepository;
import com.moni.user.auth.presentation.dto.request.LoginRequest;
import com.moni.user.auth.presentation.dto.request.SignupRequest;
import com.moni.user.auth.presentation.dto.response.LoginResponse;
import com.moni.user.auth.presentation.dto.response.SignupResponse;
import com.moni.user.user.domain.entity.User;
import com.moni.user.user.domain.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Transactional
    public SignupResponse signup(SignupRequest request, String generatedNickname) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(AuthErrorCode.EMAIL_DUPLICATE);
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = User.create(
                request.getEmail(),
                encodedPassword,
                request.getName(),
                generatedNickname,
                request.getPhone()
        );
        User savedUser = userRepository.save(user);

        return new SignupResponse(savedUser.getId(), savedUser.getEmail(), savedUser.getNickname());
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(AuthErrorCode.LOGIN_FAILED));

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new CustomException(AuthErrorCode.USER_SUSPENDED);
        }
        if (user.getStatus() == UserStatus.DELETED) {
            throw new CustomException(AuthErrorCode.USER_DELETED);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException(AuthErrorCode.LOGIN_FAILED);
        }

        return tokenService.issueTokens(user);
    }

    @Transactional
    public LoginResponse refresh(String accessToken, String refreshToken) {
        UUID userId = tokenService.validateAndGetUserId(refreshToken);
        tokenService.blacklistAccessToken(accessToken, "refresh");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

        return tokenService.issueTokens(user);
    }

    public void logout(String accessToken, String refreshToken) {
        tokenService.blacklistAccessToken(accessToken, "logout");
        tokenService.deleteRefreshTokenByToken(refreshToken);
    }
}