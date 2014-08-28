/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.common.util.jmx;

import java.lang.management.ManagementFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains methods to register and unregister MBeans with the platporm server.
 *
 * @author Thomas Pantelis
 */
public final class MBeanRegistrar {

    private static final Logger LOG = LoggerFactory.getLogger(MBeanRegistrar.class);

    private static final MBeanServer MB_SERVER = ManagementFactory.getPlatformMBeanServer();

    public static final String BASE_JMX_PREFIX = "org.opendaylight.controller:";

    private MBeanRegistrar() {
    }

    /**
     * Build an MBean ObjectName with the {@link #BASE_JMX_PREFIX}.
     *
     * @param mBeanName Used as the <code>name</code> property in the bean's ObjectName
     * @param mBeanType Used as the <code>type</code> property in the bean's ObjectName
     * @param mBeanCategory Used as the <code>Category</code> property in the bean's ObjectName
     * @return true is successfully registered, false otherwise.
     */
    public static ObjectName buildMBeanObjectName(@Nonnull String mBeanName,
            @Nonnull String mBeanType, @Nullable String mBeanCategory) throws MalformedObjectNameException {
        StringBuilder builder = new StringBuilder(BASE_JMX_PREFIX).append("type=").append(mBeanType);

        if(mBeanCategory != null) {
            builder.append(",Category=").append(mBeanCategory);
        }

        builder.append(",name=").append(mBeanName);
        return new ObjectName(builder.toString());
    }

    /**
     * Registers an MBean with the platform MBean server with the domain defined by
     * {@link #BASE_JMX_PREFIX}.
     *
     * @param bean the MBean instance to register
     * @param mBeanName Used as the <code>name</code> property in the bean's ObjectName
     * @param mBeanType Used as the <code>type</code> property in the bean's ObjectName
     * @param mBeanCategory Used as the <code>Category</code> property in the bean's ObjectName
     * @return true is successfully registered, false otherwise.
     */
    public static boolean registerMBean(@Nonnull Object bean, @Nonnull ObjectName objectName) {
        boolean registered = false;
        try {

            LOG.debug("Register MBean {}", objectName);

            // unregistered if already registered
            if(MB_SERVER.isRegistered(objectName)) {

                LOG.debug("MBean {} found to be already registered", objectName);

                try {
                    unregisterMBean(objectName);
                } catch(Exception e) {

                    LOG.warn("unregister mbean {} resulted in exception {} ", objectName, e);
                }
            }

            MB_SERVER.registerMBean(bean, objectName);
            registered = true;

            LOG.debug("MBean {} registered successfully", objectName.getCanonicalName());
        } catch(Exception e) {
            LOG.error("MBean registration failed", e);
        }

        return registered;
    }

    /**
     * Unregisters an MBean with the platform MBean server.
     *
     * @return true is successfully unregistered, false otherwise.
     */
    public static boolean unregisterMBean(ObjectName objectName) {
        if(objectName == null) {
            return false;
        }

        try {
            MB_SERVER.unregisterMBean(objectName);
            return true;
        } catch(Exception e) {
            LOG.error("Failed when unregistering MBean", e);
        }

        return false;
    }
}
