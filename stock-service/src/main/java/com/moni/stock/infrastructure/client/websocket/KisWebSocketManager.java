package com.moni.stock.infrastructure.client.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moni.stock.infrastructure.client.KisOAuthClient;
import com.moni.stock.infrastructure.client.KisProperties;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import jakarta.websocket.ContainerProvider;
import jakarta.websocket.WebSocketContainer;
import java.net.URI;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class KisWebSocketManager {

    private final KisProperties kisProperties;
    private final KisOAuthClient kisOAuthClient;
    private final KisWebSocketHandler kisWebSocketHandler;
    private final ObjectMapper objectMapper;

    private WebSocketSession session;
    private final Map<String, Boolean> subscribedTickers = new ConcurrentHashMap<>();

    public void connect() {
        if (kisProperties.isMock()) {
            log.info("KIS mock 모드 — WebSocket 연결 건너뜀");
            return;
        }
        kisOAuthClient.resetApprovalKey();
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        container.setDefaultMaxTextMessageBufferSize(1024 * 1024); // 1MB 메모리 부족 예외 방지
        //웹소켓 구독
        StandardWebSocketClient client = new StandardWebSocketClient(container);
        //kisWebSocketHandler에서 수신 메시지 처리
        client.execute(kisWebSocketHandler, new WebSocketHttpHeaders(), URI.create(kisProperties.getWsUrl()))
                .thenAccept(s -> {
                    this.session = s;
                    log.info("KIS WebSocket 연결 완료");
                })
                .exceptionally(e -> {
                    log.error("KIS WebSocket 연결 실패: {}", e.getMessage());
                    return null;
                });
    }

    public void subscribe(String ticker) {
        if (subscribedTickers.containsKey(ticker)) return;
        sendSubscription(ticker, "1");
    }

    public void confirmSubscribed(String ticker) {
        subscribedTickers.put(ticker, true);
    }

    public void unsubscribe(String ticker) {
        if (!subscribedTickers.containsKey(ticker)) return;
        sendSubscription(ticker, "2");
        subscribedTickers.remove(ticker);
    }

    private void sendSubscription(String ticker, String trType) {
        if (session == null || !session.isOpen()) {
            log.warn("WebSocket 세션이 열려있지 않습니다. ticker={}", ticker);
            return;
        }
        try {
            Map<String, Object> request = Map.of(
                    "header", Map.of(
                            "approval_key", kisOAuthClient.getApprovalKey(),
                            "custtype", "P",
                            "tr_type", trType,
                            "content-type", "utf-8"
                    ),
                    "body", Map.of(
                            "input", Map.of(
                                    "tr_id", "H0STCNT0",
                                    "tr_key", ticker
                            )
                    )
            );
            //구독 websocket으로 publish
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(request)));
            log.info("KIS 구독 {}: {}", trType.equals("1") ? "등록" : "해제", ticker);
        } catch (Exception e) {
            log.error("구독 메시지 전송 실패: ticker={}", ticker, e);
        }
    }

    public boolean isConnected() {
        return session != null && session.isOpen();
    }

    @PreDestroy
    public void disconnect() {
        if (session == null || !session.isOpen()) return;
        try {
            new ArrayList<>(subscribedTickers.keySet()).forEach(ticker -> sendSubscription(ticker, "2"));
            session.close(CloseStatus.NORMAL);
            log.info("KIS WebSocket 정상 종료");
        } catch (Exception e) {
            log.warn("KIS WebSocket 종료 중 오류: {}", e.getMessage());
        }
    }
}