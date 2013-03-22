
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.configuration.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.opendaylight.controller.clustering.services.IClusterGlobalServices;
import org.opendaylight.controller.configuration.IConfigurationAware;
import org.opendaylight.controller.configuration.IConfigurationContainerAware;
import org.opendaylight.controller.configuration.IConfigurationContainerService;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.controller.sal.utils.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @file   ConfigurationImpl.java
 *
 * @brief  Backend functionality for all Configuration related tasks.
 *
 *
 */

public class ConfigurationContainerImpl implements
        IConfigurationContainerService, IConfigurationAware {
    private static final Logger logger = LoggerFactory
            .getLogger(ConfigurationContainerImpl.class);
    private IClusterGlobalServices clusterServices;
    /*
     * Collection containing the configuration objects.
     * This is configuration world: container names (also the map key)
     * are maintained as they were configured by user, same case
     */
    private Set<IConfigurationContainerAware> configurationAwareList = (Set<IConfigurationContainerAware>) Collections
            .synchronizedSet(new HashSet<IConfigurationContainerAware>());

    public void addConfigurationContainerAware(
            IConfigurationContainerAware configurationAware) {
        if (!this.configurationAwareList.contains(configurationAware)) {
            this.configurationAwareList.add(configurationAware);
        }
    }

    public int getConfigurationAwareListSize() {
    	return this.configurationAwareList.size();
    }
    
    public void removeConfigurationContainerAware(
            IConfigurationContainerAware configurationAware) {
        this.configurationAwareList.remove(configurationAware);
    }

    public void setClusterServices(IClusterGlobalServices i) {
        this.clusterServices = i;
        logger.debug("IClusterServices set");
    }

    public void unsetClusterServices(IClusterGlobalServices i) {
        if (this.clusterServices == i) {
            this.clusterServices = null;
            logger.debug("IClusterServices Unset");
        }
    }

    public void init() {
    }

    public void destroy() {
        // Clear local states
        this.configurationAwareList.clear();
    }

    @Override
    public Status saveConfiguration() {
        boolean success = true;
        for (IConfigurationContainerAware configurationAware : configurationAwareList) {
            logger.info("Save Config triggered for "
                    + configurationAware.getClass().getSimpleName());

            Status status = configurationAware.saveConfiguration();
            if (!status.isSuccess()) {
            	success = false;
            	logger.info("Failed to save config for "
            			+ configurationAware.getClass().getSimpleName());
            }
        }
        if (success) {
            return new Status(StatusCode.SUCCESS, null);
        } else {
            return new Status(StatusCode.INTERNALERROR,
            		"Failed to Save All Configurations");
        }
    }

    @Override
    public Status saveConfigurations() {
        return saveConfiguration();
    }
}
