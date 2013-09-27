/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.jmx;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;

import org.opendaylight.controller.config.api.jmx.ConfigRegistryMXBean;
import org.opendaylight.controller.config.manager.impl.ConfigRegistryImplMXBean;

/**
 * This registrator is used only to register Config Registry to JMX.
 *
 */
public class ConfigRegistryJMXRegistrator implements AutoCloseable {
    private final InternalJMXRegistrator internalJMXRegistrator;

    public ConfigRegistryJMXRegistrator(MBeanServer configMBeanServer) {
        internalJMXRegistrator = new InternalJMXRegistrator(configMBeanServer);
    }

    public AutoCloseable registerToJMX(ConfigRegistryImplMXBean configRegistry)
            throws InstanceAlreadyExistsException {
        return internalJMXRegistrator.registerMBean(configRegistry,
                ConfigRegistryMXBean.OBJECT_NAME);
    }

    @Override
    public void close() {
        internalJMXRegistrator.close();
    }
}
