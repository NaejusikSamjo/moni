package com.moni.notification.domain.repository;

import java.util.Map;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface EmitterRepository {

  SseEmitter save(String emitterId, SseEmitter sseEmitter);
  void saveEventCache(String eventCacheId, Object event);
  Map<String, SseEmitter> findAllEmitterStartWithUserId(String userId);
  Map<String, Object> findAllEventCacheStartWithUserId(String userId);
  void deleteById(String id);
  void deleteAllEmitterStartWithUserId(String userId);
  void deleteAllEventCacheStartWithUserId(String userId);
  Map<String, SseEmitter> findAll();
}
