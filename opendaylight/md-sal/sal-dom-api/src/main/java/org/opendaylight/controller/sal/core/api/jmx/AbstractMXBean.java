/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.core.api.jmx;

import java.lang.management.ManagementFactory;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public abstract class AbstractMXBean {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractMXBean.class);

    public static String BASE_JMX_PREFIX = "org.opendaylight.controller:";

    private final MBeanServer server = ManagementFactory.getPlatformMBeanServer();

    private final String mBeanName;
    private final String mBeanType;
    private final String mBeanCategory;

    protected AbstractMXBean(String mBeanName, String mBeanType, String mBeanCategory) {
        this.mBeanName = mBeanName;
        this.mBeanType = mBeanType;
        this.mBeanCategory = mBeanCategory;
    }

    protected ObjectName getMBeanObjectName() throws MalformedObjectNameException {
        StringBuilder builder = new StringBuilder(BASE_JMX_PREFIX)
                .append("type=").append(getMBeanType());

        if(getMBeanCategory() != null) {
            builder.append(",Category=").append(getMBeanCategory());
        }

        builder.append(",name=").append(getMBeanName());
        return new ObjectName(builder.toString());
    }

    public boolean registerMBean() {
        boolean registered = false;
        try {
            // Object to identify MBean
            final ObjectName mbeanName = this.getMBeanObjectName();

            Preconditions.checkArgument(mbeanName != null,
                    "Object name of the MBean cannot be null");

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

            LOG.debug("MBean {} registered successfully", mbeanName.getCanonicalName());
            registered = true;
        } catch(Exception e) {

            LOG.error("registration failed {}", e);

        }
        return registered;
    }

    public boolean unregisterMBean() {
        boolean unregister = false;
        try {
            ObjectName mbeanName = this.getMBeanObjectName();
            unregister = true;
            unregisterMBean(mbeanName);
        } catch(Exception e) {

            LOG.error("Failed when unregistering MBean {}", e);
        }

        return unregister;
    }

    private void unregisterMBean(ObjectName mbeanName) throws MBeanRegistrationException,
            InstanceNotFoundException {
        server.unregisterMBean(mbeanName);
    }

    public String getMBeanName() {
        return mBeanName;
    }

    public String getMBeanType() {
        return mBeanType;
    }

    public String getMBeanCategory() {
        return mBeanCategory;
    }

    public MBeanServer getMBeanServer() {
        return server;
    }
}
