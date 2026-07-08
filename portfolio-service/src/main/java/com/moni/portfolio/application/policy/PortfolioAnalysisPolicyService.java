package com.moni.portfolio.application.policy;

import com.moni.common.error.exception.CustomException;
import com.moni.portfolio.application.service.UserSubscriptionStatusQueryService;
import com.moni.portfolio.domain.entity.Portfolio;
import com.moni.portfolio.domain.enums.AnalysisStatus;
import com.moni.portfolio.domain.exception.PortfolioErrorCode;
import com.moni.portfolio.domain.repository.PortfolioAnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PortfolioAnalysisPolicyService {

    private static final long FREE_PLAN_AI_ANALYSIS_LIMIT = 5L;
    private static final ZoneId ANALYSIS_DAILY_LIMIT_ZONE = ZoneId.of("Asia/Seoul");
    private static final List<AnalysisStatus> DAILY_LIMIT_STATUSES = List.of(
            AnalysisStatus.PENDING,
            AnalysisStatus.SUCCESS
    );

    private final PortfolioAnalysisRepository portfolioAnalysisRepository;
    private final UserSubscriptionStatusQueryService userSubscriptionStatusQueryService;

    public void validateRequest(UUID userId, Portfolio portfolio) {
        validateDailyAnalysisLimit(portfolio);
        if (portfolio.getAiAnalysisCount() < FREE_PLAN_AI_ANALYSIS_LIMIT) {
            return;
        }

        if (!userSubscriptionStatusQueryService.isPaidPlan(userId)) {
            throw new CustomException(PortfolioErrorCode.PORTFOLIO_ANALYSIS_FREE_LIMIT_EXCEEDED);
        }
    }

    private void validateDailyAnalysisLimit(Portfolio portfolio) {
        LocalDate today = LocalDate.now(ANALYSIS_DAILY_LIMIT_ZONE);
        LocalDateTime startDateTime = today.atStartOfDay();
        LocalDateTime endDateTime = today.plusDays(1).atStartOfDay();
        if (portfolioAnalysisRepository.existsByPortfolioIdAndStatusInAndCreatedAtBetween(
                portfolio.getId(),
                DAILY_LIMIT_STATUSES,
                startDateTime,
                endDateTime
        )) {
            throw new CustomException(PortfolioErrorCode.PORTFOLIO_ANALYSIS_DAILY_LIMIT_EXCEEDED);
        }
    }
}
