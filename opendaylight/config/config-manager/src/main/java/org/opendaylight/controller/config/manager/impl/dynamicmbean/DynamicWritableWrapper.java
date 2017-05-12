/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.dynamicmbean;

import java.lang.reflect.Method;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.annotations.RequireInterface;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.spi.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps {@link org.opendaylight.controller.config.spi.Module} instance in a
 * {@link DynamicMBean} interface. Inspects dependency attributes, identified by
 * ObjectName getter/setter and {@link RequireInterface} annotation. Used to
 * simplify client calls - to set a dependency, only instance name is needed.
 * This class creates new writable String attribute for each dependency with
 * 'Name' suffix backed by the actual ObjectName attribute.
 * <p>
 * Thread safety - setting attributes is synchronized on 'this'. Synchronization
 * of {@link org.opendaylight.controller.config.spi.Module#validate()} and
 * {@link org.opendaylight.controller.config.spi.Module#getInstance()} is also
 * guaranteed by
 * org.opendaylight.controller.config.manager.impl.ConfigTransactionControllerInternal
 * so the actual {@link org.opendaylight.controller.config.spi.Module} needs not
 * to be thread safe.
 * </p>
 */
@ThreadSafe
public class DynamicWritableWrapper extends AbstractDynamicWrapper {
    private static final Logger LOG = LoggerFactory
            .getLogger(DynamicWritableWrapper.class);

    private final ReadOnlyAtomicBoolean configBeanModificationDisabled;

    public DynamicWritableWrapper(final Module module,
                                  final ModuleIdentifier moduleIdentifier,
                                  final String transactionIdentifier,
                                  final ReadOnlyAtomicBoolean configBeanModificationDisabled,
                                  final MBeanServer internalServer, final MBeanServer configMBeanServer) {
        super(module, true, moduleIdentifier, ObjectNameUtil
                        .createTransactionModuleON(transactionIdentifier, moduleIdentifier), getOperations(moduleIdentifier),
                internalServer, configMBeanServer);
        this.configBeanModificationDisabled = configBeanModificationDisabled;
    }

    private static MBeanOperationInfo[] getOperations(
            final ModuleIdentifier moduleIdentifier) {
        Method validationMethod;
        try {
            validationMethod = DynamicWritableWrapper.class.getMethod("validate");
        } catch (final NoSuchMethodException e) {
            throw new IllegalStateException("No such method exception on " + moduleIdentifier, e);
        }
        return new MBeanOperationInfo[]{new MBeanOperationInfo("Validation", validationMethod)};
    }

    @Override
    public synchronized void setAttribute(final Attribute attribute)
            throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
        Attribute newAttribute = attribute;
        if (configBeanModificationDisabled.get()) {
            throw new IllegalStateException("Operation is not allowed now");
        }

        if ("Attribute".equals(newAttribute.getName())) {
            setAttribute((Attribute) newAttribute.getValue());
            return;
        }

        try {
            if (newAttribute.getValue() instanceof ObjectName) {
                newAttribute = fixDependencyAttribute(newAttribute);
            } else if (newAttribute.getValue() instanceof ObjectName[]) {
                newAttribute = fixDependencyListAttribute(newAttribute);
            }

            internalServer.setAttribute(objectNameInternal, newAttribute);
        } catch (final InstanceNotFoundException e) {
            throw new MBeanException(e);
        }

    }

    private Attribute fixDependencyListAttribute(final Attribute attribute) {
        Attribute newAttribute = attribute;
        AttributeHolder attributeHolder = attributeHolderMap.get(newAttribute.getName());
        if (attributeHolder.getRequireInterfaceOrNull() != null) {
            newAttribute = new Attribute(newAttribute.getName(), fixObjectNames((ObjectName[]) newAttribute.getValue()));
        }
        return newAttribute;
    }

    private Attribute fixDependencyAttribute(final Attribute attribute) {
        Attribute newAttribute = attribute;
        AttributeHolder attributeHolder = attributeHolderMap.get(newAttribute.getName());
        if (attributeHolder.getRequireInterfaceOrNull() != null) {
            newAttribute = new Attribute(newAttribute.getName(), fixObjectName((ObjectName) newAttribute.getValue()));
        } else {
            newAttribute = new Attribute(newAttribute.getName(), newAttribute.getValue());
        }
        return newAttribute;
    }

    private ObjectName[] fixObjectNames(final ObjectName[] dependencies) {
        int i = 0;

        for (ObjectName dependencyOn : dependencies) {
            dependencies[i++] = fixObjectName(dependencyOn);
        }

        return dependencies;
    }

    @Override
    public AttributeList setAttributes(final AttributeList attributes) {
        AttributeList result = new AttributeList();
        for (Object attributeObject : attributes) {
            Attribute attribute = (Attribute) attributeObject;
            try {
                setAttribute(attribute);
                result.add(attribute);
            } catch (final Exception e) {
                LOG.warn("Setting attribute {} failed on {}", attribute.getName(), moduleIdentifier, e);
                throw new IllegalArgumentException(
                        "Setting attribute failed - " + attribute.getName()
                                + " on " + moduleIdentifier, e);
            }
        }
        return result;
    }

    @Override
    public Object invoke(final String actionName, final Object[] params, final String[] signature)
            throws MBeanException, ReflectionException {
        if ("validate".equals(actionName)
                && (params == null || params.length == 0)
                && (signature == null || signature.length == 0)) {
            try {
                validate();
            } catch (final Exception e) {

                throw new MBeanException(ValidationException.createForSingleException(
                        moduleIdentifier, e));
            }
            return Void.TYPE;
        }
        return super.invoke(actionName, params, signature);
    }

    public void validate() {
        module.validate();
    }
}
