/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.jmx;

import com.google.common.base.Preconditions;
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

abstract class InternalJMXRegistrator implements AutoCloseable {
    private static final class Root extends InternalJMXRegistrator {
        private final MBeanServer mbeanServer;

        Root(final MBeanServer mbeanServer) {
            this.mbeanServer = Preconditions.checkNotNull(mbeanServer);
        }

        @Override
        MBeanServer getMBeanServer() {
            return mbeanServer;
        }
    }

    private static final class Nested extends InternalJMXRegistrator {
        private final InternalJMXRegistrator parent;

        Nested(final InternalJMXRegistrator parent) {
            this.parent = Preconditions.checkNotNull(parent);
        }

        @Override
        MBeanServer getMBeanServer() {
            return parent.getMBeanServer();
        }

        @Override
        public void close() {
            try {
                super.close();
            } finally {
                parent.removeChild(this);
            }
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(InternalJMXRegistrator.class);
    @GuardedBy("this")
    private final Set<ObjectName> registeredObjectNames = new HashSet<>(1);
    @GuardedBy("this")
    private final List<Nested> children = new ArrayList<>();

    public static InternalJMXRegistrator create(final MBeanServer configMBeanServer) {
        return new Root(configMBeanServer);
    }

    public final synchronized InternalJMXRegistration registerMBean(final Object object, final ObjectName on)
            throws InstanceAlreadyExistsException {
        try {
            getMBeanServer().registerMBean(object, on);
        } catch (NotCompliantMBeanException e) {
            throw new IllegalArgumentException("Object does not comply to JMX", e);
        } catch (MBeanRegistrationException e) {
            throw new IllegalStateException("Failed to register " + on, e);
        }

        registeredObjectNames.add(on);
        return new InternalJMXRegistration(this, on);
    }

    final synchronized void unregisterMBean(final ObjectName on) {
        // first check that on was registered using this instance
        boolean removed = registeredObjectNames.remove(on);
        Preconditions.checkState(removed, "Cannot unregister - ObjectName not found in 'registeredObjectNames': {}", on);

        try {
            getMBeanServer().unregisterMBean(on);
        } catch (InstanceNotFoundException e) {
            LOG.warn("MBean {} not found on server", on, e);
        } catch (MBeanRegistrationException e) {
            throw new IllegalStateException("Failed to unregister MBean " + on, e);
        }
    }

    public final synchronized InternalJMXRegistrator createChild() {
        final Nested child = new Nested(this);
        children.add(child);
        return child;
    }

    /**
     * Allow close to be called multiple times.
     */
    @Override
    public void close() {
        internalClose();
    }

    public final <T> T newMBeanProxy(final ObjectName objectName, final Class<T> interfaceClass) {
        return JMX.newMBeanProxy(getMBeanServer(), objectName, interfaceClass);
    }

    public final <T> T newMBeanProxy(final ObjectName objectName, final Class<T> interfaceClass,
            final boolean notificationBroadcaster) {
        return JMX.newMBeanProxy(getMBeanServer(), objectName, interfaceClass, notificationBroadcaster);
    }

    public final <T> T newMXBeanProxy(final ObjectName objectName, final Class<T> interfaceClass) {
        return JMX.newMXBeanProxy(getMBeanServer(), objectName, interfaceClass);
    }

    public final <T> T newMXBeanProxy(final ObjectName objectName, final Class<T> interfaceClass,
            final boolean notificationBroadcaster) {
        return JMX.newMXBeanProxy(getMBeanServer(), objectName, interfaceClass, notificationBroadcaster);
    }

    public final Set<ObjectName> getRegisteredObjectNames() {
        return Collections.unmodifiableSet(registeredObjectNames);
    }

    public final Set<ObjectName> queryNames(final ObjectName name, final QueryExp query) {
        // keep only those that were registered using this instance
        return getSameNames(getMBeanServer().queryNames(name, query));
    }

    abstract MBeanServer getMBeanServer();

    synchronized void removeChild(final InternalJMXRegistrator child) {
        children.remove(child);
    }

    private synchronized void internalClose() {
        // close all children
        for (InternalJMXRegistrator child : children) {
            // This bypasses the call to removeChild(), preventing a potential deadlock when children are being closed
            // concurrently
            child.internalClose();
        }
        children.clear();

        // close registered ONs
        for (ObjectName on : registeredObjectNames) {
            try {
                getMBeanServer().unregisterMBean(on);
            } catch (Exception e) {
                LOG.warn("Ignoring error while unregistering {}", on, e);
            }
        }
        registeredObjectNames.clear();
    }

    private synchronized Set<ObjectName> getSameNames(final Set<ObjectName> superSet) {
        final Set<ObjectName> result = new HashSet<>(superSet);
        result.retainAll(registeredObjectNames);

        for (InternalJMXRegistrator child : children) {
            result.addAll(child.getSameNames(superSet));
        }
        return result;
    }
}
