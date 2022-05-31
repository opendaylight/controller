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
    static AutoCloseableEventExecutor globalEventExecutor() {
        return createCloseableProxy(GlobalEventExecutor.INSTANCE);
    }

    static AutoCloseableEventExecutor immediateEventExecutor() {
        return createCloseableProxy(ImmediateEventExecutor.INSTANCE);
    }

    private static AutoCloseableEventExecutor createCloseableProxy(final EventExecutor eventExecutor) {
        return Reflection.newProxy(AutoCloseableEventExecutor.class, new AbstractInvocationHandler() {
            @Override
            protected Object handleInvocation(final Object proxy, final Method method, final Object[] args)
                throws Throwable {
                if (method.getName().equals("close")) {
                    eventExecutor.shutdownGracefully(0, 1, TimeUnit.SECONDS);
                    return null;
                } else {
                    return method.invoke(eventExecutor, args);
                }
            }
        });
    }
}
