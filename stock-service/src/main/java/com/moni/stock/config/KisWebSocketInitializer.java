package com.moni.stock.config;

import com.moni.stock.domain.repository.StockRepository;
import com.moni.stock.infrastructure.client.websocket.KisWebSocketManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KisWebSocketInitializer implements ApplicationRunner {

    private final KisWebSocketManager kisWebSocketManager;
    private final StockRepository stockRepository;

    @Override
    public void run(ApplicationArguments args) {
        final int[] count = {0};
        kisWebSocketManager.connect();

        // 연결 후 잠시 대기 (연결 완료 대기)
        waitForConnection();

        log.info("KIS WebSocket 초기화 완료");
    }

    public void wsSubscribe(String ticker) {
        kisWebSocketManager.subscribe(ticker);
    }

    private void waitForConnection() {
        int attempts = 0;
        while (!kisWebSocketManager.isConnected() && attempts < 10) {
            try {
                Thread.sleep(500);
                attempts++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        if (!kisWebSocketManager.isConnected()) {
            log.warn("KIS WebSocket 연결 대기 시간 초과 - 구독 건너뜀");
        }
    }
}