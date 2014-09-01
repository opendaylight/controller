/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.common.util.jmx;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.management.MalformedObjectNameException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.Beta;

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

    /**
     * Registers this bean with the platform MBean server with the domain defined by
     * {@link #BASE_JMX_PREFIX}.
     *
     * @return true is successfully registered, false otherwise.
     */
    public boolean registerMBean() {
        try {
            return MBeanRegistrar.registerMBean(this, MBeanRegistrar.buildMBeanObjectName(
                    getMBeanName(), getMBeanType(), getMBeanCategory()));
        } catch(MalformedObjectNameException e) {
            LOG.error("Error building MBean ObjectName", e);
            return false;
        }
    }

    /**
     * Unregisters this bean with the platform MBean server.
     *
     * @return true is successfully unregistered, false otherwise.
     */
    public boolean unregisterMBean() {
        try {
            return MBeanRegistrar.unregisterMBean(MBeanRegistrar.buildMBeanObjectName(
                    getMBeanName(), getMBeanType(), getMBeanCategory()));
        } catch(MalformedObjectNameException e) {
            LOG.error("Error building MBean ObjectName", e);
            return false;
        }
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
