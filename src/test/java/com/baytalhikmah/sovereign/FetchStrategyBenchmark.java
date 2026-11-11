package com.baytalhikmah.sovereign;

import java.util.List;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import com.baytalhikmah.sovereign.query.OrderQueries;
import com.baytalhikmah.sovereign.query.OrderSummary;
import com.baytalhikmah.sovereign.service.OrderOverview;

/** The order overview screen of Labs 41-43 and 26, loaded four ways. FetchBenchmarkTest fills the static fields. */
@State(Scope.Benchmark)
public class FetchStrategyBenchmark {

    static OrderOverview orderOverview;
    static OrderQueries orderQueries;
    static String emailDomain;

    @Benchmark
    public List<String> lazyNPlusOne() {
        return orderOverview.naiveOverview(emailDomain);
    }

    @Benchmark
    public List<String> joinFetch() {
        return orderOverview.joinFetchOverview(emailDomain);
    }

    @Benchmark
    public List<String> entityGraph() {
        return orderOverview.graphOverview(emailDomain);
    }

    @Benchmark
    public List<OrderSummary> dtoProjection() {
        return orderQueries.summariesFor(emailDomain);
    }

}
