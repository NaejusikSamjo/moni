package com.moni.notification.presentation;

import com.moni.notification.application.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/notifications")
@AllArgsConstructor
public class NotifyController {

  private final NotificationService notificationService;

  @Operation(summary = "사용자가 연결되어 있을 때에만 해당 알림을 받을 수 있습니다.")
  @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public ResponseEntity<SseEmitter> subscribe(@RequestHeader("X-User-Id") UUID userId,
      @RequestHeader(value = "Last-Event-Id", required = false, defaultValue = "") String lastEventId) {
    return ResponseEntity.ok(notificationService.subscribe(userId, lastEventId));
  }
}
