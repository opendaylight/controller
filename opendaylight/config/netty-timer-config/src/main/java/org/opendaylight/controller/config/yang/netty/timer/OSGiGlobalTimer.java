/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.netty.timer;

import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, configurationPid = "org.opendaylight.netty.timer", property = "type=global-timer")
@Designate(ocd = OSGiGlobalTimer.Config.class)
public final class OSGiGlobalTimer implements Timer {
    @ObjectClassDefinition
    public @interface Config {
        @AttributeDefinition(name = "tick-duration")
        long tickDuration() default 0;
        @AttributeDefinition(name = "ticks-per-wheel")
        int ticksPerWheel() default 0;
    }

    private static final Logger LOG = LoggerFactory.getLogger(OSGiGlobalTimer.class);

    private Timer delegate;

    @Override
    public Timeout newTimeout(final TimerTask task, final long delay, final TimeUnit unit) {
        return delegate.newTimeout(task, delay, unit);
    }

    @Override
    public Set<Timeout> stop() {
        return delegate.stop();
    }

    @Activate
    void activate(final Config config) {
        delegate = HashedWheelTimerCloseable.newInstance(config.tickDuration(), config.ticksPerWheel());
        LOG.info("Global Netty timer started");
    }

    @Deactivate
    void deactivate() {
        delegate.stop();
        LOG.info("Global Netty timer stopped");
    }
}
