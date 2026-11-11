package com.baytalhikmah.sovereign;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

import com.baytalhikmah.sovereign.query.OrderQueries;
import com.baytalhikmah.sovereign.service.OrderOverview;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "logging.level.p6spy=warn")
class FetchBenchmarkTest {

    static final String KEY = "q55";

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    OrderOverview orderOverview;

    @Autowired
    OrderQueries orderQueries;

    @Test
    void measuresTheFetchStrategies() throws Exception {
        transaction.executeWithoutResult(status -> QueryLabData.create(entityManager, KEY));
        FetchStrategyBenchmark.orderOverview = orderOverview;
        FetchStrategyBenchmark.orderQueries = orderQueries;
        FetchStrategyBenchmark.emailDomain = QueryLabData.emailDomain(KEY);

        Options options = new OptionsBuilder()
            .include(FetchStrategyBenchmark.class.getName())
            .forks(0)
            .threads(1)
            .mode(Mode.AverageTime)
            .timeUnit(TimeUnit.MICROSECONDS)
            .warmupIterations(2)
            .warmupTime(TimeValue.seconds(1))
            .measurementIterations(3)
            .measurementTime(TimeValue.seconds(1))
            .shouldFailOnError(true)
            .build();
        Collection<RunResult> results = new Runner(options).run();

        Map<String, Double> microseconds = new TreeMap<>();
        for (RunResult result : results) {
            String benchmark = result.getParams().getBenchmark();
            microseconds.put(benchmark.substring(benchmark.lastIndexOf('.') + 1), result.getPrimaryResult().getScore());
        }

        System.out.println("=== FETCH STRATEGY BENCHMARK (average time per call, microseconds) ===");
        microseconds.forEach((name, score) -> System.out.printf("%-14s : %10.1f%n", name, score));
        System.out.printf("N+1 is %.1f times slower than join fetch%n",
            microseconds.get("lazyNPlusOne") / microseconds.get("joinFetch"));

        assertThat(microseconds).containsOnlyKeys("dtoProjection", "entityGraph", "joinFetch", "lazyNPlusOne");
        assertThat(microseconds.get("lazyNPlusOne")).isGreaterThan(microseconds.get("joinFetch"));
        assertThat(microseconds.get("lazyNPlusOne")).isGreaterThan(microseconds.get("dtoProjection"));
    }

}
