/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.jmx;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.concurrent.GuardedBy;
import javax.management.InstanceAlreadyExistsException;
import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.QueryExp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InternalJMXRegistrator implements Closeable {
    private static final Logger logger = LoggerFactory
            .getLogger(InternalJMXRegistrator.class);
    private final MBeanServer configMBeanServer;

    public InternalJMXRegistrator(MBeanServer configMBeanServer) {
        this.configMBeanServer = configMBeanServer;
    }

    static class InternalJMXRegistration implements AutoCloseable {
        private final InternalJMXRegistrator internalJMXRegistrator;
        private final ObjectName on;

        InternalJMXRegistration(InternalJMXRegistrator internalJMXRegistrator,
                ObjectName on) {
            this.internalJMXRegistrator = internalJMXRegistrator;
            this.on = on;
        }

        @Override
        public void close() {
            internalJMXRegistrator.unregisterMBean(on);
        }
    }

    @GuardedBy("this")
    private final Set<ObjectName> registeredObjectNames = new HashSet<>();
    private final List<InternalJMXRegistrator> children = new ArrayList<>();

    public synchronized InternalJMXRegistration registerMBean(Object object,
            ObjectName on) throws InstanceAlreadyExistsException {
        try {
            configMBeanServer.registerMBean(object, on);
        } catch (InstanceAlreadyExistsException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        registeredObjectNames.add(on);
        return new InternalJMXRegistration(this, on);
    }

    private synchronized void unregisterMBean(ObjectName on) {
        // first check that on was registered using this instance
        boolean removed = registeredObjectNames.remove(on);
        if (!removed)
            throw new IllegalStateException(
                    "Cannot unregister - ObjectName not found in 'registeredObjectNames': "
                            + on);
        try {
            configMBeanServer.unregisterMBean(on);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public InternalJMXRegistrator createChild() {
        InternalJMXRegistrator child = new InternalJMXRegistrator(
                configMBeanServer);
        children.add(child);
        return child;
    }

    /**
     * Allow close to be called multiple times.
     */
    @Override
    public synchronized void close() {
        // close children
        for (InternalJMXRegistrator child : children) {
            child.close();
        }
        // close registered ONs
        for (ObjectName on : registeredObjectNames) {
            try {
                configMBeanServer.unregisterMBean(on);
            } catch (Exception e) {
                logger.warn("Ignoring error while unregistering {}", on, e);
            }
        }
        registeredObjectNames.clear();
    }

    public <T> T newMBeanProxy(ObjectName objectName, Class<T> interfaceClass) {
        return JMX.newMBeanProxy(configMBeanServer, objectName, interfaceClass);
    }

    public <T> T newMBeanProxy(ObjectName objectName, Class<T> interfaceClass,
            boolean notificationBroadcaster) {
        return JMX.newMBeanProxy(configMBeanServer, objectName, interfaceClass,
                notificationBroadcaster);
    }

    public <T> T newMXBeanProxy(ObjectName objectName, Class<T> interfaceClass) {
        return JMX
                .newMXBeanProxy(configMBeanServer, objectName, interfaceClass);
    }

    public <T> T newMXBeanProxy(ObjectName objectName, Class<T> interfaceClass,
            boolean notificationBroadcaster) {
        return JMX.newMXBeanProxy(configMBeanServer, objectName,
                interfaceClass, notificationBroadcaster);
    }

    public Set<ObjectName> getRegisteredObjectNames() {
        return Collections.unmodifiableSet(registeredObjectNames);
    }

    public Set<ObjectName> queryNames(ObjectName name, QueryExp query) {
        Set<ObjectName> result = configMBeanServer.queryNames(name, query);
        // keep only those that were registered using this instance
        return getSameNames(result);
    }

    private Set<ObjectName> getSameNames(Set<ObjectName> superSet) {
        Set<ObjectName> result = new HashSet<>(superSet);
        result.retainAll(registeredObjectNames);
        for (InternalJMXRegistrator child : children) {
            result.addAll(child.getSameNames(superSet));
        }
        return result;
    }

}
