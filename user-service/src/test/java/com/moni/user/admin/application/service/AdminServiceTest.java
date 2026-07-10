package com.moni.user.admin.application.service;

import com.moni.common.error.exception.CustomException;
import com.moni.user.admin.presentation.dto.response.AdminUserResponse;
import com.moni.user.user.domain.entity.User;
import com.moni.user.user.domain.enums.UserRole;
import com.moni.user.user.domain.enums.UserStatus;
import com.moni.user.user.domain.exception.UserErrorCode;
import com.moni.user.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService 단위 테스트 (목업)")
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminService adminService;

    private User mockUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        mockUser = User.create("user@moni.com", "encodedPassword", "홍길동", "길동#a1b2c3", "010-1234-5678");
        userId = mockUser.getId();
    }

    @Nested
    @DisplayName("유저 목록 조회")
    class GetUsers {

        @Test
        @DisplayName("삭제되지 않은 유저 목록을 페이지로 반환한다")
        void getUsers_success() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            Page<User> userPage = new PageImpl<>(List.of(mockUser), pageable, 1);
            given(userRepository.findAllByDeletedAtIsNull(pageable)).willReturn(userPage);

            // when
            Page<AdminUserResponse> result = adminService.getUsers(pageable);

            // then
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).email()).isEqualTo("user@moni.com");
            assertThat(result.getContent().get(0).role()).isEqualTo(UserRole.USER);
        }
    }

    @Nested
    @DisplayName("유저 계정 정지")
    class SuspendUser {

        @Test
        @DisplayName("정상 정지 시 유저 상태가 SUSPENDED로 변경된다")
        void suspendUser_success() {
            // given
            given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(mockUser));

            // when
            adminService.suspendUser(userId, "서비스 이용약관 위반");

            // then
            assertThat(mockUser.getStatus()).isEqualTo(UserStatus.SUSPENDED);
            assertThat(mockUser.getSuspendedReason()).isEqualTo("서비스 이용약관 위반");
        }

        @Test
        @DisplayName("이미 정지된 유저를 정지하면 예외를 던진다")
        void suspendUser_fail_alreadySuspended() {
            // given
            mockUser.suspend("기존 사유");
            given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(mockUser));

            // when & then
            assertThatThrownBy(() -> adminService.suspendUser(userId, "새 사유"))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_ALREADY_SUSPENDED);
        }

        @Test
        @DisplayName("존재하지 않는 유저를 정지하면 예외를 던진다")
        void suspendUser_fail_userNotFound() {
            // given
            given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adminService.suspendUser(userId, "사유"))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("유저 계정 정지 해지")
    class UnsuspendUser {

        @Test
        @DisplayName("정상 해지 시 유저 상태가 ACTIVE로 변경된다")
        void unsuspendUser_success() {
            // given
            mockUser.suspend("규정 위반");
            given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(mockUser));

            // when
            adminService.unsuspendUser(userId);

            // then
            assertThat(mockUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(mockUser.getSuspendedReason()).isNull();
        }

        @Test
        @DisplayName("정지 상태가 아닌 유저를 해지하면 예외를 던진다")
        void unsuspendUser_fail_notSuspended() {
            // given
            given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(mockUser));

            // when & then
            assertThatThrownBy(() -> adminService.unsuspendUser(userId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_SUSPENDED);
        }

        @Test
        @DisplayName("존재하지 않는 유저를 해지하면 예외를 던진다")
        void unsuspendUser_fail_userNotFound() {
            // given
            given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adminService.unsuspendUser(userId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("유저 계정 삭제 (소프트)")
    class DeleteUser {

        @Test
        @DisplayName("정상 삭제 시 유저 상태가 DELETED로 변경된다")
        void deleteUser_success() {
            // given
            given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(mockUser));

            // when
            adminService.deleteUser(userId, "admin");

            // then
            assertThat(mockUser.getStatus()).isEqualTo(UserStatus.DELETED);
        }

        @Test
        @DisplayName("존재하지 않는 유저를 삭제하면 예외를 던진다")
        void deleteUser_fail_userNotFound() {
            // given
            given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adminService.deleteUser(userId, "admin"))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);
        }
    }

}
