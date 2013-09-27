/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.util;

import java.util.Set;

import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;

import org.opendaylight.controller.config.api.LookupRegistry;

public class LookupBeansUtil {

    public static ObjectName lookupConfigBean(LookupRegistry lookupRegistry,
            String moduleName, String instanceName)
            throws InstanceNotFoundException {
        Set<ObjectName> objectNames = lookupRegistry.lookupConfigBeans(
                moduleName, instanceName);
        if (objectNames.size() == 0) {
            throw new InstanceNotFoundException("No instance found");
        } else if (objectNames.size() > 1) {
            throw new InstanceNotFoundException("Too many instances found");
        }
        return objectNames.iterator().next();
    }

}
