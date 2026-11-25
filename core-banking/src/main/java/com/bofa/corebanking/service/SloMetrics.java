package com.bofa.corebanking.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/** Rolling 15-minute latency / error-rate window per endpoint, published on /ops/health/slo. */
@Component
public class SloMetrics {

    private static final long WINDOW_MS = 15 * 60 * 1000L;

    private final Map<String, Deque<long[]>> samples = new ConcurrentHashMap<String, Deque<long[]>>();

    public void record(String endpoint, long durationMs, int status) {
        Deque<long[]> q = samples.get(endpoint);
        if (q == null) {
            samples.putIfAbsent(endpoint, new ConcurrentLinkedDeque<long[]>());
            q = samples.get(endpoint);
        }
        q.addLast(new long[]{System.currentTimeMillis(), durationMs, status});
    }

    public Map<String, Map<String, Object>> snapshot() {
        long cutoff = System.currentTimeMillis() - WINDOW_MS;
        Map<String, Map<String, Object>> out = new LinkedHashMap<String, Map<String, Object>>();
        for (Map.Entry<String, Deque<long[]>> e : samples.entrySet()) {
            List<Long> durations = new ArrayList<Long>();
            int errors = 0;
            for (Iterator<long[]> it = e.getValue().iterator(); it.hasNext(); ) {
                long[] s = it.next();
                if (s[0] < cutoff) {
                    it.remove();
                    continue;
                }
                durations.add(s[1]);
                if (s[2] >= 500) {
                    errors++;
                }
            }
            if (durations.isEmpty()) {
                continue;
            }
            Collections.sort(durations);
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("requests", durations.size());
            m.put("p50Ms", durations.get((int) Math.floor(durations.size() * 0.50)));
            m.put("p95Ms", durations.get(Math.min(durations.size() - 1, (int) Math.floor(durations.size() * 0.95))));
            m.put("errorRate", (double) errors / durations.size());
            out.put(e.getKey(), m);
        }
        return out;
    }
}
