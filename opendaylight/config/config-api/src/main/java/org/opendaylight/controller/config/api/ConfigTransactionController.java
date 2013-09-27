/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.api;

import java.util.Set;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;

/**
 * Represents functionality provided by configuration transaction.
 */
public interface ConfigTransactionController extends LookupRegistry {

    /**
     * Create new configuration bean.
     *
     * @param moduleName
     * @param instanceName
     * @return ObjectName of newly created module
     * @throws InstanceAlreadyExistsException
     *             if given ifcName and instanceName is already registered
     */
    ObjectName createModule(String moduleName, String instanceName)
            throws InstanceAlreadyExistsException;

    /**
     * Destroy existing module.
     *
     * @param objectName
     *            can be either read-only module name that can be obtained using
     *            {@link ConfigRegistry#lookupConfigBean(String, String)} or
     *            writable module name that must contain current transaction
     *            name.
     * @throws InstanceNotFoundException
     *             if module is not found
     * @throws IllegalArgumentException
     *             if object name contains wrong transaction name or domain
     */
    void destroyModule(ObjectName objectName) throws InstanceNotFoundException;

    /**
     * Destroy current transaction.
     */
    void abortConfig();

    /**
     * This method can be called multiple times, has no side effects.
     *
     * @throws ValidationException
     *             if validation fails
     */
    void validateConfig() throws ValidationException;

    /**
     *
     * @return transactionName
     */
    String getTransactionName();

    Set<String> getAvailableModuleNames();

}
