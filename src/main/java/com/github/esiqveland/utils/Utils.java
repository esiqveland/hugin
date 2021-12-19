package com.github.esiqveland.utils;

import io.vavr.CheckedFunction0;

import java.time.Duration;
import java.util.function.Consumer;

public class Utils {
    public static <T> T timed(CheckedFunction0<T> f, Consumer<Duration> log) throws Throwable {
        var start = System.nanoTime();
        try {
            return f.apply();
        } finally {
            var end = System.nanoTime();
            var elapsedNanos = end - start;
            log.accept(Duration.ofNanos(elapsedNanos));
        }
    }
}
