/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.jmx;

import com.google.common.base.Preconditions;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.concurrent.GuardedBy;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.JMX;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.QueryExp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InternalJMXRegistrator implements Closeable {
    private static final Logger LOG = LoggerFactory
            .getLogger(InternalJMXRegistrator.class);
    private final MBeanServer configMBeanServer;
    private final InternalJMXRegistrator parent;

    public InternalJMXRegistrator(final MBeanServer configMBeanServer) {
        this.configMBeanServer = configMBeanServer;
        this.parent = null;
    }

    private InternalJMXRegistrator(final InternalJMXRegistrator parent) {
        this.parent = Preconditions.checkNotNull(parent);
        this.configMBeanServer = parent.configMBeanServer;
    }

    static class InternalJMXRegistration implements AutoCloseable {
        private final InternalJMXRegistrator internalJMXRegistrator;
        private final ObjectName on;

        InternalJMXRegistration(final InternalJMXRegistrator internalJMXRegistrator,
                                final ObjectName on) {
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
    @GuardedBy("this")
    private final List<InternalJMXRegistrator> children = new ArrayList<>();

    public synchronized InternalJMXRegistration registerMBean(final Object object,
                                                              final ObjectName on) throws InstanceAlreadyExistsException {
        try {
            configMBeanServer.registerMBean(object, on);
        } catch (MBeanRegistrationException | NotCompliantMBeanException e) {
            throw new IllegalStateException(e);
        }
        registeredObjectNames.add(on);
        return new InternalJMXRegistration(this, on);
    }

    private synchronized void unregisterMBean(final ObjectName on) {
        // first check that on was registered using this instance
        boolean removed = registeredObjectNames.remove(on);
        if (!removed) {
            throw new IllegalStateException("Cannot unregister - ObjectName not found in 'registeredObjectNames': " + on);
        }
        try {
            configMBeanServer.unregisterMBean(on);
        } catch (InstanceNotFoundException | MBeanRegistrationException e) {
            throw new IllegalStateException(e);
        }
    }

    public synchronized InternalJMXRegistrator createChild() {
        InternalJMXRegistrator child = new InternalJMXRegistrator(this);
        children.add(child);
        return child;
    }

    private synchronized void removeChild(final InternalJMXRegistrator child) {
        children.remove(child);
    }

    /**
     * Allow close to be called multiple times.
     */
    @Override
    public synchronized void close() {
        // close all children
        while (!children.isEmpty()) {
            final InternalJMXRegistrator child = children.get(0);

            // This cascade and remove the child from the list via removeChild()
            child.close();
        }

        // close registered ONs
        for (ObjectName on : registeredObjectNames) {
            try {
                configMBeanServer.unregisterMBean(on);
            } catch (Exception e) {
                LOG.warn("Ignoring error while unregistering {}", on, e);
            }
        }
        registeredObjectNames.clear();

        if (parent != null) {
            parent.removeChild(this);
        }
    }

    public <T> T newMBeanProxy(final ObjectName objectName, final Class<T> interfaceClass) {
        return JMX.newMBeanProxy(configMBeanServer, objectName, interfaceClass);
    }

    public <T> T newMBeanProxy(final ObjectName objectName, final Class<T> interfaceClass,
                               final boolean notificationBroadcaster) {
        return JMX.newMBeanProxy(configMBeanServer, objectName, interfaceClass,
                notificationBroadcaster);
    }

    public <T> T newMXBeanProxy(final ObjectName objectName, final Class<T> interfaceClass) {
        return JMX
                .newMXBeanProxy(configMBeanServer, objectName, interfaceClass);
    }

    public <T> T newMXBeanProxy(final ObjectName objectName, final Class<T> interfaceClass,
                                final boolean notificationBroadcaster) {
        return JMX.newMXBeanProxy(configMBeanServer, objectName,
                interfaceClass, notificationBroadcaster);
    }

    public Set<ObjectName> getRegisteredObjectNames() {
        return Collections.unmodifiableSet(registeredObjectNames);
    }

    public Set<ObjectName> queryNames(final ObjectName name, final QueryExp query) {
        Set<ObjectName> result = configMBeanServer.queryNames(name, query);
        // keep only those that were registered using this instance
        return getSameNames(result);
    }

    private synchronized Set<ObjectName> getSameNames(final Set<ObjectName> superSet) {
        Set<ObjectName> result = new HashSet<>(superSet);
        result.retainAll(registeredObjectNames);
        for (InternalJMXRegistrator child : children) {
            result.addAll(child.getSameNames(superSet));
        }
        return result;
    }

}
