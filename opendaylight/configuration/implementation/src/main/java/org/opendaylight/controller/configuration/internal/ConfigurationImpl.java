
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.configuration.internal;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.ICacheUpdateAware;
import org.opendaylight.controller.clustering.services.IClusterGlobalServices;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.opendaylight.controller.configuration.ConfigurationEvent;
import org.opendaylight.controller.configuration.IConfigurationAware;
import org.opendaylight.controller.configuration.IConfigurationService;
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

public class ConfigurationImpl implements IConfigurationService, ICacheUpdateAware<ConfigurationEvent, String> {
    private static final Logger logger = LoggerFactory
            .getLogger(ConfigurationImpl.class);
    private IClusterGlobalServices clusterServices;
    private ConcurrentMap <ConfigurationEvent, String> configEvent;
    /*
     * Collection containing the configuration objects.
     * This is configuration world: container names (also the map key)
     * are maintained as they were configured by user, same case
     */
    private Set<IConfigurationAware> configurationAwareList = (Set<IConfigurationAware>) Collections
            .synchronizedSet(new HashSet<IConfigurationAware>());


    public int getConfigurationAwareListSize() {
        return this.configurationAwareList.size();
    }

    public void addConfigurationAware(IConfigurationAware configurationAware) {
        if (!this.configurationAwareList.contains(configurationAware)) {
            this.configurationAwareList.add(configurationAware);
        }
    }

    public void removeConfigurationAware(IConfigurationAware configurationAware) {
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
        logger.info("ContainerManager startup....");
    }

    public void start() {
        allocateCache();
        retrieveCache();
    }

    public void destroy() {
        // Clear local states
        this.configurationAwareList.clear();
    }

    @Override
    public Status saveConfigurations() {
        if (configEvent != null) {
            configEvent.put(ConfigurationEvent.SAVE, "");
        }
        return saveConfigurationsInternal();
    }

    private Status saveConfigurationsInternal() {
        boolean success = true;
        for (IConfigurationAware configurationAware : configurationAwareList) {
            Status status = configurationAware.saveConfiguration();
            if (!status.isSuccess()) {
                success = false;
                logger.info("Failed to save config for {}",
                        configurationAware.getClass().getName());
            }
        }
        if (success) {
            return new Status(StatusCode.SUCCESS);
        } else {
            return new Status(StatusCode.INTERNALERROR,
                    "Failed to Save All Configurations");
        }
    }

    @Override
    public void entryCreated(ConfigurationEvent key, String cacheName,
            boolean originLocal) {
        if (originLocal) return;
    }

    @Override
    public void entryUpdated(ConfigurationEvent key, String new_value,
            String cacheName, boolean originLocal) {
        if (originLocal) return;
        if (key == ConfigurationEvent.SAVE) {
            saveConfigurationsInternal();
        }
    }

    @Override
    public void entryDeleted(ConfigurationEvent key, String cacheName,
            boolean originLocal) {
        if (originLocal) return;
    }

    @SuppressWarnings("deprecation")
    private void allocateCache() {
        if (this.clusterServices == null) {
            logger.error("uninitialized clusterServices, can't create cache");
            return;
        }
        try {
            this.clusterServices.createCache("config.event.save",
                    EnumSet.of(IClusterServices.cacheMode.NON_TRANSACTIONAL));
        } catch (CacheConfigException cce) {
            logger.error("Error creating Configuration cache ", cce);
        } catch (CacheExistException cce) {
            logger.error("Configuration Cache already exists, destroy and recreate ", cce);
        }
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    private void retrieveCache() {
        if (this.clusterServices == null) {
            logger.error("uninitialized clusterServices, can't retrieve cache");
            return;
        }
        configEvent = (ConcurrentMap<ConfigurationEvent, String>) this.clusterServices.getCache("config.event.save");
        if (configEvent == null) {
            logger.error("Failed to retrieve configuration Cache");
        }
    }
}
