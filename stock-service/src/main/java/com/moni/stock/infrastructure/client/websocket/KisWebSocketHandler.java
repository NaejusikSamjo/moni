package com.moni.stock.infrastructure.client.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moni.stock.domain.entity.StockPrice;
import com.moni.stock.infrastructure.redis.StockPriceRedisAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class KisWebSocketHandler extends TextWebSocketHandler {

    private final StockPriceRedisAdapter stockPriceRedisAdapter;
    private final ObjectMapper objectMapper;

    @Lazy
    @Autowired
    private KisWebSocketManager kisWebSocketManager;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("KIS WebSocket 연결 성공: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        //log.info("RAW: {}", payload.length() > 80 ? payload.substring(0, 80) : payload);

        // JSON 응답 처리 (PINGPONG, 구독 응답 등)
        if (payload.startsWith("{")) {
            handleJsonMessage(session, payload);
            return;
        }

        // 실시간 데이터 처리: 0|H0STCNT0|001|{data}
        parsePriceAndSave(payload);
    }

    private void handleJsonMessage(WebSocketSession session, String payload) throws Exception {
        JsonNode node = objectMapper.readTree(payload);
        String trId = node.path("header").path("tr_id").asText();

        if ("PINGPONG".equals(trId)) {
            session.sendMessage(new TextMessage(payload));
            log.debug("KIS PINGPONG 응답");
        } else {
            String rtCd = node.path("body").path("rt_cd").asText(); //연결 성공 여부 (0:성공, 1:실패)
            String msg1 = node.path("body").path("msg1").asText(); //메시지
            String trKey = node.path("header").path("tr_key").asText(); //ticker
            //log.info("KIS 메시지 수신: tr_id={}, tr_key={}, rt_cd={}, msg={}", trId, trKey, rtCd, msg1);
            if ("0".equals(rtCd) && "H0STCNT0".equals(trId)) {
                //연결 중인 stock 세션에 저장
                kisWebSocketManager.confirmSubscribed(trKey);
            }
        }
    }

    private void parsePriceAndSave(String payload) {
        // 형식: {encrypt}|{tr_id}|{count}|{data}
        String[] parts = payload.split("\\|", 4);
        if (parts.length < 4) return;

        String trId = parts[1];
        if (!"H0STCNT0".equals(trId)) return;

        int pageNum = Integer.parseInt(parts[2]);
        for(int i=0; i<pageNum; i++) {
            // 데이터: ticker^체결시간^현재가^...^매도호가1^매수호가1^체결거래량^누적거래량^...
            String[] data = parts[3].split("\\^");
            if (data.length < 14) return;

            try {

                StockPrice price = stockPriceRedisAdapter.getPrice(data[0]).get();

                StockPrice stockPrice = StockPrice.builder()
                        .ticker(data[0]) //ticker
                        .currentPrice(new BigDecimal(data[2])) //현재가
                        .askPrice(new BigDecimal(data[10]))  // 매도호가1
                        .bidPrice(new BigDecimal(data[11]))  // 매수호가1
                        .volume(Long.parseLong(data[13]))    // 누적거래량
                        .section(price.getSection())
                        .build();

            stockPriceRedisAdapter.savePrice(stockPrice);
                //log.debug("가격 저장: {} = {}", stockPrice.getTicker(), stockPrice.getCurrentPrice());
            } catch (NumberFormatException e) {
                log.warn("가격 파싱 실패: {}", payload);
            }
        }


    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("KIS WebSocket 오류: {}", exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.warn("KIS WebSocket 연결 종료: {}", status);
        kisWebSocketManager.connect();

        waitForConnection();

        log.info("KIS WebSocket 재초기화 완료");
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