
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.configuration.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.ICacheUpdateAware;
import org.opendaylight.controller.clustering.services.IClusterGlobalServices;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.opendaylight.controller.configuration.ConfigurationEvent;
import org.opendaylight.controller.configuration.ConfigurationObject;
import org.opendaylight.controller.configuration.IConfigurationAware;
import org.opendaylight.controller.configuration.IConfigurationService;
import org.opendaylight.controller.containermanager.IContainerManager;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.IObjectReader;
import org.opendaylight.controller.sal.utils.ObjectReader;
import org.opendaylight.controller.sal.utils.ObjectWriter;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @file   ConfigurationImpl.java
 *
 * @brief  Backend functionality for all ConfigurationService related tasks.
 *
 */

public class ConfigurationService implements IConfigurationService, ICacheUpdateAware<ConfigurationEvent, String> {
    private static final Logger logger = LoggerFactory
            .getLogger(ConfigurationService.class);
    public static final String SAVE_EVENT_CACHE = "config.event.save";
    private static final Object ROOT = GlobalConstants.STARTUPHOME.toString();
    private IClusterGlobalServices clusterServices;
    private ConcurrentMap <ConfigurationEvent, String> configEvent;
    private Set<IConfigurationAware> configurationAwareList = Collections
            .synchronizedSet(new HashSet<IConfigurationAware>());
    private ObjectReader objReader;
    private ObjectWriter objWriter;


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
        logger.info("ConfigurationService Manager init");
    }

    public void start() {
        allocateCache();
        retrieveCache();
        objReader = new ObjectReader();
        objWriter = new ObjectWriter();
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
                logger.warn("Failed to save config for {}",
                        configurationAware.getClass().getName());
            }
        }
        // Remove startup directories of containers that were removed from
        // the configuration but not saved
        IContainerManager containerManager = (IContainerManager) ServiceHelper.getGlobalInstance(
                IContainerManager.class, this);
        List<String> containerNames = containerManager.getContainerNameList();
        File startupDir = new File(ROOT.toString());
        File fileList[] = startupDir.listFiles();
        for (File file : fileList) {
            if (file.getName().equals("README") || file.getName().endsWith(".conf")
               || file.getName().endsWith(".sav")) {
                continue;
            }
            if (!containerNames.contains(file.getName())) {
                File[] containerFiles = file.listFiles();
                for (File containerFile : containerFiles) {
                    containerFile.delete();
                }
                success = file.delete();
                if (!success) {
                    logger.warn("Failed to save config for {}",
                            file.getName());
                }
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
        if (originLocal) {
            return;
        }
    }

    @Override
    public void entryUpdated(ConfigurationEvent key, String new_value,
            String cacheName, boolean originLocal) {
        if (originLocal) {
            return;
        }
        if (key == ConfigurationEvent.SAVE) {
            saveConfigurationsInternal();
        }
    }

    @Override
    public void entryDeleted(ConfigurationEvent key, String cacheName,
            boolean originLocal) {
        if (originLocal) {
            return;
        }
    }

    private void allocateCache() {
        if (this.clusterServices == null) {
            logger.error("uninitialized clusterServices, can't create cache");
            return;
        }
        try {
            this.clusterServices.createCache(SAVE_EVENT_CACHE,
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));
        } catch (CacheConfigException cce) {
            logger.debug("Error creating ConfigurationService cache ", cce);
        } catch (CacheExistException cce) {
            logger.debug("ConfigurationService Cache already exists, destroy and recreate ", cce);
        }
    }

    @SuppressWarnings({ "unchecked" })
    private void retrieveCache() {
        if (this.clusterServices == null) {
            logger.error("uninitialized clusterServices, can't retrieve cache");
            return;
        }
        configEvent = (ConcurrentMap<ConfigurationEvent, String>) this.clusterServices.getCache(SAVE_EVENT_CACHE);
        if (configEvent == null) {
            logger.error("Failed to retrieve configuration Cache");
        }
    }

    @Override
    public Status persistConfiguration(List<ConfigurationObject> config, String fileName) {
        String destination = String.format("%s%s", ROOT, fileName);
        return objWriter.write(config, destination);
    }

    @Override
    public List<ConfigurationObject> retrieveConfiguration(IObjectReader reader, String fileName) {
        if (!clusterServices.amICoordinator()) {
            return Collections.emptyList();
        }
        String source = String.format("%s%s", ROOT, fileName);
        Object obj = objReader.read(reader, source);
        if (obj == null) {
            return Collections.<ConfigurationObject> emptyList();
        }
        if (obj instanceof ConcurrentMap) {
            return new ArrayList<ConfigurationObject>(((ConcurrentMap)obj).values());
        }
        return (List<ConfigurationObject>) obj;
    }
}
