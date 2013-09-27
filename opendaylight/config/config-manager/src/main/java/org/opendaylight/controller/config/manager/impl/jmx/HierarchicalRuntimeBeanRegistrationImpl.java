/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.jmx;

import java.util.HashMap;
import java.util.Map;

import javax.management.InstanceAlreadyExistsException;
import javax.management.ObjectName;

import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.api.runtime.HierarchicalRuntimeBeanRegistration;
import org.opendaylight.controller.config.api.runtime.RuntimeBean;

public class HierarchicalRuntimeBeanRegistrationImpl implements
        HierarchicalRuntimeBeanRegistration {
    private final ModuleIdentifier moduleIdentifier;
    private final InternalJMXRegistrator internalJMXRegistrator;
    private final Map<String, String> properties;

    public HierarchicalRuntimeBeanRegistrationImpl(
            ModuleIdentifier moduleIdentifier,
            InternalJMXRegistrator internalJMXRegistrator,
            Map<String, String> properties) {
        this.moduleIdentifier = moduleIdentifier;
        this.internalJMXRegistrator = internalJMXRegistrator;
        this.properties = properties;
    }

    @Override
    public ObjectName getObjectName() {
        return ObjectNameUtil.createRuntimeBeanName(
                moduleIdentifier.getFactoryName(),
                moduleIdentifier.getInstanceName(), properties);
    }

    @Override
    public HierarchicalRuntimeBeanRegistrationImpl register(String key,
            String value, RuntimeBean mxBean) {
        Map<String, String> currentProperties = new HashMap<>(properties);
        currentProperties.put(key, value);
        ObjectName on = ObjectNameUtil.createRuntimeBeanName(
                moduleIdentifier.getFactoryName(),
                moduleIdentifier.getInstanceName(), currentProperties);
        InternalJMXRegistrator child = internalJMXRegistrator.createChild();
        try {
            child.registerMBean(mxBean, on);
        } catch (InstanceAlreadyExistsException e) {
            throw RootRuntimeBeanRegistratorImpl.sanitize(e, moduleIdentifier,
                    on);
        }
        return new HierarchicalRuntimeBeanRegistrationImpl(moduleIdentifier,
                child, currentProperties);
    }

    @Override
    public void close() {
        internalJMXRegistrator.close();
    }
}
