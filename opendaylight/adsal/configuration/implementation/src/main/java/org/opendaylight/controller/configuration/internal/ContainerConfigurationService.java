
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
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.ICacheUpdateAware;
import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.opendaylight.controller.configuration.ConfigurationEvent;
import org.opendaylight.controller.configuration.ConfigurationObject;
import org.opendaylight.controller.configuration.IConfigurationAware;
import org.opendaylight.controller.configuration.IConfigurationContainerAware;
import org.opendaylight.controller.configuration.IConfigurationContainerService;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.IObjectReader;
import org.opendaylight.controller.sal.utils.ObjectReader;
import org.opendaylight.controller.sal.utils.ObjectWriter;
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

public class ContainerConfigurationService implements IConfigurationContainerService,
        IConfigurationAware,
        ICacheUpdateAware<String, String> {
    public static final String CONTAINER_SAVE_EVENT_CACHE = "config.container.event.save";
    private static final Logger logger = LoggerFactory.getLogger(ContainerConfigurationService.class);
    private IClusterContainerServices clusterServices;
    private ConcurrentMap<String, String> containerConfigEvent;
    // Directory which contains the startup files for this container
    private String root;
    private Set<IConfigurationContainerAware> configurationAwareList = Collections
            .synchronizedSet(new HashSet<IConfigurationContainerAware>());
    private ObjectReader objReader;
    private ObjectWriter objWriter;
    private String containerName;

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

    public void setClusterServices(IClusterContainerServices i) {
        this.clusterServices = i;
        logger.debug("IClusterServices set");
    }

    public void unsetClusterServices(IClusterContainerServices i) {
        if (this.clusterServices == i) {
            this.clusterServices = null;
            logger.debug("IClusterServices Unset");
        }
    }

    void init(Component c) {
        Dictionary<?, ?> props = c.getServiceProperties();
        containerName = (props != null) ? (String) props.get("containerName") :
            GlobalConstants.DEFAULT.toString();
        root =  String.format("%s%s/", GlobalConstants.STARTUPHOME.toString(), containerName);
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

    /**
     * Function called by the dependency manager before Container is Stopped and Destroyed.
     */
    public void containerStop() {
        // Do nothing
    }

    @Override
    public String getConfigurationRoot() {
        return root;
    }

    @Override
    public Status saveConfiguration() {
        boolean success = true;

        for (IConfigurationContainerAware configurationAware : configurationAwareList) {
            logger.trace("Save Config triggered for {}", configurationAware.getClass().getSimpleName());

            Status status = configurationAware.saveConfiguration();
            if (!status.isSuccess()) {
                success = false;
                logger.warn("Failed to save config for {} ({})", configurationAware.getClass().getSimpleName(),
                        status.getDescription());
            }
        }
        if (success) {
            return new Status(StatusCode.SUCCESS);
        } else {
            return new Status(StatusCode.INTERNALERROR, "Failed to save one or more configurations");
        }
    }

    @Override
    public Status saveConfigurations() {
        containerConfigEvent.put(ConfigurationEvent.SAVE.toString(), "");
        return saveConfiguration();
    }

    @Override
    public void entryCreated(String key, String cacheName,
            boolean originLocal) {
        if (originLocal) {
            return;
        }
    }

    @Override
    public void entryUpdated(String key, String new_value,
            String cacheName, boolean originLocal) {
        if (originLocal) {
            return;
        }
        logger.debug("Processing {} event", key);
        if (key.equals(ConfigurationEvent.SAVE.toString())) {
            saveConfiguration();
        }
    }

    @Override
    public void entryDeleted(String key, String cacheName,
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
            this.clusterServices.createCache(CONTAINER_SAVE_EVENT_CACHE,
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));
        } catch (CacheConfigException cce) {
            logger.debug("Error creating ContainerConfigurationService cache ", cce);
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
        containerConfigEvent =
                (ConcurrentMap<String, String>) this.clusterServices.getCache(CONTAINER_SAVE_EVENT_CACHE);
        if (containerConfigEvent == null) {
            logger.error("Failed to retrieve configuration Cache");
        }
    }

    @Override
    public Status persistConfiguration(List<ConfigurationObject> config, String fileName) {
        if (!hasBeenSaved()) {
            return new Status(StatusCode.NOTALLOWED,
                    String.format("Container %s has not been saved yet", containerName));
        }
        String destination = String.format("%s%s", root, fileName);
        return objWriter.write(config, destination);
    }

    @Override
    public List<ConfigurationObject> retrieveConfiguration(IObjectReader reader, String fileName) {
        if (!clusterServices.amICoordinator()) {
            return Collections.emptyList();
        }
        String source = String.format("%s%s", root, fileName);
        Object obj = objReader.read(reader, source);
        if (obj == null) {
            return Collections.<ConfigurationObject> emptyList();
        }
        if (obj instanceof ConcurrentMap) {
            return new ArrayList<ConfigurationObject>(((ConcurrentMap)obj).values());
        }
        return (List<ConfigurationObject>) obj;
    }

    @Override
    public boolean hasBeenSaved() {
        try {
            File configRoot = new File(this.getConfigurationRoot());
            return configRoot.exists();
        } catch (Exception e) {
            return false;
        }

    }
}
