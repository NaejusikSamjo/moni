package com.moni.portfolio.application.policy;

import com.moni.common.error.exception.CustomException;
import com.moni.portfolio.domain.entity.Portfolio;
import com.moni.portfolio.domain.exception.PortfolioErrorCode;
import com.moni.portfolio.domain.repository.PortfolioAnalysisRepository;
import com.moni.portfolio.infrastructure.client.PaymentServiceClient;
import com.moni.portfolio.infrastructure.client.dto.response.ExternalApiResponseDto;
import com.moni.portfolio.infrastructure.client.dto.response.SubscriptionStatusResponseDto;
import feign.FeignException;
import feign.RetryableException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PortfolioAnalysisPolicyService {

    private static final long FREE_PLAN_AI_ANALYSIS_LIMIT = 5L;
    private static final ZoneId ANALYSIS_DAILY_LIMIT_ZONE = ZoneId.of("Asia/Seoul");

    private final PortfolioAnalysisRepository portfolioAnalysisRepository;
    private final PaymentServiceClient paymentServiceClient;

    public void validateRequest(UUID userId, Portfolio portfolio) {
        validateDailyAnalysisLimit(portfolio);
        if (!isPaidPlan(userId) && portfolio.getAiAnalysisCount() >= FREE_PLAN_AI_ANALYSIS_LIMIT) {
            throw new CustomException(PortfolioErrorCode.PORTFOLIO_ANALYSIS_FREE_LIMIT_EXCEEDED);
        }
    }

    private void validateDailyAnalysisLimit(Portfolio portfolio) {
        LocalDate today = LocalDate.now(ANALYSIS_DAILY_LIMIT_ZONE);
        LocalDateTime startDateTime = today.atStartOfDay();
        LocalDateTime endDateTime = today.plusDays(1).atStartOfDay();
        if (portfolioAnalysisRepository.existsByPortfolioIdAndUpdatedAtBetween(
                portfolio.getId(),
                startDateTime,
                endDateTime
        )) {
            throw new CustomException(PortfolioErrorCode.PORTFOLIO_ANALYSIS_DAILY_LIMIT_EXCEEDED);
        }
    }

    private boolean isPaidPlan(UUID userId) {
        try {
            ExternalApiResponseDto<SubscriptionStatusResponseDto> response =
                    paymentServiceClient.getSubscriptionStatus(userId);
            if (response == null || response.data() == null) {
                throw new CustomException(PortfolioErrorCode.PAYMENT_RESPONSE_INVALID);
            }
            return response.data().isPaidPlan();
        } catch (RetryableException exception) {
            throw new CustomException(PortfolioErrorCode.PAYMENT_SERVICE_TIMEOUT);
        } catch (FeignException exception) {
            throw new CustomException(PortfolioErrorCode.PAYMENT_SERVICE_ERROR);
        }
    }
}
