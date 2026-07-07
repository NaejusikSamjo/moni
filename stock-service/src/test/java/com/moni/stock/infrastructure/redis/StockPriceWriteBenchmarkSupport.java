package com.moni.stock.infrastructure.redis;

import com.moni.stock.domain.entity.StockPrice;
import com.moni.stock.infrastructure.client.KisOAuthClient;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

// KisWebSocketHandler가 틱마다 하는 Redis SET 1회를 흉내내는 합성 부하 테스트.
// redis-benchmark(순수 Redis 처리량)와 달리 StockPriceRedisAdapter -> Jackson 직렬화 -> RedisTemplate
// 실제 앱 코드 경로를 그대로 태워서 앱 오버헤드까지 포함한 값을 측정한다.
// 서브클래스가 @TestPropertySource로 stock.redis.mode 등을 지정해 standalone/cluster를 고른다.
@SpringBootTest
@Tag("benchmark")
abstract class StockPriceWriteBenchmarkSupport {

    private static final int TICKER_COUNT = 500;
    private static final int TOTAL_OPS = 20_000;
    private static final int[] CONCURRENCY_LEVELS = {1, 20, 50, 100, 200};

    @Autowired
    private StockPriceRedisAdapter stockPriceRedisAdapter;

    @MockitoBean
    private KisOAuthClient kisOAuthClient;

    @Test
    void benchmarkWriteThroughput() throws InterruptedException {
        List<String> tickers = new ArrayList<>();
        for (int i = 0; i < TICKER_COUNT; i++) {
            tickers.add(String.format("%06d", i));
        }

        System.out.println("| 동시성(-c) | ops/sec | p50(ms) | p95(ms) | p99(ms) |");
        System.out.println("|---|---|---|---|---|");

        for (int concurrency : CONCURRENCY_LEVELS) {
            BenchmarkResult result = run(tickers, concurrency);
            System.out.printf(
                    "| %d | %.0f | %.2f | %.2f | %.2f |%n",
                    concurrency, result.opsPerSec(), result.p50Ms(), result.p95Ms(), result.p99Ms());
        }
    }

    private BenchmarkResult run(List<String> tickers, int concurrency) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(concurrency);
        long[] latenciesNanos = new long[TOTAL_OPS];
        CountDownLatch latch = new CountDownLatch(TOTAL_OPS);

        long start = System.nanoTime();
        for (int i = 0; i < TOTAL_OPS; i++) {
            int index = i;
            pool.execute(() -> {
                try {
                    String ticker = tickers.get(ThreadLocalRandom.current().nextInt(tickers.size()));
                    long callStart = System.nanoTime();
                    stockPriceRedisAdapter.savePrice(randomPrice(ticker));
                    latenciesNanos[index] = System.nanoTime() - callStart;
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await(5, TimeUnit.MINUTES);
        long elapsedNanos = System.nanoTime() - start;
        pool.shutdown();

        Arrays.sort(latenciesNanos);
        double opsPerSec = TOTAL_OPS / (elapsedNanos / 1_000_000_000.0);
        return new BenchmarkResult(
                opsPerSec,
                toMillis(percentile(latenciesNanos, 0.50)),
                toMillis(percentile(latenciesNanos, 0.95)),
                toMillis(percentile(latenciesNanos, 0.99)));
    }

    private StockPrice randomPrice(String ticker) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        BigDecimal base = BigDecimal.valueOf(10_000 + random.nextInt(90_000));
        return StockPrice.builder()
                .ticker(ticker)
                .askPrice(base.add(BigDecimal.ONE))
                .bidPrice(base.subtract(BigDecimal.ONE))
                .currentPrice(base)
                .volume((long) random.nextInt(1_000_000))
                .section("KOSPI")
                .build();
    }

    private long percentile(long[] sortedNanos, double p) {
        int index = (int) Math.ceil(p * sortedNanos.length) - 1;
        return sortedNanos[Math.max(0, Math.min(index, sortedNanos.length - 1))];
    }

    private double toMillis(long nanos) {
        return nanos / 1_000_000.0;
    }

    private record BenchmarkResult(double opsPerSec, double p50Ms, double p95Ms, double p99Ms) {
    }
}