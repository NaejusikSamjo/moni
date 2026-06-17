package com.moni.payment.actuator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "management.endpoints.web.exposure.include=health,metrics,prometheus",
        "management.endpoint.health.show-details=always",
        "management.endpoint.prometheus.access=read_only",
        "management.prometheus.metrics.export.enabled=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Actuator 엔드포인트 통합 테스트")
class ActuatorEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("/actuator/health 가 200 OK와 UP 상태를 반환한다")
    void healthEndpointReturnsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("/actuator/prometheus 가 200 OK와 JVM 메트릭을 반환한다")
    void prometheusEndpointReturnMetrics() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("jvm_memory")));
    }

    @Test
    @DisplayName("/actuator/metrics 가 200 OK와 메트릭 목록을 반환한다")
    void metricsEndpointReturnsList() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names").isArray());
    }

    @Test
    @DisplayName("/actuator/metrics/jvm.memory.used 개별 메트릭 조회가 가능하다")
    void specificMetricIsAccessible() throws Exception {
        mockMvc.perform(get("/actuator/metrics/jvm.memory.used"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("jvm.memory.used"))
                .andExpect(jsonPath("$.measurements[0].value").isNumber());
    }
}
