/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.netty.eventexecutor;

import static io.netty.util.concurrent.GlobalEventExecutor.INSTANCE;

import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.ProgressivePromise;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.ScheduledFuture;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, property = "type=global-event-executor")
public final class OSGiGlobalEventExecutor implements EventExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(OSGiGlobalEventExecutor.class);

    @Override
    public boolean isShuttingDown() {
        return INSTANCE.isShuttingDown();
    }

    @Override
    public Future<?> shutdownGracefully() {
        return INSTANCE.shutdownGracefully();
    }

    @Override
    public Future<?> shutdownGracefully(final long quietPeriod, final long timeout, final TimeUnit unit) {
        return INSTANCE.shutdownGracefully(quietPeriod, timeout, unit);
    }

    @Override
    public Future<?> terminationFuture() {
        return INSTANCE.terminationFuture();
    }

    @Override
    @Deprecated
    public void shutdown() {
        INSTANCE.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return INSTANCE.shutdownNow();
    }

    @Override
    public Iterator<EventExecutor> iterator() {
        return INSTANCE.iterator();
    }

    @Override
    public Future<?> submit(final Runnable task) {
        return INSTANCE.submit(task);
    }

    @Override
    public <T> Future<T> submit(final Runnable task, final T result) {
        return INSTANCE.submit(task, result);
    }

    @Override
    public <T> Future<T> submit(final Callable<T> task) {
        return INSTANCE.submit(task);
    }

    @Override
    public ScheduledFuture<?> schedule(final Runnable command, final long delay, final TimeUnit unit) {
        return INSTANCE.schedule(command, delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(final Callable<V> callable, final long delay, final TimeUnit unit) {
        return INSTANCE.schedule(callable, delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(final Runnable command, final long initialDelay, final long period,
            final TimeUnit unit) {
        return INSTANCE.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable command, final long initialDelay, final long delay,
            final TimeUnit unit) {
        return INSTANCE.scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

    @Override
    public boolean isShutdown() {
        return INSTANCE.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return INSTANCE.isTerminated();
    }

    @Override
    public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
        return INSTANCE.awaitTermination(timeout, unit);
    }

    @Override
    public <T> List<java.util.concurrent.Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks)
            throws InterruptedException {
        return INSTANCE.invokeAll(tasks);
    }

    @Override
    public <T> List<java.util.concurrent.Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks,
            final long timeout, final TimeUnit unit) throws InterruptedException {
        return INSTANCE.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException {
        return INSTANCE.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return INSTANCE.invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(final Runnable command) {
        INSTANCE.execute(command);
    }

    @Override
    public EventExecutor next() {
        return INSTANCE.next();
    }

    @Override
    public EventExecutorGroup parent() {
        return INSTANCE.parent();
    }

    @Override
    public boolean inEventLoop() {
        return INSTANCE.inEventLoop();
    }

    @Override
    public boolean inEventLoop(final Thread thread) {
        return INSTANCE.inEventLoop(thread);
    }

    @Override
    public <V> Promise<V> newPromise() {
        return INSTANCE.newPromise();
    }

    @Override
    public <V> ProgressivePromise<V> newProgressivePromise() {
        return INSTANCE.newProgressivePromise();
    }

    @Override
    public <V> Future<V> newSucceededFuture(final V result) {
        return INSTANCE.newSucceededFuture(result);
    }

    @Override
    public <V> Future<V> newFailedFuture(final Throwable cause) {
        return INSTANCE.newFailedFuture(cause);
    }

    @Activate
    void activate() {
        LOG.info("Global Event executor enabled");
    }

    @Deactivate
    void deactivate() {
        LOG.info("Global Event executor disabled");
    }

}
