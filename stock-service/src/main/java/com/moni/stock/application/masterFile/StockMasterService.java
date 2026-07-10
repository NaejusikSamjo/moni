package com.moni.stock.application.masterFile;

import com.moni.stock.domain.entity.Stock;
import com.moni.stock.domain.entity.Theme;
import com.moni.stock.domain.repository.StockRepository;
import com.moni.stock.domain.repository.ThemeRepository;
import com.moni.stock.domain.type.MarketType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockMasterService {

    @Value("${kospiUrl}")
    private String kospiUrl;
    @Value("${kosdaqUrl}")
    private String kosdaqUrl;
    @Value("${themeUrl}")
    private String themeUrl;

    private final KisDataParser kisDataParser;
    private final StockRepository stockRepository;  // 도메인 인터페이스 주입 (StockEntity 직접 X)
    private final ThemeRepository themeRepository;

    public void runOnceOnStartupKospi() {
        log.info("코스피 종목 마스터 초기화 시작");
        updateKospiStockMasters();
    }

    public void runOnceOnStartupKosdaq() {
        log.info("코스닥 종목 마스터 초기화 시작");
        updateKodaqStockMasters();
    }

    public void updateKospiStockMasters() {
        try {
            List<Stock> stocks = kisDataParser.parseMasterFile(kospiUrl, MarketType.KOSPI);
            log.info("파싱 완료: {}개 종목", stocks.size());

            stockRepository.saveAll(stocks);
            log.info("종목 마스터 저장 완료: {}개", stocks.size());
        } catch (Exception e) {
            log.error("종목 마스터 업데이트 실패", e);
            throw new RuntimeException(e);
        }
    }

    public void updateKodaqStockMasters() {
        try {
            List<Stock> stocks = kisDataParser.parseMasterFile(kosdaqUrl, MarketType.KOSDAQ);
            log.info("파싱 완료: {}개 종목", stocks.size());

            stockRepository.saveAll(stocks);
            log.info("종목 마스터 저장 완료: {}개", stocks.size());
        } catch (Exception e) {
            log.error("종목 마스터 업데이트 실패", e);
            throw new RuntimeException(e);
        }
    }

    public void updateThemeMasters() {
        try {
            List<Theme> themes = kisDataParser.parseThemeMasterFile(themeUrl);

            themeRepository.saveAll(themes);
            log.info("테마 마스터 저장 완료: {}개", themes.size());
        } catch (Exception e) {
            log.error("테마 마스터 업데이트 실패", e);
            throw new RuntimeException(e);
        }
    }
}
