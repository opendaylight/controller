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
import org.opendaylight.protocol.framework.ReconnectImmediatelyStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;

/**
*
*/
@Deprecated
public final class ReconnectImmediatelyStrategyFactoryModule extends org.opendaylight.controller.config.yang.protocol.framework.AbstractReconnectImmediatelyStrategyFactoryModule
 {

    public ReconnectImmediatelyStrategyFactoryModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public ReconnectImmediatelyStrategyFactoryModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            ReconnectImmediatelyStrategyFactoryModule oldModule, java.lang.AutoCloseable oldInstance) {

        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    protected void customValidation(){
        JmxAttributeValidationException.checkNotNull(getReconnectTimeout(), "value is not set.", reconnectTimeoutJmxAttribute);
        JmxAttributeValidationException.checkCondition(getReconnectTimeout() >= 0, "value " + getReconnectTimeout() + " is less than 0",
                reconnectTimeoutJmxAttribute);
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return new ReconnectImmediatelyStrategyFactoryCloseable(getReconnectExecutorDependency(), getReconnectTimeout());
    }

    private static final class ReconnectImmediatelyStrategyFactoryCloseable implements ReconnectStrategyFactory, AutoCloseable {

        private final EventExecutor executor;
        private final int timeout;

        public ReconnectImmediatelyStrategyFactoryCloseable(final EventExecutor executor, final int timeout) {
            this.executor = executor;
            this.timeout = timeout;
        }

        @Override
        public void close() throws Exception {
            // no-op
        }

        @Override
        public ReconnectStrategy createReconnectStrategy() {
            return new ReconnectImmediatelyStrategy(this.executor, this.timeout);
        }

    }
}
