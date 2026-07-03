package com.moni.notification.domain.entity;

import com.moni.notification.domain.vo.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "p_notification")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Notification extends BaseIdGenerator {

  private String content;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private NotificationType notificationType;

  @Column(nullable = false)
  private UUID receiver;

  @Column(nullable = false)
  private Boolean isRead;

  public static Notification create(String content, NotificationType notificationType, UUID receiver, Boolean isRead) {
    Notification notification = new Notification();
    notification.content = content;
    notification.notificationType = notificationType;
    notification.receiver = receiver;
    notification.isRead = isRead;
    return notification;
  }
}
