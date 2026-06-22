package com.moni.user.user.application.service;

import com.moni.common.error.CommonErrorCode;
import com.moni.common.error.exception.CustomException;
import com.moni.common.security.SecurityUtil;
import com.moni.user.auth.application.service.TokenService;

import com.moni.user.user.domain.entity.Interest;
import com.moni.user.user.domain.entity.Tendency;
import com.moni.user.user.domain.entity.User;
import com.moni.user.user.domain.entity.Watchlist;
import com.moni.user.user.domain.exception.UserErrorCode;
import com.moni.user.user.domain.repository.InterestRepository;
import com.moni.user.user.domain.repository.TendencyRepository;
import com.moni.user.user.domain.repository.UserRepository;
import com.moni.user.user.domain.repository.WatchlistRepository;
import com.moni.user.user.presentation.dto.request.InterestRequest;
import com.moni.user.user.presentation.dto.request.TendencyRequest;
import com.moni.user.user.presentation.dto.request.UserUpdateRequest;
import com.moni.user.user.presentation.dto.response.InterestResponse;
import com.moni.user.user.presentation.dto.response.TendencyResponse;
import com.moni.user.user.presentation.dto.response.UserResponse;
import com.moni.user.user.presentation.dto.response.WatchlistResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TendencyRepository tendencyRepository;
    private final InterestRepository interestRepository;
    private final WatchlistRepository watchlistRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    // 내 정보 조회/수정
    @Transactional(readOnly = true)
    public UserResponse getMe(UUID userId) {
        return UserResponse.from(getUserById(userId));
    }

    @Transactional
    public UserResponse updateMe(UUID userId, UserUpdateRequest request) {
        validateOwnership(userId);
        User user = getUserById(userId);

        if (request.getNickname() != null) {
            if (userRepository.existsByNickname(request.getNickname())) {
                throw new CustomException(UserErrorCode.NICKNAME_DUPLICATE);
            }
        }

        if (request.getPassword() != null) {
            if (passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new CustomException(UserErrorCode.PASSWORD_SAME_AS_CURRENT);
            }
            user.updatePassword(passwordEncoder.encode(request.getPassword()));
        }

        user.updateProfile(request.getName(), request.getNickname(), request.getPhone());

        return UserResponse.from(user);
    }

    @Transactional
    public void withdraw(UUID userId, String deletedBy) {
        validateOwnership(userId);
        User user = getUserById(userId);
        user.withdraw("본인 요청", deletedBy);
        tokenService.deleteRefreshToken(userId);
        log.info("[USER] 회원 탈퇴 완료 - userId={}", userId);
    }

    // 투자 성향
    @Transactional
    public TendencyResponse createTendency(UUID userId, TendencyRequest request) {
        validateOwnership(userId);

        if (tendencyRepository.existsByUserIdAndDeletedAtIsNull(userId)) {
            throw new CustomException(UserErrorCode.TENDENCY_ALREADY_EXISTS);
        }

        User user = getUserById(userId);
        Tendency tendency = Tendency.create(user, request.getScore());
        return TendencyResponse.from(tendencyRepository.save(tendency));
    }

    @Transactional(readOnly = true)
    public TendencyResponse getTendency(UUID userId) {
        validateOwnership(userId);
        Tendency tendency = tendencyRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.TENDENCY_NOT_FOUND));
        return TendencyResponse.from(tendency);
    }

    @Transactional
    public TendencyResponse updateTendency(UUID userId, TendencyRequest request) {
        validateOwnership(userId);
        Tendency tendency = tendencyRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.TENDENCY_NOT_FOUND));
        tendency.update(request.getScore());
        return TendencyResponse.from(tendency);
    }

    // 관심사
    @Transactional
    public List<InterestResponse> createInterests(UUID userId, InterestRequest request) {
        validateOwnership(userId);
        User user = getUserById(userId);

        List<Interest> interests = request.getCategories().stream()
                .map(category -> Interest.create(user, category))
                .toList();

        return InterestResponse.fromList(interestRepository.saveAll(interests));
    }

    @Transactional(readOnly = true)
    public List<InterestResponse> getInterests(UUID userId) {
        validateOwnership(userId);
        return InterestResponse.fromList(interestRepository.findAllByUserIdAndDeletedAtIsNull(userId));
    }

    @Transactional
    public List<InterestResponse> updateInterests(UUID userId, InterestRequest request) {
        validateOwnership(userId);
        User user = getUserById(userId);

        interestRepository.deleteAllByUserId(userId);

        List<Interest> interests = request.getCategories().stream()
                .map(category -> Interest.create(user, category))
                .toList();

        return InterestResponse.fromList(interestRepository.saveAll(interests));
    }

    // 관심종목
    @Transactional
    public WatchlistResponse addWatchlist(UUID userId, String stockCode) {
        validateOwnership(userId);

        if (watchlistRepository.existsByUserIdAndStockCodeAndDeletedAtIsNull(userId, stockCode)) {
            throw new CustomException(UserErrorCode.WATCHLIST_ALREADY_EXISTS);
        }

        User user = getUserById(userId);
        Watchlist watchlist = Watchlist.create(user, stockCode);
        return WatchlistResponse.from(watchlistRepository.save(watchlist));
    }

    @Transactional(readOnly = true)
    public List<WatchlistResponse> getWatchlist(UUID userId) {
        validateOwnership(userId);
        return WatchlistResponse.fromList(watchlistRepository.findAllByUserIdAndDeletedAtIsNull(userId));
    }

    @Transactional
    public void removeWatchlist(UUID userId, String stockCode) {
        validateOwnership(userId);
        Watchlist watchlist = watchlistRepository.findByUserIdAndStockCodeAndDeletedAtIsNull(userId, stockCode)
                .orElseThrow(() -> new CustomException(UserErrorCode.WATCHLIST_NOT_FOUND));
        watchlist.delete(userId.toString());
    }

    // 공통 유틸
    private User getUserById(UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
    }

    private void validateOwnership(UUID resourceUserId) {
        UUID currentUserId = SecurityUtil.getCurrentUserId()
                .orElseThrow(() -> new CustomException(CommonErrorCode.UNAUTHORIZED));
        boolean isAdmin = SecurityUtil.getCurrentUserRole().map("ADMIN"::equals).orElse(false);
        if (!isAdmin && !resourceUserId.equals(currentUserId)) {
            throw new CustomException(UserErrorCode.FORBIDDEN);
        }
    }
}
