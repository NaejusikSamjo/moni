package com.moni.ai.domain.enums;

import com.moni.ai.domain.exception.AiErrorCode;
import com.moni.common.error.exception.CustomException;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public enum WatchCompany {
    SAMSUNG_ELECTRONICS("005930", "삼성전자"),
    SK_HYNIX("000660", "SK하이닉스"),
    SK_SQUARE("402340", "SK스퀘어"),
    SAMSUNG_ELECTRO_MECHANICS("009150", "삼성전기"),
    HYUNDAI_MOTOR("005380", "현대차"),
    LG_ENERGY_SOLUTION("373220", "LG에너지솔루션"),
    SAMSUNG_LIFE("032830", "삼성생명"),
    SAMSUNG_C_T("028260", "삼성물산"),
    SAMSUNG_BIOLOGICS("207940", "삼성바이오로직스"),
    HYUNDAI_HEAVY_INDUSTRIES("329180", "현대중공업"),
    KB_FINANCIAL("105560", "KB금융"),
    DOOSAN_ENERBILITY("034020", "두산에너빌리티"),
    HANWHA_AEROSPACE("012450", "한화에어로스페이스"),
    SK_HOLDINGS("034730", "SK"),
    KIA("000270", "기아"),
    SHINHAN_FINANCIAL("055550", "신한지주"),
    HYUNDAI_MOBIS("012330", "현대모비스"),
    CELLTRION("068270", "셀트리온"),
    SAMSUNG_SDI("006400", "삼성SDI"),
    HD_HYUNDAI_ELECTRIC("267260", "HD현대일렉트릭"),
    HANA_FINANCIAL("086790", "하나금융지주"),
    HANWHA_OCEAN("042660", "한화오션"),
    LG_ELECTRONICS("066570", "LG전자"),
    NAVER("035420", "NAVER"),
    SAMSUNG_FIRE_MARINE("000810", "삼성화재"),
    HD_KOREA_SHIPBUILDING("009540", "HD한국조선해양"),
    POSCO_HOLDINGS("005490", "POSCO홀딩스"),
    KOREA_ELECTRIC_POWER("015760", "한국전력"),
    HANMI_SEMICONDUCTOR("042700", "한미반도체"),
    KOREA_ZINC("010130", "고려아연"),
    LG_INNOTEK("011070", "LG이노텍"),
    WOORI_FINANCIAL("316140", "우리금융지주"),
    HYUNDAI_ROTEM("064350", "현대로템"),
    LG_CHEM("051910", "LG화학"),
    SK_TELECOM("017670", "SK텔레콤"),
    MERITZ_FINANCIAL("138040", "메리츠금융지주"),
    KT_G("033780", "KT&G"),
    HD_HYUNDAI("267250", "HD현대"),
    SK_INNOVATION("096770", "SK이노베이션"),
    KAKAO("035720", "카카오"),
    SAMSUNG_SDS("018260", "삼성에스디에스"),
    HYUNDAI_GLOVIS("086280", "현대글로비스"),
    KT("030200", "KT"),
    ECOPRO_BM("247540", "에코프로비엠"),
    KRAFTON("259960", "크래프톤"),
    KOREAN_AIR("003490", "대한항공"),
    KAKAO_BANK("323410", "카카오뱅크"),
    KIWOOM_SECURITIES("039490", "키움증권"),
    HYBE("352820", "하이브"),
    AMORE_PACIFIC("090430", "아모레퍼시픽");

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