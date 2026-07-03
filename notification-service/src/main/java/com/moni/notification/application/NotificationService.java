package com.moni.notification.application;

import com.moni.notification.application.dto.NotifyResponseDto;
import com.moni.notification.domain.entity.Notification;
import com.moni.notification.domain.repository.EmitterRepositoryImpl;
import com.moni.notification.domain.repository.NotificationRepository;
import com.moni.notification.domain.vo.NotificationType;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

  // 연결 시간 1시간
  private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 60;

  private final EmitterRepositoryImpl emitterRepository;
  private final NotificationRepository notificationRepository;


  public SseEmitter subscribe(UUID userId, String lastEventId) {
    String emitterId = createTimeBasedId(userId);
    SseEmitter emitter = emitterRepository.save(emitterId, new SseEmitter(DEFAULT_TIMEOUT));

    // 시간 초과, or 비동기 요청이 안되면 삭제
    emitter.onCompletion(() -> emitterRepository.deleteById(emitterId));
    emitter.onTimeout(() -> emitterRepository.deleteById(emitterId));

    // 503 에러를 방지하기 위한 더미 이벤트 전송
    String eventId = createTimeBasedId(userId);
    sendNotification(emitter, eventId, emitterId, "EventStream Created. [userId=" + userId + "]");

    // 클라이언트가 미수신한 Event 목록이 존재할 경우 전송하여 Event 유실을 예방
    if (hasLostData(lastEventId)) {
      sendLostData(lastEventId, userId, emitterId, emitter);
    }

    return emitter;
  }

  private void sendNotification(SseEmitter emitter, String eventId, String emitterId, Object data) {
    try {
      emitter.send(SseEmitter.event()
          .id(eventId)
          .name("sse")
          .data(data)
      );
    } catch (IOException exception) {
      emitterRepository.deleteById(emitterId);
    }
  }

  private boolean hasLostData(String lastEventId) {
    return !lastEventId.isEmpty();
  }

  private void sendLostData(String lastEventId, UUID userId, String emitterId, SseEmitter emitter) {
    Map<String, Object> eventCaches = emitterRepository.findAllEventCacheStartWithUserId(userId.toString());
    eventCaches.entrySet().stream()
        .filter(entry -> lastEventId.compareTo(entry.getKey()) < 0)
        .forEach(entry -> sendNotification(emitter, entry.getKey(), emitterId, entry.getValue()));
  }

  public void send(UUID userId, NotificationType notificationType, String content) {
    Notification notify = Notification.create(content, notificationType, userId, false);

    Notification notification = notificationRepository.save(notify);

    String eventId = createTimeBasedId(userId);

    Map<String, SseEmitter> emitters = emitterRepository.findAllEmitterStartWithUserId(userId.toString());
    emitters.forEach(
        (key, emitter) -> {
          emitterRepository.saveEventCache(eventId, notification);
          sendNotification(emitter, eventId, key, NotifyResponseDto.toResponse(notification));
        }
    );
  }

  private String createTimeBasedId(UUID userId) {
    return userId.toString() + "_" + System.currentTimeMillis();
  }
}
