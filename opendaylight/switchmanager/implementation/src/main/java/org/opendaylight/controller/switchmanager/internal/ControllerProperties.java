/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.switchmanager.internal;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.IClusterGlobalServices;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.opendaylight.controller.sal.core.MacAddress;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class ControllerProperties
 * provides implementation for the ControllerProperties API
 */
public class ControllerProperties implements IControllerProperties {

    private IClusterGlobalServices clusterService = null;
    private final Logger log = LoggerFactory
            .getLogger(ControllerProperties.class);
    private static final String controllerGlobalPropsCacheName = "switchmanager.controllerGlobalProps";

    private ConcurrentMap<String, Property> controllerGlobalProps;

    private void allocateCaches() {
        if (this.clusterService == null) {
            this.nonClusterObjectCreate();
            log.warn("un-initialized clusterService, can't create cache");
            return;
        }
        try {
            this.clusterService.createCache(controllerGlobalPropsCacheName,
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));
        } catch (CacheConfigException cce) {
            log.error("\nCache configuration invalid - check cache mode");
        } catch (CacheExistException ce) {
            log.error("\nCache already exits - destroy and recreate if needed");
        }
    }

    private void nonClusterObjectCreate() {
        controllerGlobalProps = new ConcurrentHashMap<String, Property>();
    }

    @SuppressWarnings({ "unchecked" })
    private void retrieveCaches() {
        if (this.clusterService == null) {
            log.warn("un-initialized clusterService, can't create cache");
            return;
        }
        controllerGlobalProps = (ConcurrentMap<String, Property>) this.clusterService
                .getCache(controllerGlobalPropsCacheName);
        if (controllerGlobalProps == null) {
            log.error("\nFailed to get cache for controllerGlobalProps");
        }
    }

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     * @param c Component
     */
    void init(Component c) {
        // nothing to do
    }

    private void initializeProperties() {
        // Add controller MAC, if first node in the cluster
        if (!controllerGlobalProps.containsKey(MacAddress.name)) {
            byte controllerMac[] = getHardwareMAC();
            if (controllerMac != null) {
                Property existing = controllerGlobalProps.putIfAbsent(MacAddress.name, new MacAddress(controllerMac));
                if (existing == null && log.isTraceEnabled()) {
                    log.trace("Setting controller MAC address in the cluster: {}", HexEncode.bytesToHexStringFormat(controllerMac));
                }
            }
        }
    }

    private byte[] getHardwareMAC() {
        Enumeration<NetworkInterface> nis;
        byte[] macAddress = null;

        try {
            nis = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            log.error("Failed to acquire controller MAC: ", e);
            return macAddress;
        }

        while (nis.hasMoreElements()) {
            NetworkInterface ni = nis.nextElement();
            try {
                macAddress = ni.getHardwareAddress();
            } catch (SocketException e) {
                log.error("Failed to acquire controller MAC: ", e);
            }
            if (macAddress != null && macAddress.length != 0) {
                break;
            }
        }
        if (macAddress == null) {
            log.warn("Failed to acquire controller MAC: No physical interface found");
            // This happens when running controller on windows VM, for example
            // Try parsing the OS command output
        }
        return macAddress;
    }

    /**
     * Function called by dependency manager after "init ()" is called and after
     * the services provided by the class are registered in the service registry
     *
     */
    void start() {
        // Instantiate cluster synced variables
        allocateCaches();
        retrieveCaches();
        // initialize required properties
        initializeProperties();
    }

    /**
     * Function called after registered the service in OSGi service registry.
     */
    void started() {
        // nothing to do
    }

    /**
     * Function called before services of the component are removed
     * from OSGi service registry.
     */
    void stopping() {
        // nothing to do
    }

    /**
     * Function called by the dependency manager before the services exported by
     * the component are unregistered, this will be followed by a "destroy ()"
     * calls
     *
     */
    void stop() {
        // nothing to do
    }

    /**
     * Function called by the dependency manager when at least one dependency
     * become unsatisfied or when the component is shutting down because for
     * example bundle is being stopped.
     *
     */
    void destroy() {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Property> getControllerProperties() {
        return new HashMap<String, Property>(this.controllerGlobalProps);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Property getControllerProperty(String propertyName) {
        if (propertyName != null) {
            return this.controllerGlobalProps.get(propertyName);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Status setControllerProperty(Property property) {
        if (property != null) {
            this.controllerGlobalProps.put(property.getName(), property);
            return new Status(StatusCode.SUCCESS);
        }
        return new Status(StatusCode.BADREQUEST, "Invalid property provided when setting property");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Status removeControllerProperty(String propertyName) {
        if (propertyName != null) {
            this.controllerGlobalProps.remove(propertyName);
            return new Status(StatusCode.SUCCESS);
        }
        return new Status(StatusCode.BADREQUEST, "Invalid property provided when removing property");
    }

    /**
     * setClusterService
     * @param s
     */
    public void setClusterService(IClusterGlobalServices s) {
        this.clusterService = s;
    }

    /**
     * unsetClusterServices
     * @param s
     */
    public void unsetClusterServices(IClusterGlobalServices s) {
        if (this.clusterService == s) {
            this.clusterService = null;
        }
    }

}
