package com.moni.notification.application;

import com.moni.notification.domain.repository.EmitterRepository;
import com.moni.notification.domain.vo.NotificationType;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketNotificationScheduler {

  private final NotificationService notificationService;
  private final EmitterRepository emitterRepository;

  @Scheduled(cron = "0 55 8 * * MON-FRI", zone = "Asia/Seoul")
  public void notifyMarketOpenBefore5Min() {
    log.info("한국장 오픈 5분 전 알림 시작");
    sendToAllUsers("한국장 오픈 5분 전 입니다. 오늘 하루도 행운을 빌어요.", NotificationType.MARKET_OPEN);
  }

  @Scheduled(cron = "0 25 15 * * MON-FRI", zone = "Asia/Seoul")
  public void notifyMarketCloseBefore5Min() {
    log.info("한국장 마감 5분 전 알림 시작");
    sendToAllUsers("한국장 마감 5분 전 입니다. 오늘도 좋은 하루 되세요.", NotificationType.MARKET_CLOSE);
  }

  @Scheduled(fixedDelay = 300000)
  public void pingPong() {
    sendToAllUsers("알림 서비스 핑퐁 테스트입니다.", NotificationType.TEST);
  }

  private void sendToAllUsers(String content, NotificationType notificationType) {
    // TODO: Redis Pub/sub 구조
    Map<String, SseEmitter> allEmitters = emitterRepository.findAll();

    allEmitters.forEach((emitterId, emitter) -> {
      String userIdString = emitterId.split("_")[0];
      UUID userId = UUID.fromString(userIdString);

      CompletableFuture.runAsync(() -> {
        try {
          notificationService.send(userId, notificationType, content);
        } catch (Exception e) {
          log.error("알림 발송 실패. 해당 EmitterId: {}", emitterId, e);
        }
      });
    });
  }

}
