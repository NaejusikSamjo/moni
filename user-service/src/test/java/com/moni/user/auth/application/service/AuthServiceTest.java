package com.moni.user.auth.application.service;

import com.moni.common.error.exception.CustomException;
import com.moni.user.auth.domain.exception.AuthErrorCode;
import com.moni.user.auth.presentation.dto.request.LoginRequest;
import com.moni.user.auth.presentation.dto.request.SignupRequest;
import com.moni.user.auth.presentation.dto.response.LoginResponse;
import com.moni.user.auth.presentation.dto.response.SignupResponse;
import com.moni.user.user.domain.entity.User;
import com.moni.user.user.domain.enums.UserStatus;
import com.moni.user.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 단위 테스트 (목업)")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private AuthService authService;

    private SignupRequest signupRequest;
    private LoginRequest loginRequest;
    private User mockUser;

    @BeforeEach
    void setUp() {
        signupRequest = new SignupRequest();
        ReflectionTestUtils.setField(signupRequest, "email", "test@moni.com");
        ReflectionTestUtils.setField(signupRequest, "password", "Test1234!");
        ReflectionTestUtils.setField(signupRequest, "name", "홍길동");
        ReflectionTestUtils.setField(signupRequest, "phone", "010-1234-5678");

        loginRequest = new LoginRequest();
        ReflectionTestUtils.setField(loginRequest, "email", "test@moni.com");
        ReflectionTestUtils.setField(loginRequest, "password", "Test1234!");

        mockUser = User.create("test@moni.com", "encodedPassword", "홍길동", "귀여운 주니어#a1b2c3", "010-1234-5678");
    }

    @Nested
    @DisplayName("회원가입")
    class Signup {

        @Test
        @DisplayName("정상 회원가입 시 SignupResponse를 반환한다")
        void signup_success() {
            // given
            given(userRepository.existsByEmail(signupRequest.getEmail())).willReturn(false);
            given(passwordEncoder.encode(signupRequest.getPassword())).willReturn("encodedPassword");
            given(userRepository.save(any(User.class))).willReturn(mockUser);

            // when
            SignupResponse response = authService.signup(signupRequest, "귀여운 주니어#a1b2c3");

            // then
            assertThat(response.getEmail()).isEqualTo("test@moni.com");
            assertThat(response.getNickname()).isEqualTo("귀여운 주니어#a1b2c3");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("이메일이 중복되면 예외를 던진다")
        void signup_fail_duplicateEmail() {
            // given
            given(userRepository.existsByEmail(signupRequest.getEmail())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> authService.signup(signupRequest, "닉네임"))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.EMAIL_DUPLICATE);

            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("로그인")
    class Login {

        @Test
        @DisplayName("정상 로그인 시 토큰을 발급한다")
        void login_success() {
            // given
            given(userRepository.findByEmail(loginRequest.getEmail())).willReturn(Optional.of(mockUser));
            given(passwordEncoder.matches(loginRequest.getPassword(), mockUser.getPassword())).willReturn(true);
            given(tokenService.issueTokens(mockUser)).willReturn(new LoginResponse("access-token", "refresh-token"));

            // when
            LoginResponse response = authService.login(loginRequest);

            // then
            assertThat(response.getAccessToken()).isEqualTo("access-token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        }

        @Test
        @DisplayName("존재하지 않는 이메일이면 LOGIN_FAILED 예외를 던진다")
        void login_fail_userNotFound() {
            // given
            given(userRepository.findByEmail(loginRequest.getEmail())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.LOGIN_FAILED);
        }

        @Test
        @DisplayName("비밀번호가 일치하지 않으면 LOGIN_FAILED 예외를 던진다")
        void login_fail_invalidPassword() {
            // given
            given(userRepository.findByEmail(loginRequest.getEmail())).willReturn(Optional.of(mockUser));
            given(passwordEncoder.matches(loginRequest.getPassword(), mockUser.getPassword())).willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.LOGIN_FAILED);
        }

        @Test
        @DisplayName("정지된 계정이면 예외를 던진다")
        void login_fail_suspended() throws Exception {
            // given
            setStatus(mockUser, UserStatus.SUSPENDED);
            given(userRepository.findByEmail(loginRequest.getEmail())).willReturn(Optional.of(mockUser));

            // when & then
            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.USER_SUSPENDED);
        }
    }

    @Nested
    @DisplayName("토큰 갱신")
    class Refresh {

        @Test
        @DisplayName("정상적으로 토큰을 rotate한다")
        void refresh_success() {
            // given
            given(tokenService.getVerifiedUserId("old-refresh-token")).willReturn(mockUser.getId());
            given(userRepository.findById(mockUser.getId())).willReturn(Optional.of(mockUser));
            given(tokenService.rotateRefreshToken(mockUser, "old-refresh-token"))
                    .willReturn(new LoginResponse("new-access-token", "new-refresh-token"));

            // when
            LoginResponse response = authService.refresh("old-access-token", "old-refresh-token");

            // then
            assertThat(response.getAccessToken()).isEqualTo("new-access-token");
            assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
            verify(tokenService).blacklistAccessToken("old-access-token", "refresh");
        }

        @Test
        @DisplayName("유저를 찾을 수 없으면 예외를 던진다")
        void refresh_fail_userNotFound() {
            // given
            given(tokenService.getVerifiedUserId("old-refresh-token")).willReturn(mockUser.getId());
            given(userRepository.findById(mockUser.getId())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.refresh("old-access-token", "old-refresh-token"))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("로그아웃")
    class Logout {

        @Test
        @DisplayName("로그아웃 시 토큰 블랙리스트 등록 및 리프레시 토큰을 삭제한다")
        void logout_success() {
            // when
            authService.logout("access-token", "refresh-token");

            // then
            verify(tokenService).blacklistAccessToken("access-token", "logout");
            verify(tokenService).deleteRefreshTokenByToken("refresh-token");
        }
    }

    // 테스트 전용 리플렉션 헬퍼 (private 필드 상태값 강제 변경용 목업)
    private void setStatus(User user, UserStatus status) throws Exception {
        Field field = User.class.getDeclaredField("status");
        field.setAccessible(true);
        field.set(user, status);
    }
}