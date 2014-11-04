
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.configuration;

import java.util.List;

import org.opendaylight.controller.sal.utils.IObjectReader;
import org.opendaylight.controller.sal.utils.Status;

/**
 * Common configuration interface for Configuration Service and Container
 * Configuration Service
 */
public interface IConfigurationServiceCommon {
    /**
     * Represent the trigger to save the controller configuration cluster wide.
     * When called on IConfigurationService, it will trigger a cluster wide save
     * configuration event for all the global instance components and all
     * components in all containers. When called on
     * IContainerConfigurationService, it will trigger a cluster wide save
     * configuration event for all components in the current container.
     *
     * @return the Status object representing the result of the saving request
     */
    Status saveConfigurations();

    /**
     * Bundle will call this function to ask Configuration Manager to persist
     * their configurations. It is up to the Configuration Manager to decide
     * how the configuration will be persisted
     *
     * @param config
     *            The bundle configuration as a collection of
     *            ConfigurationObject
     * @param storeName
     *            The identifier for this configuration
     * @return The Status of the operation
     */
    Status persistConfiguration(List<ConfigurationObject> config, String storeName);

    /**
     * Bundle will call this function to ask Configuration Manager to retrieve
     * the configuration identified by the passed store name
     *
     * @param reader
     *            The reader object for parsing the configuration provided by
     *            the caller
     * @param storeName
     *            The identifier for the configuration
     * @return The retrieved configuration as a collection of
     *         ConfigurationObject
     */
    List<ConfigurationObject> retrieveConfiguration(IObjectReader reader, String storeName);
}
