/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.api;

import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;

public interface ServiceReferenceWritableRegistry extends ServiceReferenceReadableRegistry {
    /**
     * Create or update reference name to objectName. Reference name is unique per service interface name.
     * @return created or updated object name containing service name and reference name
     * @throws IllegalArgumentException if there is a mismatch between serviceInterfaceName and objectName
     * @throws InstanceNotFoundException if search did not find exactly one instance
     */
    ObjectName saveServiceReference(String serviceInterfaceName, String refName, ObjectName moduleON) throws InstanceNotFoundException;

    /**
     * Remove service reference.
     * @return true iif removed
     * @throws IllegalArgumentException if service interface name is not advertised by any module
     */
    void removeServiceReference(String serviceInterfaceName, String refName) throws InstanceNotFoundException;

    /**
     * Remove all service references.
     */
    void removeAllServiceReferences();

    /**
     * Remove all service references attached to given module.
     * @return true iif at least one reference was removed
     */
    boolean removeServiceReferences(ObjectName objectName) throws InstanceNotFoundException;
}
