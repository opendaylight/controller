/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.api;

import java.util.Map;
import java.util.Set;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;

public interface ServiceReferenceReadableRegistry {

    /**
     * Lookup object name by fully qualified service interface name and service reference name.
     * @param serviceInterfaceQName service interface name
     * @param refName service reference name supplied in
     * {@link org.opendaylight.controller.config.api.ConfigTransactionController#saveServiceReference(String, String, javax.management.ObjectName)}
     * @throws java.lang.IllegalArgumentException if module not found
     */
    ObjectName lookupConfigBeanByServiceInterfaceName(String serviceInterfaceQName, String refName);

    /**
     * Get mapping of services to reference names and module object names.
     */
    Map<String /* serviceInterfaceQName */, Map<String/* refName */, ObjectName>> getServiceMapping();

    /**
     * Get current mapping between reference names and module object names for given service interface name.
     * @param serviceInterfaceQName service interface name
     * @throws IllegalArgumentException if there is a mismatch between serviceInterfaceName and objectName
     */
    Map<String /* refName */, ObjectName> lookupServiceReferencesByServiceInterfaceName(String serviceInterfaceQName);

    /**
     * Find all available service interface names of a module.
     * @param objectName module object name
     * @throws InstanceNotFoundException if search did not find exactly one instance
     */
    Set<String> lookupServiceInterfaceNames(ObjectName objectName) throws InstanceNotFoundException;

    /**
     * @param namespace service interface namespace
     * @param localName service interface local name
     * @return fully qualified name needed by all other service reference mapping methods.
     * @throws java.lang.IllegalArgumentException if namespace or localName is not found
     */
    String getServiceInterfaceName(String namespace, String localName);

    /**
     * @return ObjectName with type=Service that was created using
     * {@link org.opendaylight.controller.config.api.ServiceReferenceWritableRegistry#saveServiceReference(String, String,
     * javax.management.ObjectName)}
     */
    ObjectName getServiceReference(String serviceInterfaceQName, String refName) throws InstanceNotFoundException;

    void checkServiceReferenceExists(ObjectName objectName) throws InstanceNotFoundException;

}
