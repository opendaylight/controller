/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.dynamicmbean;

import static java.lang.String.format;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerNotification;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.annotations.Description;
import org.opendaylight.controller.config.api.annotations.RequireInterface;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.manager.impl.util.InterfacesHelper;
import org.opendaylight.controller.config.spi.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains common code for readable/rw dynamic mbean wrappers. Routes all
 * requests (getAttribute, setAttribute, invoke) into the actual instance, but
 * provides additional functionality - namely it disallows setting attribute on
 * a read only wrapper.
 *
 */
abstract class AbstractDynamicWrapper implements DynamicMBeanModuleWrapper {
    private static final Logger logger = LoggerFactory
            .getLogger(AbstractDynamicWrapper.class);

    protected final boolean writable;
    protected final Module module;

    private final MBeanInfo mbeanInfo;
    protected final ObjectName objectNameInternal;
    protected final Map<String, AttributeHolder> attributeHolderMap;
    protected final ModuleIdentifier moduleIdentifier;
    protected final MBeanServer internalServer;

    public AbstractDynamicWrapper(Module module, boolean writable,
            ModuleIdentifier moduleIdentifier,
            ObjectName thisWrapperObjectName, MBeanOperationInfo[] dOperations,
            MBeanServer internalServer, MBeanServer configMBeanServer) {

        this.writable = writable;
        this.module = module;
        this.moduleIdentifier = moduleIdentifier;
        this.internalServer = internalServer;
        this.objectNameInternal = thisWrapperObjectName;
        // register the actual instance into an mbean server.
        registerActualModule(module, thisWrapperObjectName, objectNameInternal,
                internalServer, configMBeanServer);
        Set<Class<?>> jmxInterfaces = InterfacesHelper.getMXInterfaces(module
                .getClass());
        this.attributeHolderMap = buildMBeanInfo(module, writable,
                moduleIdentifier, jmxInterfaces, internalServer,
                objectNameInternal);
        this.mbeanInfo = generateMBeanInfo(module.getClass().getName(), module,
                attributeHolderMap, dOperations, jmxInterfaces);
    }

    /**
     * Register module into an internal mbean server, attach listener to the
     * platform mbean server. Wait until this wrapper gets unregistered, in that
     * case unregister the module and remove listener.
     */
    private final NotificationListener registerActualModule(Module module,
            final ObjectName thisWrapperObjectName,
            final ObjectName objectNameInternal,
            final MBeanServer internalServer,
            final MBeanServer configMBeanServer) {

        try {
            internalServer.registerMBean(module, objectNameInternal);
        } catch (InstanceAlreadyExistsException | MBeanRegistrationException
                | NotCompliantMBeanException | IllegalStateException e) {
            throw new IllegalStateException(
                    "Error occured during mbean registration with name " + objectNameInternal, e);
        }

        NotificationListener listener = new NotificationListener() {
            @Override
            public void handleNotification(Notification n, Object handback) {
                if (n instanceof MBeanServerNotification
                        && n.getType()
                                .equals(MBeanServerNotification.UNREGISTRATION_NOTIFICATION)) {
                    if (((MBeanServerNotification) n).getMBeanName().equals(
                            thisWrapperObjectName)) {
                        try {
                            internalServer.unregisterMBean(objectNameInternal);
                            configMBeanServer.removeNotificationListener(
                                    MBeanServerDelegate.DELEGATE_NAME, this);
                        } catch (MBeanRegistrationException
                                | ListenerNotFoundException
                                | InstanceNotFoundException e) {
                            throw new IllegalStateException(e);
                        }
                    }
                }
            }
        };
        try {
            configMBeanServer.addNotificationListener(
                    MBeanServerDelegate.DELEGATE_NAME, listener, null, null);
        } catch (InstanceNotFoundException e) {
            throw new RuntimeException("Could not add notification listener", e);
        }
        return listener;
    }

    private static MBeanInfo generateMBeanInfo(String className, Module module,
            Map<String, AttributeHolder> attributeHolderMap,
            MBeanOperationInfo[] dOperations, Set<Class<?>> jmxInterfaces) {

        String dDescription = findDescription(module.getClass(), jmxInterfaces);
        MBeanConstructorInfo[] dConstructors = new MBeanConstructorInfo[0];
        List<MBeanAttributeInfo> attributes = new ArrayList<>(
                attributeHolderMap.size());
        for (AttributeHolder attributeHolder : attributeHolderMap.values()) {
            attributes.add(attributeHolder.toMBeanAttributeInfo());
        }
        return new MBeanInfo(className, dDescription,
                attributes.toArray(new MBeanAttributeInfo[0]), dConstructors,
                dOperations, new MBeanNotificationInfo[0]);
    }

    static String findDescription(Class<?> clazz, Set<Class<?>> jmxInterfaces) {
        List<Description> descriptions = AnnotationsHelper
                .findClassAnnotationInSuperClassesAndIfcs(clazz, Description.class, jmxInterfaces);
        return AnnotationsHelper.aggregateDescriptions(descriptions);
    }

    protected static MBeanOperationInfo[] getEmptyOperations() {
        return new MBeanOperationInfo[0];
    }

    // inspect all exported interfaces ending with MXBean, extract getters &
    // setters into attribute holder
    private static Map<String, AttributeHolder> buildMBeanInfo(Module module,
            boolean writable, ModuleIdentifier moduleIdentifier,
            Set<Class<?>> jmxInterfaces, MBeanServer internalServer,
            ObjectName internalObjectName) {

        // internal variables for describing MBean elements
        Set<Method> methods = new HashSet<>();

        for (Class<?> exportedClass : jmxInterfaces) {
            Method[] ifcMethods = exportedClass.getMethods();
            methods.addAll(Arrays.asList(ifcMethods));
        }
        // TODO: fix reflection, not used
        MBeanInfo internalInfo;
        try {
            internalInfo = internalServer.getMBeanInfo(internalObjectName);
        } catch (InstanceNotFoundException | ReflectionException
                | IntrospectionException e) {
            throw new RuntimeException("MBean info not found", e);
        }

        Map<String, MBeanAttributeInfo> attributeMap = new HashMap<>();
        for (MBeanAttributeInfo a : internalInfo.getAttributes()) {
            attributeMap.put(a.getName(), a);
        }
        Map<String, AttributeHolder> attributeHolderMap = new HashMap<>();
        for (Method method : methods) {

            if (method.getParameterTypes().length == 1
                    && method.getName().startsWith("set")) {
                Method setter;
                String attribName = method.getName().substring(3);
                try {
                    setter = module.getClass().getMethod(method.getName(),
                            method.getParameterTypes());
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException("No such method on "
                            + moduleIdentifier, e);
                }
                RequireInterface ifc = AttributeHolder
                        .findRequireInterfaceAnnotation(setter, jmxInterfaces);
                String description = null;
                if (ifc != null) {
                    description = AttributeHolder.findDescription(setter,
                            jmxInterfaces);
                }
                AttributeHolder attributeHolder = new AttributeHolder(
                        attribName, module, attributeMap.get(attribName)
                                .getType(), writable, ifc, description);
                attributeHolderMap.put(attribName, attributeHolder);
            }
        }
        return attributeHolderMap;
    }

    // DynamicMBean methods

    @Override
    public MBeanInfo getMBeanInfo() {
        return mbeanInfo;
    }

    @Override
    public Object getAttribute(String attributeName)
            throws AttributeNotFoundException, MBeanException,
            ReflectionException {
        if (attributeName.equals("MBeanInfo")) {
            return getMBeanInfo();
        }

        Object obj = null;
        try {
            obj = internalServer
                    .getAttribute(objectNameInternal, attributeName);
        } catch (InstanceNotFoundException e) {
            new MBeanException(e);
        }
        if (obj instanceof ObjectName) {
            AttributeHolder attributeHolder = attributeHolderMap
                    .get(attributeName);
            if (attributeHolder.getRequireInterfaceOrNull() != null) {
                obj = fixObjectName((ObjectName) obj);
            }
            return obj;
        }
        return obj;

    }

    protected ObjectName fixObjectName(ObjectName on) {
        if (!ObjectNameUtil.ON_DOMAIN.equals(on.getDomain()))
            throw new IllegalArgumentException("Wrong domain, expected "
                    + ObjectNameUtil.ON_DOMAIN + " setter on " + on);
        // if on contains transaction name, remove it
        String transactionName = ObjectNameUtil.getTransactionName(on);
        if (transactionName != null)
            return ObjectNameUtil.withoutTransactionName(on);
        else
            return on;
    }

    @Override
    public AttributeList getAttributes(String[] attributes) {
        AttributeList result = new AttributeList();
        for (String attributeName : attributes) {
            try {
                Object value = getAttribute(attributeName);
                result.add(new Attribute(attributeName, value));

            } catch (Exception e) {
                logger.debug("Getting attribute {} failed", attributeName, e);
            }
        }
        return result;
    }

    @Override
    public Object invoke(String actionName, Object[] params, String[] signature)
            throws MBeanException, ReflectionException {
        if ("getAttribute".equals(actionName) && params.length == 1
                && signature[0].equals(String.class.getName())) {
            try {
                return getAttribute((String) params[0]);
            } catch (AttributeNotFoundException e) {
                throw new MBeanException(e, "Attribute not found on "
                        + moduleIdentifier);
            }
        } else if ("getAttributes".equals(actionName) && params.length == 1
                && signature[0].equals(String[].class.getName())) {
            return getAttributes((String[]) params[0]);
        } else if ("setAttributes".equals(actionName) && params.length == 1
                && signature[0].equals(AttributeList.class.getName())) {
            return setAttributes((AttributeList) params[0]);
        } else {
            logger.debug("Operation not found {} ", actionName);
            throw new UnsupportedOperationException(
                    format("Operation not found on %s. Method invoke is only supported for getInstance and getAttribute(s) "
                            + "method, got actionName %s, params %s, signature %s ",
                            moduleIdentifier, actionName, params, signature));
        }
    }

    @Override
    public final int hashCode() {
        return module.hashCode();
    }

    @Override
    public final boolean equals(Object other) {
        return module.equals(other);
    }

}
