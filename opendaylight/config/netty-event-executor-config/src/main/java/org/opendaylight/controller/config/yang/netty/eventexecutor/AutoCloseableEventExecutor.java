package org.opendaylight.controller.config.yang.netty.eventexecutor;

import com.google.common.reflect.AbstractInvocationHandler;
import com.google.common.reflect.Reflection;
import io.netty.util.concurrent.EventExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

public interface AutoCloseableEventExecutor extends EventExecutor, AutoCloseable {


    public static class CloseableEventExecutorMixin implements AutoCloseable {
        private final EventExecutor eventExecutor;

        public CloseableEventExecutorMixin(EventExecutor eventExecutor) {
            this.eventExecutor = eventExecutor;
        }

        @Override
        public void close() {
            eventExecutor.shutdownGracefully(0, 1, TimeUnit.SECONDS);
        }


        public static AutoCloseable createCloseableProxy(final EventExecutor eventExecutor) {
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


    }
}