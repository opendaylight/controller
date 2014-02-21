/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.netty.eventexecutor;

import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.ImmediateEventExecutor;
import org.opendaylight.controller.config.yang.netty.eventexecutor.AutoCloseableEventExecutor.CloseableEventExecutorMixin;

public final class ImmediateEventExecutorModule extends org.opendaylight.controller.config.yang.netty.eventexecutor.AbstractImmediateEventExecutorModule {

    public ImmediateEventExecutorModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public ImmediateEventExecutorModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
                                        ImmediateEventExecutorModule oldModule, java.lang.AutoCloseable oldInstance) {

        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    protected void customValidation() {
        // Add custom validation for module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        EventExecutor eventExecutor = ImmediateEventExecutor.INSTANCE;
        return CloseableEventExecutorMixin.createCloseableProxy(eventExecutor);
    }
}
