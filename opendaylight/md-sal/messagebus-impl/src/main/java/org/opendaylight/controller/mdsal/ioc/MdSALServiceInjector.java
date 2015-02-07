/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.mdsal.ioc;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.core.api.AbstractProvider;
import org.opendaylight.controller.sal.core.api.Broker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MdSALServiceInjector {
    private static final Logger LOGGER = LoggerFactory.getLogger(MdSALServiceInjector.class);

    public static class BindingAware implements BindingAwareProvider, AutoCloseable {
        private final Map<Class<?>, Object> injectionContext;

        public BindingAware(Map<Class<?>, Object> injectionContext) {
            this.injectionContext = injectionContext;
        }

        @Override
        public void onSessionInitiated(BindingAwareBroker.ProviderContext session) {
            MdSAL mdSAL = getMdSAL(injectionContext);
            mdSAL.setBindingAwareContext(session);

            if (mdSAL.isReady()) {
                naiveInject(injectionContext);
                initializeContext(injectionContext);
            }

            LOGGER.info("BindingAwareBroker.ProviderContext initialized");
        }

        @Override
        public void close() throws Exception {}
    }

    public static class BindingIndependent extends AbstractProvider implements AutoCloseable {
        private final Map<Class<?>, Object> injectionContext;

        public BindingIndependent(Map<Class<?>, Object> injectionContext) {
            this.injectionContext = injectionContext;
        }

        @Override
        public void onSessionInitiated(Broker.ProviderSession session) {
            MdSAL mdSAL = getMdSAL(injectionContext);
            mdSAL.setBindingIndependentContext(session);

            if (mdSAL.isReady()) {
                naiveInject(injectionContext);
                initializeContext(injectionContext);
            }

            LOGGER.info("Broker.ProviderSession initialized");
        }

        @Override
        public void close() throws Exception {}
    }

    private static void initializeContext(Map<Class<?>, Object> injectionContext) {
        // Cyclomatic complexity or arrow antipattern !
        for (Object injectable : injectionContext.values()) {
            for (Method method : injectable.getClass().getMethods()) {
                if (method.isAnnotationPresent(Callback.class)) {
                    // TODO: for a while ignoring event type
                    try {
                        method.invoke(injectable);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    private static synchronized MdSAL getMdSAL(Map<Class<?>, Object> injectionContext) {
        if (injectionContext.containsKey(MdSAL.class) == false) {
            injectionContext.put(MdSAL.class, new MdSAL());
        }

        return (MdSAL)injectionContext.get(MdSAL.class);
    }

    /**
     * Naive implementation of IOC
     * So far injector works directly on implementation instead of interfaces
     * After injection concept will be investigated further, implementation
     * can be enhanced.
     *
     * @param injectionContext
     */
    public static void naiveInject(Map<Class<?>, Object> injectionContext) {
        for (Object injectable : injectionContext.values()) {
            Field[] fields = injectable.getClass().getDeclaredFields();

            for (Field field : fields) {
                if ( field.isAnnotationPresent(Inject.class) ) {
                    Object value = injectionContext.get(field.getType());
                    setValue(injectable, field, value);
                }
            }
        }
    }

    private static void setValue(Object injectable, Field field, Object value) {
        boolean accessible = field.isAccessible();
        field.setAccessible(true);

        try {
            if (field.get(injectable) == null) {
                field.set(injectable, value);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } finally {
            field.setAccessible(accessible);
        }
    }
}
