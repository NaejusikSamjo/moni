package com.moni.portfolio.application.service;

import com.moni.common.error.exception.CustomException;
import com.moni.portfolio.domain.entity.Portfolio;
import com.moni.portfolio.domain.exception.PortfolioErrorCode;
import com.moni.portfolio.domain.repository.PortfolioRepository;
import com.moni.portfolio.presentation.dto.response.PortfolioCreateResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;

    /** 포트폴리오 생성 로직 */
    @Transactional
    public PortfolioCreateResponseDto createPortfolio(UUID userId) {
        if (portfolioRepository.existsByUserId(userId)) {
            throw new CustomException(PortfolioErrorCode.PORTFOLIO_ALREADY_EXISTS);
        }

        Portfolio portfolio = Portfolio.create(userId);

        return PortfolioCreateResponseDto.from(portfolioRepository.save(portfolio));
    }
}
