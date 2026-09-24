package org.netra.features.performance;

import java.util.Collections;
import java.util.List;

/**
 * Encapsulates benchmark metrics for an operation:
 * Request count, throughput, error rate, avg, p50, p95, p99, min, max latencies.
 */
public class BenchmarkResult {

    private final String operationName;
    private final int totalRequests;
    private final int errorCount;
    private final double durationSeconds;
    private final double throughput;
    private final double errorRate;
    private final double avgLatencyMs;
    private final double p50Ms;
    private final double p95Ms;
    private final double p99Ms;
    private final double minMs;
    private final double maxMs;

    public BenchmarkResult(String operationName, List<Long> latenciesNanos, int errorCount, double durationSeconds) {
        this.operationName = operationName;
        this.totalRequests = latenciesNanos.size();
        this.errorCount = errorCount;
        this.durationSeconds = durationSeconds;
        this.throughput = durationSeconds > 0 ? totalRequests / durationSeconds : 0.0;
        this.errorRate = totalRequests > 0 ? (errorCount * 100.0) / totalRequests : 0.0;

        if (latenciesNanos.isEmpty()) {
            this.avgLatencyMs = 0;
            this.p50Ms = 0;
            this.p95Ms = 0;
            this.p99Ms = 0;
            this.minMs = 0;
            this.maxMs = 0;
        } else {
            Collections.sort(latenciesNanos);
            long sum = 0;
            for (long l : latenciesNanos) sum += l;
            this.avgLatencyMs = (sum / (double) latenciesNanos.size()) / 1_000_000.0;
            this.minMs = latenciesNanos.get(0) / 1_000_000.0;
            this.maxMs = latenciesNanos.get(latenciesNanos.size() - 1) / 1_000_000.0;
            this.p50Ms = percentile(latenciesNanos, 50.0);
            this.p95Ms = percentile(latenciesNanos, 95.0);
            this.p99Ms = percentile(latenciesNanos, 99.0);
        }
    }

    private static double percentile(List<Long> sorted, double pct) {
        if (sorted.isEmpty()) return 0;
        int idx = (int) Math.ceil((pct / 100.0) * sorted.size()) - 1;
        idx = Math.max(0, Math.min(sorted.size() - 1, idx));
        return sorted.get(idx) / 1_000_000.0;
    }

    public String getOperationName() {
        return operationName;
    }

    public int getTotalRequests() {
        return totalRequests;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public double getDurationSeconds() {
        return durationSeconds;
    }

    public double getThroughput() {
        return throughput;
    }

    public double getErrorRate() {
        return errorRate;
    }

    public double getAvgLatencyMs() {
        return avgLatencyMs;
    }

    public double getP50Ms() {
        return p50Ms;
    }

    public double getP95Ms() {
        return p95Ms;
    }

    public double getP99Ms() {
        return p99Ms;
    }

    public double getMinMs() {
        return minMs;
    }

    public double getMaxMs() {
        return maxMs;
    }

    @Override
    public String toString() {
        return String.format("%s: Count=%d, Avg=%.2f ms, p50=%.2f ms, p95=%.2f ms, p99=%.2f ms, Min=%.2f ms, Max=%.2f ms, Throughput=%.2f req/s, ErrorRate=%.2f%%",
                operationName, totalRequests, avgLatencyMs, p50Ms, p95Ms, p99Ms, minMs, maxMs, throughput, errorRate);
    }
}
