/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.dynamicmbean;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ReflectionException;

import org.opendaylight.controller.config.api.DynamicMBeanWithInstance;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.spi.Module;

/**
 * Wraps {@link org.opendaylight.controller.config.spi.Module} in a
 * {@link DynamicMBeanWithInstance}. Setting attributes is disabled.
 */
public class DynamicReadableWrapper extends AbstractDynamicWrapper implements
        DynamicMBeanWithInstance {
    private final AutoCloseable instance;

    /**
     * @param module
     * @param instance
     *            for recreating Module.
     *
     */
    public DynamicReadableWrapper(Module module, AutoCloseable instance,
            ModuleIdentifier moduleIdentifier, MBeanServer internalServer,
            MBeanServer configMBeanServer) {
        super(module, false, moduleIdentifier, ObjectNameUtil
                .createReadOnlyModuleON(moduleIdentifier),
                getEmptyOperations(), internalServer, configMBeanServer);
        this.instance = instance;
    }

    @Override
    public Module getModule() {
        return module;
    }

    @Override
    public AutoCloseable getInstance() {
        return instance;
    }

    @Override
    public Object invoke(String actionName, Object[] params, String[] signature)
            throws MBeanException, ReflectionException {
        if ("getInstance".equals(actionName)
                && (params == null || params.length == 0)
                && (signature == null || signature.length == 0)) {
            return getInstance();
        }
        return super.invoke(actionName, params, signature);
    }

    @Override
    public Object getAttribute(String attributeName)
            throws AttributeNotFoundException, MBeanException,
            ReflectionException {
        if (attributeName.equals("getInstance")) {
            return getInstance();
        }
        return super.getAttribute(attributeName);
    }

    @Override
    public void setAttribute(Attribute attribute)
            throws AttributeNotFoundException, InvalidAttributeValueException,
            MBeanException, ReflectionException {
        throw new UnsupportedOperationException(
                "setAttributes is not supported on " + moduleIdentifier);
    }

    @Override
    public AttributeList setAttributes(AttributeList attributes) {
        throw new UnsupportedOperationException(
                "setAttributes is not supported on " + moduleIdentifier);
    }
}
