/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.util.jmx;

import static java.util.Objects.requireNonNull;

import java.lang.management.ManagementFactory;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base for an MXBean implementation class. This class is not intended for use outside of MD-SAL and its part
 * of private implementation (still exported as public to be reused across MD-SAL implementation components) and may be
 * removed in subsequent releases.
 *
 * @author Thomas Pantelis
 */
public abstract class AbstractMXBean {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractMXBean.class);

    public static final String BASE_JMX_PREFIX = "org.opendaylight.controller:";

    private final MBeanServer server = ManagementFactory.getPlatformMBeanServer();

    private final String beanName;
    private final String beanType;
    private final String beanCategory;

    /**
     * Constructor.
     *
     * @param beanName Used as the <code>name</code> property in the bean's ObjectName.
     * @param beanType Used as the <code>type</code> property in the bean's ObjectName.
     * @param beanCategory Used as the <code>Category</code> property in the bean's ObjectName.
     */
    protected AbstractMXBean(final @NonNull String beanName, final @NonNull String beanType,
            final @Nullable String beanCategory) {
        this.beanName = requireNonNull(beanName);
        this.beanType = requireNonNull(beanType);
        this.beanCategory = beanCategory;
    }

    private ObjectName getMBeanObjectName() throws MalformedObjectNameException {
        final var sb = new StringBuilder(BASE_JMX_PREFIX).append("type=").append(beanType);
        if (beanCategory != null) {
            sb.append(",Category=").append(beanCategory);
        }
        return new ObjectName(sb.append(",name=").append(beanName).toString());
    }

    /**
     * This method is a wrapper for registerMBean with void return type so it can be invoked by dependency
     * injection frameworks such as Spring and Blueprint.
     */
    public final void register() {
        registerMBean();
    }

    /**
     * Registers this bean with the platform MBean server with the domain defined by
     * {@link #BASE_JMX_PREFIX}.
     *
     * @return true is successfully registered, false otherwise.
     */
    public final boolean registerMBean() {
        boolean registered = false;
        try {
            // Object to identify MBean
            final ObjectName mbeanName = getMBeanObjectName();

            LOG.debug("Register MBean {}", mbeanName);

            // unregistered if already registered
            if (server.isRegistered(mbeanName)) {

                LOG.debug("MBean {} found to be already registered", mbeanName);

                try {
                    unregisterMBean(mbeanName);
                } catch (MBeanRegistrationException | InstanceNotFoundException e) {
                    LOG.warn("unregister mbean {} resulted in exception", mbeanName, e);
                }
            }
            server.registerMBean(this, mbeanName);
            registered = true;

            LOG.debug("MBean {} registered successfully", mbeanName.getCanonicalName());
        } catch (InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException
                | MalformedObjectNameException e) {
            LOG.error("registration failed", e);
        }
        return registered;
    }

    /**
     * This method is a wrapper for unregisterMBean with void return type so it can be invoked by dependency
     * injection frameworks such as Spring and Blueprint.
     */
    public final void unregister() {
        unregisterMBean();
    }

    /**
     * Unregisters this bean with the platform MBean server.
     *
     * @return true is successfully unregistered, false otherwise.
     */
    public final boolean unregisterMBean() {
        try {
            unregisterMBean(getMBeanObjectName());
            return true;
        } catch (MBeanRegistrationException | InstanceNotFoundException | MalformedObjectNameException e) {
            LOG.debug("Failed when unregistering MBean", e);
            return false;
        }
    }

    private void unregisterMBean(final ObjectName mbeanName) throws MBeanRegistrationException,
            InstanceNotFoundException {
        server.unregisterMBean(mbeanName);
    }

    /**
     * Returns the <code>name</code> property of the bean's ObjectName.
     */
    public final String getMBeanName() {
        return beanName;
    }

    /**
     * Returns the <code>type</code> property of the bean's ObjectName.
     */
    public final String getMBeanType() {
        return beanType;
    }

    /**
     * Returns the <code>Category</code> property of the bean's ObjectName.
     */
    public final String getMBeanCategory() {
        return beanCategory;
    }
}
