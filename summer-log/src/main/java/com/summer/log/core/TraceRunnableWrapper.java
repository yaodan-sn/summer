package com.summer.log.core;

import com.summer.log.constant.LogCategoryConstant;
import com.summer.log.filter.LogCategoryFilter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.SpanNamer;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * 对Runnable接口进行包装,实现多线程环境的日志文件按类型分割。
 *
 * @author Tom
 * @version 1.0.0
 * @date 1/21/22
 */
@Slf4j
public class TraceRunnableWrapper implements Runnable {

    private final String logCategory;

    private final Runnable delegate;

    public TraceRunnableWrapper(Runnable delegate) {
        this(delegate, null);
    }

    public TraceRunnableWrapper(Runnable delegate, boolean continueSpan) {
        this(delegate, null, continueSpan);
    }

    public TraceRunnableWrapper(Runnable delegate, String name) {
        this(delegate, name, false);
    }

    public TraceRunnableWrapper(Runnable delegate, String name, boolean continueSpan) {
        this(delegate, name, null, continueSpan);
    }

    public TraceRunnableWrapper(Runnable delegate, String name, String logCategory, boolean continueSpan) {
        if (StringUtils.hasText(logCategory)) {
            this.logCategory = logCategory;
        } else {
            this.logCategory = LogCategoryConstant.ASYNC;
        }
        if (Objects.nonNull(TracerHolder.getTracer())) {
            if (continueSpan) {
                this.delegate = new TraceRunnable(TracerHolder.getTracer(), TracerHolder.getSpanNamer(), delegate, name);
            } else {
                this.delegate = new TraceRunnable(TracerHolder.getTracer(), null, TracerHolder.getSpanNamer(), delegate, name);
            }
        } else {
            this.delegate = delegate;
        }
    }

    @Override
    public void run() {
        MDC.put(LogCategoryFilter.LOG_CATEGORY_NAME, logCategory);
        try {
            this.delegate.run();
        } finally {
            MDC.remove(LogCategoryFilter.LOG_CATEGORY_NAME);
        }
    }

    private class TraceRunnable implements Runnable {

        private static final String DEFAULT_SPAN_NAME = "async";

        private final Tracer tracer;

        private final Runnable delegate;

        private final Span parent;

        private final String spanName;

        public TraceRunnable(Tracer tracer, SpanNamer spanNamer, Runnable delegate, String name) {
            this(tracer, tracer.currentSpan(), spanNamer, delegate, name);
        }

        public TraceRunnable(Tracer tracer, Span parent, SpanNamer spanNamer, Runnable delegate, String name) {
            this.tracer = tracer;
            this.delegate = delegate;
            this.parent = parent;
            this.spanName = name != null ? name : spanNamer.name(delegate, DEFAULT_SPAN_NAME);
        }

        @Override
        public void run() {
            Span childSpan = this.tracer.nextSpan(this.parent).name(this.spanName);
            try (Tracer.SpanInScope ws = this.tracer.withSpan(childSpan.start())) {
                this.delegate.run();
            } catch (Exception | Error e) {
                childSpan.error(e);
                throw e;
            } finally {
                childSpan.end();
            }
        }

    }

}
