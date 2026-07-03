package com.moni.trade.asset.application.service;

import com.moni.common.error.exception.CustomException;
import com.moni.trade.account.domain.entity.Account;
import com.moni.trade.account.domain.exception.AccountErrorCode;
import com.moni.trade.account.domain.repository.AccountRepository;
import com.moni.trade.asset.application.calculator.AssetCalculator;
import com.moni.trade.asset.application.calculator.model.AssetResult;
import com.moni.trade.asset.application.calculator.model.HoldingInput;
import com.moni.trade.asset.application.calculator.model.HoldingResult;
import com.moni.trade.asset.application.calculator.model.PriceInput;
import com.moni.trade.asset.domain.exception.AssetErrorCode;
import com.moni.trade.asset.presentation.dto.response.AssetHoldingResponseDto;
import com.moni.trade.asset.presentation.dto.response.AssetHoldingsResponseDto;
import com.moni.trade.asset.presentation.dto.response.AssetResponseDto;
import com.moni.trade.holding.domain.entity.Holding;
import com.moni.trade.holding.domain.repository.HoldingRepository;
import com.moni.trade.trade.infrastructure.client.StockServiceClient;
import com.moni.trade.trade.infrastructure.client.dto.ExternalApiResponseDto;
import com.moni.trade.trade.infrastructure.client.dto.StockPriceResponseDto;
import feign.FeignException;
import feign.RetryableException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssetService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 50;
    private static final BigDecimal INITIAL_PRINCIPAL_AMOUNT = new BigDecimal("10000000");
    private static final String DEFAULT_SORT = "evaluationAmount,desc";
    private static final String EVALUATION_AMOUNT_ASC = "evaluationAmount,asc";

    private final AccountRepository accountRepository;
    private final HoldingRepository holdingRepository;
    private final AssetCalculator assetCalculator;
    private final StockServiceClient stockServiceClient;

    /** 자산 조회 로직 */
    public AssetResponseDto getAssets(UUID userId) {
        Account account = getAccount(userId);
        BigDecimal cashBalance = account.getBalance();
        List<HoldingInput> holdings = getHoldings(account.getId());
        List<PriceInput> prices = getPrices(holdings);
        AssetResult result = assetCalculator.calculateAssets(
                cashBalance,
                INITIAL_PRINCIPAL_AMOUNT,
                holdings,
                prices
        );

        return AssetResponseDto.from(result);
    }

    /** 보유 종목 현황 조회 로직 */
    public AssetHoldingsResponseDto getHoldings(UUID userId, int page, int size, String sort) {
        int resolvedPage = resolvePage(page);
        int resolvedSize = resolveSize(size);
        String resolvedSort = resolveSort(sort);

        // Trade 보유 종목 전체를 조회한 뒤 각 ticker의 현재가를 Stock에서 조회
        Account account = getAccount(userId);
        List<HoldingInput> holdings = getHoldings(account.getId());
        List<PriceInput> prices = getPrices(holdings);

        // 평가금액, 평가손익, 수익률과 비중은 페이지를 나누기 전에 전체 종목 기준으로 계산
        List<HoldingResult> holdingResults = assetCalculator.calculateHoldings(holdings, prices);

        // 계산 결과의 평가금액을 기준으로 오름차순 또는 기본 내림차순 정렬
        Comparator<HoldingResult> comparator = Comparator.comparing(HoldingResult::evaluationAmount);
        if (DEFAULT_SORT.equals(resolvedSort)) {
            comparator = comparator.reversed();
        }

        List<HoldingResult> sortedHoldings = holdingResults.stream()
                .sorted(comparator)
                .toList();

        long totalElements = sortedHoldings.size();
        int totalPages = calculateTotalPages(totalElements, resolvedSize);
        long fromIndex = (long) resolvedPage * resolvedSize;

        // 전체 결과의 정렬이 끝난 뒤 요청한 페이지 범위만 응답 DTO로 변환
        List<AssetHoldingResponseDto> content;
        if (fromIndex >= totalElements) {
            content = List.of();
        } else {
            int from = (int) fromIndex;
            int to = Math.min(from + resolvedSize, sortedHoldings.size());
            content = sortedHoldings.subList(from, to).stream()
                    .map(AssetHoldingResponseDto::from)
                    .toList();
        }

        return new AssetHoldingsResponseDto(
                content,
                resolvedPage,
                resolvedSize,
                totalElements,
                totalPages,
                resolvedSort
        );
    }

    private Account getAccount(UUID userId) {
        return accountRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(AccountErrorCode.ACCOUNT_NOT_FOUND));
    }

    private List<HoldingInput> getHoldings(UUID accountId) {
        Page<Holding> firstPage = getHoldingPage(accountId, DEFAULT_PAGE);
        List<Holding> tradeHoldings = new ArrayList<>(firstPage.getContent());

        for (int page = 1; page < firstPage.getTotalPages(); page++) {
            Page<Holding> nextPage = getHoldingPage(accountId, page);
            tradeHoldings.addAll(nextPage.getContent());
        }

        return tradeHoldings.stream()
                .map(this::toHoldingInput)
                .toList();
    }

    private List<PriceInput> getPrices(List<HoldingInput> holdings) {
        return holdings.stream()
                .map(holding -> {
                    StockPriceResponseDto stock = requestStockData(
                            () -> stockServiceClient.getStock(holding.ticker())
                    );

                    if (stock.ticker() == null || !holding.ticker().equals(stock.ticker())) {
                        throw new CustomException(AssetErrorCode.STOCK_RESPONSE_INVALID);
                    }
                    if (stock.price() == null || stock.price().compareTo(BigDecimal.ZERO) <= 0) {
                        throw new CustomException(AssetErrorCode.STOCK_PRICE_NOT_FOUND);
                    }
                    if (stock.name() == null || stock.name().isBlank()) {
                        throw new CustomException(AssetErrorCode.STOCK_RESPONSE_INVALID);
                    }

                    return new PriceInput(stock.ticker(), stock.name(), stock.price());
                })
                .toList();
    }

    private Page<Holding> getHoldingPage(UUID accountId, int page) {
        PageRequest pageable = PageRequest.of(page, DEFAULT_SIZE, Sort.by("createdAt").descending());
        return holdingRepository.findByAccountId(accountId, pageable);
    }

    private HoldingInput toHoldingInput(Holding holding) {
        return new HoldingInput(
                holding.getTicker(),
                holding.getQuantity().longValue(),
                holding.getAveragePrice(),
                holding.getTotalAmount()
        );
    }

    private <T> T requestStockData(Supplier<ExternalApiResponseDto<T>> request) {
        try {
            ExternalApiResponseDto<T> response = request.get();
            if (response == null || response.data() == null) {
                throw new CustomException(AssetErrorCode.STOCK_RESPONSE_INVALID);
            }
            return response.data();
        } catch (RetryableException exception) {
            throw new CustomException(AssetErrorCode.STOCK_SERVICE_TIMEOUT);
        } catch (FeignException exception) {
            if (exception.status() == HttpStatus.NOT_FOUND.value()) {
                throw new CustomException(AssetErrorCode.STOCK_PRICE_NOT_FOUND);
            }
            throw new CustomException(AssetErrorCode.STOCK_SERVICE_ERROR);
        }
    }

    /** page는 음수일 경우 기본 페이지로 보정 */
    private int resolvePage(int page) {
        if (page < 0) {
            return DEFAULT_PAGE;
        }
        return page;
    }

    /** size는 1 미만 또는 최대 허용값 초과 시 기본 크기로 보정 */
    private int resolveSize(int size) {
        if (size < 1 || size > MAX_SIZE) {
            return DEFAULT_SIZE;
        }
        return size;
    }

    /** 지원하지 않는 정렬 조건은 기본 정렬로 보정 */
    private String resolveSort(String sort) {
        if (EVALUATION_AMOUNT_ASC.equalsIgnoreCase(sort)) {
            return EVALUATION_AMOUNT_ASC;
        }
        return DEFAULT_SORT;
    }

    private int calculateTotalPages(long totalElements, int size) {
        if (totalElements == 0) {
            return 0;
        }
        return (int) ((totalElements + size - 1) / size);
    }

}
