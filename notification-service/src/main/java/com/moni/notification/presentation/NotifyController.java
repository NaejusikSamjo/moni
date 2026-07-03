package com.moni.notification.presentation;

import com.moni.notification.application.NotificationService;
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

  @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public ResponseEntity<SseEmitter> subscribe(@RequestHeader("X-User-Id") UUID userId,
      @RequestHeader(value = "Last-Event-Id", required = false, defaultValue = "") String lastEventId) {
    return ResponseEntity.ok(notificationService.subscribe(userId, lastEventId));
  }
}
