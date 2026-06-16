package com.moni.stock.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {

    private static final Logger log = LoggerFactory.getLogger(TestController.class);

    /**
     * Grafana "에러 Trace" 패널 검증용 — 의도적으로 500 에러와 ERROR 로그를 생성한다.
     * 호출: GET http://localhost:19092/test/error
     */
    @GetMapping("/error")
    public ResponseEntity<String> triggerError() {
        log.error("테스트 오류 발생: /test/error 엔드포인트 호출됨 (Distributed Tracing 에러 검증용)");
        throw new RuntimeException("의도적으로 발생시킨 테스트 오류 — Tempo 에러 Trace 확인용");
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        log.info("stock-service ping OK");
        return ResponseEntity.ok("stock-service OK");
    }
}
