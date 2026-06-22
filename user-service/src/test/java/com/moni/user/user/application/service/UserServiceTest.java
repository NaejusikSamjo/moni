package com.moni.user.user.application.service;

import com.moni.common.error.exception.CustomException;
import com.moni.common.security.SecurityUtil;
import com.moni.user.auth.application.service.TokenService;
import com.moni.user.user.domain.entity.Tendency;
import com.moni.user.user.domain.entity.User;
import com.moni.user.user.domain.entity.Watchlist;
import com.moni.user.user.domain.enums.TendencyType;
import com.moni.user.user.domain.exception.UserErrorCode;
import com.moni.user.user.domain.repository.InterestRepository;
import com.moni.user.user.domain.repository.TendencyRepository;
import com.moni.user.user.domain.repository.UserRepository;
import com.moni.user.user.domain.repository.WatchlistRepository;
import com.moni.user.user.presentation.dto.request.TendencyRequest;
import com.moni.user.user.presentation.dto.request.UserUpdateRequest;
import com.moni.user.user.presentation.dto.response.TendencyResponse;
import com.moni.user.user.presentation.dto.response.UserResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 단위 테스트 (목업)")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TendencyRepository tendencyRepository;

    @Mock
    private InterestRepository interestRepository;

    @Mock
    private WatchlistRepository watchlistRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private UserService userService;

    private User mockUser;
    private UUID userId;
    private MockedStatic<SecurityUtil> securityUtilMock;

    @BeforeEach
    void setUp() {
        mockUser = User.create("test@moni.com", "encodedPassword", "홍길동", "귀여운 주니어#a1b2c3", "010-1234-5678");
        userId = mockUser.getId();

        // SecurityUtil은 정적 메서드라 목업 처리 (SecurityContext 없이 단위 테스트)
        securityUtilMock = mockStatic(SecurityUtil.class);
        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(Optional.of(userId));
        securityUtilMock.when(SecurityUtil::getCurrentUserRole).thenReturn(Optional.of("USER"));
    }

    @AfterEach
    void tearDown() {
        securityUtilMock.close();
    }

    @Nested
    @DisplayName("내 정보 조회")
    class GetMe {

        @Test
        @DisplayName("존재하는 유저면 UserResponse를 반환한다")
        void getMe_success() {
            // given
            given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(mockUser));

            // when
            UserResponse response = userService.getMe(userId);

            // then
            assertThat(response.getEmail()).isEqualTo("test@moni.com");
            assertThat(response.getNickname()).isEqualTo("귀여운 주니어#a1b2c3");
        }

        @Test
        @DisplayName("존재하지 않는 유저면 예외를 던진다")
        void getMe_fail_notFound() {
            // given
            given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.getMe(userId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("내 정보 수정")
    class UpdateMe {

        @Test
        @DisplayName("타인의 정보를 수정하려 하면 권한 예외를 던진다")
        void updateMe_fail_forbidden() {
            // given
            UUID otherUserId = UUID.randomUUID();
            UserUpdateRequest request = new UserUpdateRequest();

            // when & then
            assertThatThrownBy(() -> userService.updateMe(otherUserId, request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("투자 성향")
    class TendencyTest {

        @Test
        @DisplayName("정상 등록 시 TendencyResponse를 반환한다")
        void createTendency_success() {
            // given
            TendencyRequest request = new TendencyRequest();
            ReflectionTestUtils.setField(request, "score", 85);
            given(tendencyRepository.existsByUserIdAndDeletedAtIsNull(userId)).willReturn(false);
            given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(mockUser));
            given(tendencyRepository.save(any(Tendency.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            TendencyResponse response = userService.createTendency(userId, request);

            // then
            assertThat(response.getScore()).isEqualTo(85);
            assertThat(response.getType()).isEqualTo(TendencyType.AGGRESSIVE);
        }

        @Test
        @DisplayName("이미 등록된 성향이 있으면 예외를 던진다")
        void createTendency_fail_alreadyExists() {
            // given
            TendencyRequest request = new TendencyRequest();
            ReflectionTestUtils.setField(request, "score", 80);
            given(tendencyRepository.existsByUserIdAndDeletedAtIsNull(userId)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.createTendency(userId, request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.TENDENCY_ALREADY_EXISTS);
        }
    }

    @Nested
    @DisplayName("관심종목")
    class WatchlistTest {

        @Test
        @DisplayName("이미 추가된 종목이면 예외를 던진다")
        void addWatchlist_fail_alreadyExists() {
            // given
            given(watchlistRepository.existsByUserIdAndStockCodeAndDeletedAtIsNull(userId, "005930"))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.addWatchlist(userId, "005930"))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.WATCHLIST_ALREADY_EXISTS);
        }

        @Test
        @DisplayName("정상 삭제 요청 시 soft delete 처리한다")
        void removeWatchlist_success() {
            // given
            Watchlist watchlist = Watchlist.create(mockUser, "005930");
            given(watchlistRepository.findByUserIdAndStockCodeAndDeletedAtIsNull(userId, "005930"))
                    .willReturn(Optional.of(watchlist));

            // when
            userService.removeWatchlist(userId, "005930");

            // then
            assertThat(watchlist.getDeletedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("회원 탈퇴")
    class Withdraw {

        @Test
        @DisplayName("정상 탈퇴 시 리프레시 토큰을 삭제한다")
        void withdraw_success() {
            // given
            given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(mockUser));

            // when
            userService.withdraw(userId, userId.toString());

            // then
            verify(tokenService).deleteRefreshToken(userId);
        }
    }
}
