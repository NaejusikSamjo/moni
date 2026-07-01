package com.moni.portfolio.application.service;

import com.moni.common.error.exception.CustomException;
import com.moni.portfolio.domain.entity.Portfolio;
import com.moni.portfolio.domain.exception.PortfolioErrorCode;
import com.moni.portfolio.domain.repository.PortfolioRepository;
import com.moni.portfolio.infrastructure.client.StockServiceClient;
import com.moni.portfolio.presentation.dto.response.PortfolioCreateResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

@DisplayName("PortfolioService 테스트")
@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private StockServiceClient stockServiceClient;

    @InjectMocks
    private PortfolioService portfolioService;

    @Nested
    @DisplayName("createPortfolio()")
    class CreatePortfolio {

        @Test
        @DisplayName("성공 - 포트폴리오가 없으면 새로 생성한다")
        void success_create() {
            // given
            given(portfolioRepository.existsByUserId(USER_ID)).willReturn(false);
            given(portfolioRepository.save(any(Portfolio.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            PortfolioCreateResponseDto result = portfolioService.createPortfolio(USER_ID);

            // then
            assertThat(result.userId()).isEqualTo(USER_ID);
            then(portfolioRepository).should().save(any(Portfolio.class));
            verifyNoInteractions(stockServiceClient);
        }

        @Test
        @DisplayName("실패 - 포트폴리오가 이미 있으면 예외가 발생한다")
        void fail_portfolio_already_exists() {
            // given
            given(portfolioRepository.existsByUserId(USER_ID)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> portfolioService.createPortfolio(USER_ID))
                    .isInstanceOfSatisfying(CustomException.class, exception ->
                            assertThat(exception.getErrorCode())
                                    .isEqualTo(PortfolioErrorCode.PORTFOLIO_ALREADY_EXISTS));

            then(portfolioRepository).should().existsByUserId(USER_ID);
            then(portfolioRepository).should(never()).save(any(Portfolio.class));
            verifyNoInteractions(stockServiceClient);
        }
    }
}
