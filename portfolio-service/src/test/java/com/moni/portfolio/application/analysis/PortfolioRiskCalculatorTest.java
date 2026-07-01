package com.moni.portfolio.application.analysis;

import com.moni.portfolio.infrastructure.client.dto.response.UserTendencyResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PortfolioRiskCalculator 테스트")
class PortfolioRiskCalculatorTest {

    private final PortfolioRiskCalculator portfolioRiskCalculator = new PortfolioRiskCalculator();

    @Nested
    @DisplayName("calculate()")
    class Calculate {

        @Test
        @DisplayName("성공 - 사용자 성향과 포트폴리오 위험 점수로 적합도를 계산한다")
        void success_calculate_suitability() {
            // given
            UserTendencyResponseDto userTendency = new UserTendencyResponseDto(
                    UUID.fromString("00000000-0000-0000-0000-000000000001"),
                    33,
                    "STABLE"
            );

            // when
            TendencySuitabilityResult result = portfolioRiskCalculator.calculate(
                    userTendency,
                    new BigDecimal("10000000.00"),
                    new BigDecimal("4000000.00"),
                    new BigDecimal("60.00"),
                    new BigDecimal("100.00"),
                    2
            );

            // then
            assertThat(result.userTendencyType()).isEqualTo(TendencyType.STABLE);
            assertThat(result.userTendencyScore()).isEqualTo(33);
            assertThat(result.portfolioRiskType()).isEqualTo(TendencyType.ACTIVE);
            assertThat(result.portfolioRiskScore()).isEqualTo(70);
            assertThat(result.suitabilityScore()).isEqualTo(63);
            assertThat(result.suitabilityLevel()).isEqualTo("보통");
        }

        @Test
        @DisplayName("성공 - 비중이 100을 초과하면 100으로 보정해 계산한다")
        void success_clamp_percent() {
            // given
            UserTendencyResponseDto userTendency = new UserTendencyResponseDto(
                    UUID.fromString("00000000-0000-0000-0000-000000000002"),
                    90,
                    "AGGRESSIVE"
            );

            // when
            TendencySuitabilityResult result = portfolioRiskCalculator.calculate(
                    userTendency,
                    new BigDecimal("10000000.00"),
                    new BigDecimal("12000000.00"),
                    new BigDecimal("150.00"),
                    new BigDecimal("130.00"),
                    1
            );

            // then
            assertThat(result.portfolioRiskScore()).isEqualTo(100);
            assertThat(result.portfolioRiskType()).isEqualTo(TendencyType.AGGRESSIVE);
            assertThat(result.suitabilityScore()).isEqualTo(90);
            assertThat(result.suitabilityLevel()).isEqualTo("적합");
        }
    }
}
