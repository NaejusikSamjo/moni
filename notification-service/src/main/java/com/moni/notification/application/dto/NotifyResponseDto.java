package com.moni.notification.application.dto;

import com.moni.notification.domain.entity.Notification;
import com.moni.notification.domain.vo.NotificationType;
import java.time.LocalDateTime;
import java.util.UUID;

public record NotifyResponseDto(
    UUID id,
    UUID receiver,
    String content,
    NotificationType type,
    LocalDateTime createdAt
) {

  public static NotifyResponseDto toResponse(Notification notification) {
    return new NotifyResponseDto(notification.getId(), notification.getReceiver(), notification.getContent(), notification.getNotificationType(), notification.getCreatedAt());
  }

}
