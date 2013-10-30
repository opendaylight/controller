/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.api;

import java.util.List;
import java.util.Set;

import javax.management.ObjectName;

import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.api.jmx.constants.ConfigRegistryConstants;

/**
 * Provides functionality for working with configuration registry - mainly
 * creating and committing config transactions.
 */
public interface ConfigRegistry extends LookupRegistry {

    /**
     * Only well-known ObjectName in configuration system, under which
     * ConfigRegisry is registered.
     */
    public static final ObjectName OBJECT_NAME = ConfigRegistryConstants.OBJECT_NAME;

    /**
     * Opens new configuration transaction.
     *
     * @return {@link ObjectName} of {@link ConfigTransactionControllerMXBean}
     */
    ObjectName beginConfig();

    /**
     * Verifies and commits transaction.
     *
     * @param transactionControllerON
     *            {@link ObjectName} of
     *            {@link ConfigTransactionControllerMXBean} that was received in
     *            {@link #beginConfig()} method call.
     * @return CommitStatus
     * @throws ValidationException
     *             if validation fails
     * @throws ConflictingVersionException
     *             if configuration state was changed
     */
    CommitStatus commitConfig(ObjectName transactionControllerON)
            throws ConflictingVersionException, ValidationException;

    /**
     * @return list of open configuration transactions.
     */
    List<ObjectName> getOpenConfigs();

    /**
     * Will return true unless there was a transaction that succeeded during
     * validation but failed in second phase of commit. In this case the server
     * is unstable and its state is undefined.
     */
    boolean isHealthy();

    Set<String> getAvailableModuleNames();

    /**
     * Find all runtime beans
     *
     * @return objectNames
     */
    Set<ObjectName> lookupRuntimeBeans();

    /**
     * Find all runtime of specified module
     *
     * @param moduleName
     *            of bean
     * @param instanceName
     *            of bean
     * @return objectNames
     */
    Set<ObjectName> lookupRuntimeBeans(String moduleName, String instanceName);

}
