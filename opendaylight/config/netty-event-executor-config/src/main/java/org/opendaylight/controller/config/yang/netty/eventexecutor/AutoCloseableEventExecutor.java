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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.ImmediateEventExecutor;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

public interface AutoCloseableEventExecutor extends EventExecutor, AutoCloseable {

    static AutoCloseableEventExecutor globalEventExecutor() {
        return CloseableEventExecutorMixin.createCloseableProxy(GlobalEventExecutor.INSTANCE);
    }

    static AutoCloseableEventExecutor immediateEventExecutor() {
        return CloseableEventExecutorMixin.createCloseableProxy(ImmediateEventExecutor.INSTANCE);
    }

    class CloseableEventExecutorMixin implements AutoCloseable {
        public static final int DEFAULT_SHUTDOWN_SECONDS = 1;
        private final EventExecutor eventExecutor;

        public CloseableEventExecutorMixin(final EventExecutor eventExecutor) {
            this.eventExecutor = eventExecutor;
        }

        @Override
        @SuppressFBWarnings(value = "UC_USELESS_VOID_METHOD", justification = "False positive")
        public void close() {
            eventExecutor.shutdownGracefully(0, DEFAULT_SHUTDOWN_SECONDS, TimeUnit.SECONDS);
        }

        static AutoCloseableEventExecutor createCloseableProxy(final EventExecutor eventExecutor) {
            final CloseableEventExecutorMixin closeableEventExecutor = new CloseableEventExecutorMixin(eventExecutor);
            return Reflection.newProxy(AutoCloseableEventExecutor.class, new AbstractInvocationHandler() {
                @Override
                protected Object handleInvocation(final Object proxy, final Method method, final Object[] args)
                        throws Throwable {
                    if (method.getName().equals("close")) {
                        closeableEventExecutor.close();
                        return null;
                    } else {
                        return method.invoke(closeableEventExecutor.eventExecutor, args);
                    }
                }
            });
        }
    }
}
