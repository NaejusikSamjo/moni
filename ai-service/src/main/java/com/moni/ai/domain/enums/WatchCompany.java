package com.moni.ai.domain.enums;

import com.moni.ai.common.exception.AiErrorCode;
import com.moni.common.error.exception.CustomException;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public enum WatchCompany {

    SAMSUNG("005930", "삼성전자"),
    SK_HYNIX("000660", "SK하이닉스"),
    HYUNDAI("005380", "현대차");

    private final String ticker;
    private final String companyName;

    WatchCompany(String ticker, String companyName) {
        this.ticker = ticker;
        this.companyName = companyName;
    }

    public static Map<String, String> toMap() {
        return Arrays.stream(values())
                .collect(Collectors.toMap(
                        WatchCompany::getTicker,
                        WatchCompany::getCompanyName
                ));
    }

    public static WatchCompany fromTicker(String ticker) {
        return Arrays.stream(values())
                .filter(c -> c.getTicker().equals(ticker))
                .findFirst()
                .orElseThrow(() -> new CustomException(AiErrorCode.TICKER_NOT_FOUND));
    }

}
