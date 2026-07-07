package com.moni.stock.infrastructure.redis;

import org.junit.jupiter.api.Tag;
import org.springframework.test.context.TestPropertySource;

// 로컬 redis-cluster-0/1/2 컨테이너(3-master, no replica) 대상 — 실행 전 로컬에 띄우고
// REDIS_CLUSTER_HOST=localhost 로 재기동해서 announce 주소가 localhost를 가리키게 해야 함
// (TROUBLESHOOTING.md 1번 이슈와 동일한 원인 — 로컬 JVM에서 접속하므로 host.docker.internal은 못 씀)
@Tag("benchmark")
@TestPropertySource(properties = {
        "stock.redis.mode=cluster",
        "spring.data.redis.cluster.nodes=localhost:7000,localhost:7001,localhost:7002"
})
class StockRedisClusterBenchmarkTest extends StockPriceWriteBenchmarkSupport {
}