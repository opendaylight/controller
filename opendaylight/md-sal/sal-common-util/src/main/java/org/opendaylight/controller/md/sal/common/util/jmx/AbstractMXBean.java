/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.common.util.jmx;

import com.google.common.annotations.Beta;
import java.lang.management.ManagementFactory;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base for an MXBean implementation class.
 * <p>
 * This class is not intended for use outside of MD-SAL and its part of private
 * implementation (still exported as public to be reused across MD-SAL implementation
 * components) and may be removed in subsequent
 * releases.
 *
 * @author Thomas Pantelis
 */
@Beta
public abstract class AbstractMXBean {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractMXBean.class);

    public static String BASE_JMX_PREFIX = "org.opendaylight.controller:";

    private final MBeanServer server = ManagementFactory.getPlatformMBeanServer();

    private final String mBeanName;
    private final String mBeanType;
    private final String mBeanCategory;

    /**
     * Constructor.
     *
     * @param mBeanName Used as the <code>name</code> property in the bean's ObjectName.
     * @param mBeanType Used as the <code>type</code> property in the bean's ObjectName.
     * @param mBeanCategory Used as the <code>Category</code> property in the bean's ObjectName.
     */
    protected AbstractMXBean(@Nonnull String mBeanName, @Nonnull String mBeanType,
            @Nullable String mBeanCategory) {
        this.mBeanName = mBeanName;
        this.mBeanType = mBeanType;
        this.mBeanCategory = mBeanCategory;
    }

    private ObjectName getMBeanObjectName() throws MalformedObjectNameException {
        StringBuilder builder = new StringBuilder(BASE_JMX_PREFIX)
                .append("type=").append(getMBeanType());

        if(getMBeanCategory() != null) {
            builder.append(",Category=").append(getMBeanCategory());
        }

        builder.append(",name=").append(getMBeanName());
        return new ObjectName(builder.toString());
    }

    /**
     * Registers this bean with the platform MBean server with the domain defined by
     * {@link #BASE_JMX_PREFIX}.
     *
     * @return true is successfully registered, false otherwise.
     */
    public boolean registerMBean() {
        boolean registered = false;
        try {
            // Object to identify MBean
            final ObjectName mbeanName = this.getMBeanObjectName();

            LOG.debug("Register MBean {}", mbeanName);

            // unregistered if already registered
            if(server.isRegistered(mbeanName)) {

                LOG.debug("MBean {} found to be already registered", mbeanName);

                try {
                    unregisterMBean(mbeanName);
                } catch(Exception e) {

                    LOG.warn("unregister mbean {} resulted in exception {} ", mbeanName, e);
                }
            }
            server.registerMBean(this, mbeanName);
            registered = true;

            LOG.debug("MBean {} registered successfully", mbeanName.getCanonicalName());
        } catch(Exception e) {

            LOG.error("registration failed {}", e);

        }
        return registered;
    }

    /**
     * Unregisters this bean with the platform MBean server.
     *
     * @return true is successfully unregistered, false otherwise.
     */
    public boolean unregisterMBean() {
        boolean unregister = false;
        try {
            ObjectName mbeanName = this.getMBeanObjectName();
            unregisterMBean(mbeanName);
            unregister = true;
        } catch(Exception e) {
            LOG.debug("Failed when unregistering MBean {}", e);
        }

        return unregister;
    }

    private void unregisterMBean(ObjectName mbeanName) throws MBeanRegistrationException,
            InstanceNotFoundException {
        server.unregisterMBean(mbeanName);
    }

    /**
     * Returns the <code>name</code> property of the bean's ObjectName.
     */
    public String getMBeanName() {
        return mBeanName;
    }

    /**
     * Returns the <code>type</code> property of the bean's ObjectName.
     */
    public String getMBeanType() {
        return mBeanType;
    }

    /**
     * Returns the <code>Category</code> property of the bean's ObjectName.
     */
    public String getMBeanCategory() {
        return mBeanCategory;
    }
}
