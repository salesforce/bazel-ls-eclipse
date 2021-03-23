package com.salesforce.b2eclipse.internal;

import java.util.HashMap;
import java.util.Optional;

import com.salesforce.b2eclipse.BazelJdtPlugin;

public final class TimeTracker {
    private static final double NANO_IN_SECONDS = 1000000000d;
    private static final HashMap<String, Long> TIMES = new HashMap<String, Long>();

    private TimeTracker() {
    }

    public static void start() {
        final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        final StackTraceElement element = findStackElement(stackTrace);
        final String methodName = getMethodName(element);
        long start = System.nanoTime();
        TIMES.put(methodName, Long.valueOf(start));
    }

    public static void finish() {
        long finish = System.nanoTime();
        final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        final StackTraceElement element = findStackElement(stackTrace);
        final String methodName = getMethodName(element);
        Long start = TIMES.remove(methodName);
        if (start != null) {
            BazelJdtPlugin.logInfo(String.format("TIME TRACKER: Method %s duration %f seconds", methodName,
                ((double) finish - start.doubleValue()) / NANO_IN_SECONDS));
        }
    }

    private static String getMethodName(StackTraceElement element) {
        final StringBuilder builder = new StringBuilder();
        Optional<StackTraceElement> optional = Optional.ofNullable(element);
        optional.ifPresent((item) -> builder.append(item.getClassName()).append("#").append(item.getMethodName()));
        return builder.toString();
    }

    private static StackTraceElement findStackElement(StackTraceElement[] stackTrace) {
        StackTraceElement element = null;
        for (int i = 0; i < stackTrace.length; i++) {
            if (TimeTracker.class.getName().equals(stackTrace[i].getClassName())) {
                element = stackTrace[i + 1];
                break;
            }
        }
        return element;
    }
}
