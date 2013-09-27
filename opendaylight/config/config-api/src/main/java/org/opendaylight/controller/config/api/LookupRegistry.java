/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.api;

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

}
