package com.moni.user.admin.application.service;

import com.moni.common.error.exception.CustomException;
import com.moni.user.admin.presentation.dto.response.AdminUserResponse;
import com.moni.user.admin.presentation.dto.response.DeletedUserResponse;
import com.moni.user.user.domain.entity.User;
import com.moni.user.user.domain.enums.UserStatus;
import com.moni.user.user.domain.exception.UserErrorCode;
import com.moni.user.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> getUsers(Pageable pageable) {
        return userRepository.findAllByDeletedAtIsNull(pageable).map(AdminUserResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<DeletedUserResponse> getDeletedUsers(Pageable pageable) {
        return userRepository.findAllByDeletedAtIsNotNull(pageable).map(DeletedUserResponse::from);
    }

    @Transactional
    public void suspendUser(UUID userId, String reason) {
        User user = getUserById(userId);
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new CustomException(UserErrorCode.USER_ALREADY_SUSPENDED);
        }
        user.suspend(reason);
    }

    @Transactional
    public void unsuspendUser(UUID userId) {
        User user = getUserById(userId);
        if (user.getStatus() != UserStatus.SUSPENDED) {
            throw new CustomException(UserErrorCode.USER_NOT_SUSPENDED);
        }
        user.unsuspend();
    }

    @Transactional
    public void deleteUser(UUID userId, String deletedBy) {
        User user = getUserById(userId);
        user.withdraw("관리자 삭제", deletedBy);
    }

    private User getUserById(UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
    }
}
