package io.etherflow.core;

import java.util.concurrent.*;

public final class Schedulers {

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

    private static final ScheduledExecutorService PARALLEL =
            Executors.newScheduledThreadPool(CPU_COUNT, r -> {
                Thread t = new Thread(r, "etherflow-parallel-" + r.hashCode());
                t.setDaemon(true);
                return t;
            });

    private static final ExecutorService SINGLE =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "etherflow-single");
                t.setDaemon(true);
                return t;
            });

    private static final ScheduledExecutorService TIMER =
            Executors.newScheduledThreadPool(CPU_COUNT, r -> {
                Thread t = new Thread(r, "etherflow-timer-" + r.hashCode());
                t.setDaemon(true);
                return t;
            });

    private static final ForkJoinPool BOUNDED_ELASTIC = new ForkJoinPool(
            Math.max(CPU_COUNT * 10, 100),
            ForkJoinPool.defaultForkJoinWorkerThreadFactory,
            null, false);

    public static Executor parallel() {
        return PARALLEL;
    }

    public static Executor single() {
        return SINGLE;
    }

    public static ScheduledExecutorService timer() {
        return TIMER;
    }

    public static Executor boundedElastic() {
        return BOUNDED_ELASTIC;
    }

    public static Executor immediate() {
        return Runnable::run;
    }

    private Schedulers() {}
}
