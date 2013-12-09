/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.api;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;

public interface LookupRegistry {

    /**
     * Find all modules. Same Module can be registered multiple times.
     *
     * @return objectNames
     */
    Set<ObjectName> lookupConfigBeans();

    /**
     * Find modules with given module name.
     *
     * @param moduleName
     * @return objectNames
     */
    Set<ObjectName> lookupConfigBeans(String moduleName);

    /**
     * Find read modules.
     *
     * @param moduleName
     *            exact match for searched module name, can contain '*' to match
     *            all values.
     * @param instanceName
     *            exact match for searched instance name, can contain '*' to
     *            match all values.
     * @return objectNames
     */
    Set<ObjectName> lookupConfigBeans(String moduleName, String instanceName);

    /**
     * Find read module.
     *
     * @param moduleName
     *            exact match for searched module name, can contain '*' to match
     *            all values.
     * @param instanceName
     *            exact match for searched instance name, can contain '*' to
     *            match all values.
     * @return objectNames
     * @throws InstanceNotFoundException
     *             if search did not find exactly one instance
     */
    ObjectName lookupConfigBean(String moduleName, String instanceName)
            throws InstanceNotFoundException;

    /**
     * Lookup object name by fully qualified service interface name and service reference name.
     * @param serviceInterfaceName service interface name
     * @param refName service reference name supplied in
     * {@link org.opendaylight.controller.config.api.ConfigTransactionController#saveServiceReference(String, String, javax.management.ObjectName)}
     */
    ObjectName lookupConfigBeanByServiceInterfaceName(String serviceInterfaceName, String refName);

    /**
     * Get mapping of services to reference names and module object names.
     */
    Map<String /* serviceInterfaceName */, Map<String/* refName */, ObjectName>> getServiceMapping();

    /**
     * Get current mapping between reference names and module object names for given service interface name.
     * @param serviceInterfaceName service interface name
     * @throws IllegalArgumentException if there is a mismatch between serviceInterfaceName and objectName
     */
    Map<String /* refName */, ObjectName> lookupServiceReferencesByServiceInterfaceName(String serviceInterfaceName);

    /**
     * Find all available service interface names of a module.
     * @param objectName module object name
     * @throws InstanceNotFoundException if search did not find exactly one instance
     */
    Set<String> lookupServiceInterfaceNames(ObjectName objectName);

    /**
     * @return fully qualified name needed by all other service reference mapping methods.
     * @param namespace service interface namespace
     * @param localName service interface local name
     */
    String getServiceInterfaceName(String namespace, String localName);
}
