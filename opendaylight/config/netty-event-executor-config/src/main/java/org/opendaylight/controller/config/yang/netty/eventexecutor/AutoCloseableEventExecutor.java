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


    class CloseableEventExecutorMixin implements AutoCloseable {
        public static final int DEFAULT_SHUTDOWN_SECONDS = 1;
        private final EventExecutor eventExecutor;

        public CloseableEventExecutorMixin(EventExecutor eventExecutor) {
            this.eventExecutor = eventExecutor;
        }

        @Override
        public void close() throws Exception {
            eventExecutor.shutdownGracefully(0, DEFAULT_SHUTDOWN_SECONDS, TimeUnit.SECONDS);
        }


        private static AutoCloseableEventExecutor createCloseableProxy(
                final CloseableEventExecutorMixin closeableEventExecutorMixin) {
            return Reflection.newProxy(AutoCloseableEventExecutor.class, new AbstractInvocationHandler() {
                @Override
                protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {
                    if (method.getName().equals("close")) {
                        closeableEventExecutorMixin.close();
                        return null;
                    } else {
                        return method.invoke(closeableEventExecutorMixin.eventExecutor, args);
                    }
                }
            });
        }

        public static AutoCloseableEventExecutor globalEventExecutor() {
            return createCloseableProxy(new CloseableEventExecutorMixin(GlobalEventExecutor.INSTANCE));
        }

        public static AutoCloseableEventExecutor immediateEventExecutor() {
            return createCloseableProxy(new CloseableEventExecutorMixin(ImmediateEventExecutor.INSTANCE));
        }

        public static AutoCloseableEventExecutor forwardingEventExecutor(final EventExecutor eventExecutor,
                final AutoCloseable closeable) {
            return createCloseableProxy(new CloseableEventExecutorMixin(eventExecutor) {
                @Override
                public void close() throws Exception {
                    // Intentional no-op.
                    closeable.close();
                }
            });
        }
    }
}