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
public class TestMarketNotificationScheduler {

  private final NotificationService notificationService;
  private final EmitterRepository emitterRepository;

  @Scheduled(cron = "0 40 10 * * MON-FRI", zone = "Asia/Seoul")
  public void notifyTest1() {
    log.info("한국장 오픈 5분 전 알림 시작");
    sendToAllUsers("한국장 오픈 5분 전 입니다. 오늘 하루도 행운을 빌어요.", NotificationType.MARKET_OPEN);
  }

  @Scheduled(cron = "0 41 10 * * MON-FRI", zone = "Asia/Seoul")
  public void notifyTest2() {
    log.info("한국장 마감 5분 전 알림 시작");
    sendToAllUsers("한국장 마감 5분 전 입니다. 오늘도 좋은 하루 되세요.", NotificationType.MARKET_CLOSE);
  }

  @Scheduled(cron = "0 42 10 * * MON-FRI", zone = "Asia/Seoul")
  public void notifyTest3() {
    log.info("한국장 마감 5분 전 알림 시작");
    sendToAllUsers("한국장 마감 5분 전 입니다. 오늘도 좋은 하루 되세요.", NotificationType.MARKET_CLOSE);
  }

  @Scheduled(cron = "0 38 10 * * MON-FRI", zone = "Asia/Seoul")
  public void notifyTest4() {
    log.info("한국장 마감 5분 전 알림 시작");
    sendToAllUsers("한국장 마감 5분 전 입니다. 오늘도 좋은 하루 되세요.", NotificationType.MARKET_CLOSE);
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
