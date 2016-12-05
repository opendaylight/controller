/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.netty.timer;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import java.util.Set;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

public final class HashedWheelTimerCloseable implements AutoCloseable, Timer {

    private final Timer timer;

    private HashedWheelTimerCloseable(Timer timer) {
        this.timer = timer;
    }

    @Override
    public void close() throws Exception {
        stop();
    }

    @Override
    public Timeout newTimeout(TimerTask task, long delay, TimeUnit unit) {
        return this.timer.newTimeout(task, delay, unit);
    }

    @Override
    public Set<Timeout> stop() {
        return this.timer.stop();
    }

    public static HashedWheelTimerCloseable newInstance(@Nullable ThreadFactory threadFactory,
            @Nullable Long duration, @Nullable Integer ticksPerWheel) {
        TimeUnit unit = TimeUnit.MILLISECONDS;
        if(!nullOrNonPositive(duration) && threadFactory == null && nullOrNonPositive(ticksPerWheel)) {
            return new HashedWheelTimerCloseable(new HashedWheelTimer(duration, unit));
        }

        if(!nullOrNonPositive(duration) && threadFactory == null && !nullOrNonPositive(ticksPerWheel)) {
            return new HashedWheelTimerCloseable(new HashedWheelTimer(duration, unit, ticksPerWheel));
        }

        if(nullOrNonPositive(duration) && threadFactory != null && nullOrNonPositive(ticksPerWheel)) {
            return new HashedWheelTimerCloseable(new HashedWheelTimer(threadFactory));
        }

        if(!nullOrNonPositive(duration) && threadFactory != null && nullOrNonPositive(ticksPerWheel)) {
            return new HashedWheelTimerCloseable(
                    new HashedWheelTimer(threadFactory, duration, unit));
        }

        if(!nullOrNonPositive(duration) && threadFactory != null && !nullOrNonPositive(ticksPerWheel)) {
            return new HashedWheelTimerCloseable(
                    new HashedWheelTimer(threadFactory, duration, unit, ticksPerWheel));
        }

        return new HashedWheelTimerCloseable(new HashedWheelTimer());
    }

    private static boolean nullOrNonPositive(Number n) {
        return n == null || n.longValue() <= 0;
    }
}
