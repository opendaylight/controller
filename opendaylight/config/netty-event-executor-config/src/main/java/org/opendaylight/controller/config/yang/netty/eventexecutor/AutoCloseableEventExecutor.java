/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.netty.eventexecutor;

import com.google.common.reflect.AbstractInvocationHandler;
import com.google.common.reflect.Reflection;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.ImmediateEventExecutor;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

public interface AutoCloseableEventExecutor extends EventExecutor, AutoCloseable {


    public static class CloseableEventExecutorMixin implements AutoCloseable {
        public static final int DEFAULT_SHUTDOWN_SECONDS = 1;
        private final EventExecutor eventExecutor;

        public CloseableEventExecutorMixin(EventExecutor eventExecutor) {
            this.eventExecutor = eventExecutor;
        }

        @Override
        public void close() {
            eventExecutor.shutdownGracefully(0, DEFAULT_SHUTDOWN_SECONDS, TimeUnit.SECONDS);
        }


        private static AutoCloseableEventExecutor createCloseableProxy(final EventExecutor eventExecutor) {
            final CloseableEventExecutorMixin closeableGlobalEventExecutorMixin =
                    new CloseableEventExecutorMixin(eventExecutor);
            return Reflection.newProxy(AutoCloseableEventExecutor.class, new AbstractInvocationHandler() {
                @Override
                protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {
                    if (method.getName().equals("close")) {
                        closeableGlobalEventExecutorMixin.close();
                        return null;
                    } else {
                        return method.invoke(eventExecutor, args);
                    }
                }
            });
        }

        public static AutoCloseableEventExecutor globalEventExecutor() {
            return createCloseableProxy(GlobalEventExecutor.INSTANCE);
        }

        public static AutoCloseableEventExecutor immediateEventExecutor() {
            return createCloseableProxy(ImmediateEventExecutor.INSTANCE);
        }
    }
}