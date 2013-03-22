
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * @file   ServiceHelper.java
 *
 * @brief  The class helps to register and retrieve OSGi service registry
 */
package org.opendaylight.controller.sal.utils;

import java.util.Hashtable;
import org.osgi.framework.ServiceRegistration;
import java.util.Dictionary;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class helps to register and retrieve OSGi service registry
 *
 *
 */
public class ServiceHelper {
    private static final Logger logger = LoggerFactory
            .getLogger(ServiceHelper.class);

    /**
     * Register a Service in the OSGi service registry
     *
     * @param clazz The target class
     * @param containerName The container name
     * @param instance of the object exporting the service, be careful
     * the object must implement/extend clazz else the registration
     * will fail unless a ServiceFactory is passed as parameter
     * @param properties The properties to be attached to the service
     * registration
     * @return true if registration happened, false otherwise
     */
    public static boolean registerService(Class<?> clazz, String containerName,
            Object instance, Dictionary<String, Object> properties) {
        if (properties == null) {
            properties = (Dictionary<String, Object>) new Hashtable<String, Object>();
        }
        properties.put("containerName", containerName);
        return registerGlobalService(clazz, instance, properties);
    }

    /**
     * Register a Global Service in the OSGi service registry
     *
     * @param clazz The target class
     * @param instance of the object exporting the service, be careful
     * the object must implement/extend clazz else the registration
     * will fail unless a ServiceFactory is passed as parameter
     * @param properties The properties to be attached to the service
     * registration
     * @return true if registration happened, false otherwise
     */
    public static boolean registerGlobalService(Class<?> clazz,
            Object instance, Dictionary<String, Object> properties) {
        try {
            BundleContext bCtx = FrameworkUtil.getBundle(instance.getClass())
                    .getBundleContext();
            if (bCtx == null) {
                logger.error("Could not retrieve the BundleContext");
                return false;
            }

            ServiceRegistration registration = bCtx.registerService(clazz
                    .getName(), instance, properties);
            if (registration == null) {
                logger.error("Failed to register {} for instance {}", clazz,
                        instance);
            }
            return true;
        } catch (Exception e) {
            logger.error("Exception "+e.getMessage() +" while registering the service "+instance.toString());
        }
        return false;
    }

    /**
     * Retrieve instance of a class via OSGI registry, if there
     * are many only the first is returned.
     *
     * @param clazz The target class
     * @param containerName The container name
     * @param bundle The caller
     */
    public static Object getInstance(Class<?> clazz, String containerName,
            Object bundle) {
        // Back-end convention: Container id in lower case. Let's enforce it here
        return getInstance(clazz, containerName.toLowerCase(), bundle, null);
    }

    /**
     * Retrieve global instance of a class via OSGI registry, if
     * there are many only the first is returned.
     *
     * @param clazz The target class
     * @param bundle The caller
     */
    public static Object getGlobalInstance(Class<?> clazz, Object bundle) {
        return getGlobalInstance(clazz, bundle, null);
    }

    /**
     * Retrieve instance of a class via OSGI registry, if there
     * are many only the first is returned. On this version an LDAP
     * type of filter is applied
     *
     * @param clazz The target class
     * @param containerName The container name
     * @param bundle The caller
     * @param serviceFilter LDAP filter to be applied in the search
     */
    public static Object getInstance(Class<?> clazz, String containerName,
            Object bundle, String serviceFilter) {
        Object[] instances = getInstances(clazz, containerName, bundle,
                serviceFilter);
        if (instances != null) {
            return instances[0];
        }
        return null;
    }

    /**
     * Retrieve global instance of a class via OSGI registry, if
     * there are many only the first is returned. On this version an LDAP
     * type of filter is applied
     *
     * @param clazz The target class
     * @param bundle The caller
     * @param serviceFilter LDAP filter to be applied in the search
     */
    public static Object getGlobalInstance(Class<?> clazz, Object bundle,
            String serviceFilter) {
        Object[] instances = getGlobalInstances(clazz, bundle, serviceFilter);
        if (instances != null) {
            return instances[0];
        }
        return null;
    }

    /**
     * Retrieve all the Instances of a Service, optionally
     * filtered via serviceFilter if non-null else all the results are
     * returned if null
     *
     * @param clazz The target class
     * @param containerName The container name
     * @param bundle The caller
     * @param serviceFilter LDAP filter to be applied in the search
     */
    public static Object[] getInstances(Class<?> clazz, String containerName,
            Object bundle, String serviceFilter) {
        Object instances[] = null;
        try {
            BundleContext bCtx = FrameworkUtil.getBundle(bundle.getClass())
                    .getBundleContext();

            ServiceReference[] services = null;
            if (serviceFilter != null) {
                services = bCtx.getServiceReferences(clazz.getName(),
                        "(&(containerName=" + containerName + ")"
                                + serviceFilter + ")");
            } else {
                services = bCtx.getServiceReferences(clazz.getName(),
                        "(containerName=" + containerName + ")");
            }

            if (services != null) {
                instances = new Object[services.length];
                for (int i = 0; i < services.length; i++) {
                    instances[i] = bCtx.getService(services[i]);
                }
            }
        } catch (Exception e) {
            logger.error("Instance reference is NULL");
        }
        return instances;
    }

    /**
     * Retrieve all the Instances of a Service, optionally
     * filtered via serviceFilter if non-null else all the results are
     * returned if null
     *
     * @param clazz The target class
     * @param bundle The caller
     * @param serviceFilter LDAP filter to be applied in the search
     */
    public static Object[] getGlobalInstances(Class<?> clazz, Object bundle,
            String serviceFilter) {
        Object instances[] = null;
        try {
            BundleContext bCtx = FrameworkUtil.getBundle(bundle.getClass())
                    .getBundleContext();

            ServiceReference[] services = bCtx.getServiceReferences(clazz
                    .getName(), serviceFilter);

            if (services != null) {
                instances = new Object[services.length];
                for (int i = 0; i < services.length; i++) {
                    instances[i] = bCtx.getService(services[i]);
                }
            }
        } catch (Exception e) {
            logger.error("Instance reference is NULL");
        }
        return instances;
    }
}
