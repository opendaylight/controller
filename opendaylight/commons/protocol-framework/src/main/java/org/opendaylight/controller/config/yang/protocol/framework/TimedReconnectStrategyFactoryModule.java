/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.protocol.framework;

import io.netty.util.concurrent.EventExecutor;

import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.protocol.framework.TimedReconnectStrategy;

import com.google.common.base.Preconditions;

/**
*
*/
@Deprecated
public final class TimedReconnectStrategyFactoryModule extends org.opendaylight.controller.config.yang.protocol.framework.AbstractTimedReconnectStrategyFactoryModule
 {

    public TimedReconnectStrategyFactoryModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public TimedReconnectStrategyFactoryModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            TimedReconnectStrategyFactoryModule oldModule, java.lang.AutoCloseable oldInstance) {

        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    protected void customValidation(){
        JmxAttributeValidationException.checkNotNull(getSleepFactor(), "value is not set.", sleepFactorJmxAttribute);
        JmxAttributeValidationException.checkCondition(getSleepFactor().doubleValue() >= 1, "value " + getSleepFactor()
                + " is less than 1", sleepFactorJmxAttribute);

        JmxAttributeValidationException.checkNotNull(getConnectTime(), "value is not set.", connectTimeJmxAttribute);
        JmxAttributeValidationException.checkCondition(getConnectTime() >= 0, "value " + getConnectTime()
                + " is less than 0", connectTimeJmxAttribute);

        JmxAttributeValidationException.checkNotNull(getMinSleep(), "value is not set.", minSleepJmxAttribute);
        JmxAttributeValidationException.checkCondition(getMaxSleep() == null || getMinSleep() <= getMaxSleep(),
                "value " + getMinSleep() + " is greter than MaxSleep " + getMaxSleep(), minSleepJmxAttribute);
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return new TimedReconnectStrategyFactoryCloseable(getTimedReconnectExecutorDependency(),
                getConnectTime(), getMinSleep(), getSleepFactor().doubleValue(), getMaxSleep(), getMaxAttempts(),
                getDeadline());
    }

    private static final class TimedReconnectStrategyFactoryCloseable implements ReconnectStrategyFactory, AutoCloseable {

        private final EventExecutor executor;
        private final Long deadline, maxAttempts, maxSleep;
        private final double sleepFactor;
        private final int connectTime;
        private final long minSleep;

        public TimedReconnectStrategyFactoryCloseable(final EventExecutor executor, final int connectTime, final long minSleep, final double sleepFactor,
                final Long maxSleep, final Long maxAttempts, final Long deadline) {
            Preconditions.checkArgument(maxSleep == null || minSleep <= maxSleep);
            Preconditions.checkArgument(sleepFactor >= 1);
            Preconditions.checkArgument(connectTime >= 0);
            this.executor = Preconditions.checkNotNull(executor);
            this.deadline = deadline;
            this.maxAttempts = maxAttempts;
            this.minSleep = minSleep;
            this.maxSleep = maxSleep;
            this.sleepFactor = sleepFactor;
            this.connectTime = connectTime;
        }

        @Override
        public void close() throws Exception {
            // no-op
        }

        @Override
        public ReconnectStrategy createReconnectStrategy() {
            return new TimedReconnectStrategy(this.executor,
                    this.connectTime, this.minSleep, this.sleepFactor, this.maxSleep, this.maxAttempts,
                    this.deadline);
        }

    }
}
