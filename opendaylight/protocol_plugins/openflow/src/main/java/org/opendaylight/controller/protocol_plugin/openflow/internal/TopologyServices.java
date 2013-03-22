
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow.internal;

import java.util.Dictionary;
import java.util.Set;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.protocol_plugin.openflow.IRefreshInternalProvider;
import org.opendaylight.controller.protocol_plugin.openflow.ITopologyServiceShimListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.topology.IPluginInTopologyService;
import org.opendaylight.controller.sal.topology.IPluginOutTopologyService;

public class TopologyServices implements ITopologyServiceShimListener,
        IPluginInTopologyService {
    protected static final Logger logger = LoggerFactory
            .getLogger(TopologyServices.class);
    private IPluginOutTopologyService salTopoService = null;
    private IRefreshInternalProvider topoRefreshService = null;
    private String containerName;

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    @SuppressWarnings("unchecked")
    void init(Component c) {
        logger.debug("INIT called!");
        Dictionary<Object, Object> props = c.getServiceProperties();
        containerName = (props != null) ? (String) props.get("containerName")
                : null;
    }

    /**
     * Function called by the dependency manager when at least one
     * dependency become unsatisfied or when the component is shutting
     * down because for example bundle is being stopped.
     *
     */
    void destroy() {
        logger.debug("DESTROY called!");
    }

    /**
     * Function called by dependency manager after "init ()" is called
     * and after the services provided by the class are registered in
     * the service registry
     *
     */
    void start() {
        logger.debug("START called!");
    }

    /**
     * Function called by the dependency manager before the services
     * exported by the component are unregistered, this will be
     * followed by a "destroy ()" calls
     *
     */
    void stop() {
        logger.debug("STOP called!");
    }

    /**
     * Retrieve SAL service IPluginOutTopologyService
     *
     * @param s Called by Dependency Manager as soon as the SAL
     * service is available
     */
    public void setPluginOutTopologyService(IPluginOutTopologyService s) {
        logger.debug("Setting IPluginOutTopologyService to:" + s);
        this.salTopoService = s;
    }

    /**
     * called when SAL service IPluginOutTopologyService is no longer available
     *
     * @param s Called by Dependency Manager as soon as the SAL
     * service is unavailable
     */
    public void unsetPluginOutTopologyService(IPluginOutTopologyService s) {
        if (this.salTopoService == s) {
            logger.debug("UNSetting IPluginOutTopologyService from:" + s);
            this.salTopoService = null;
        }
    }

    /**
     * Retrieve OF protocol_plugin service IRefreshInternalProvider
     *
     * @param s Called by Dependency Manager as soon as the SAL
     * service is available
     */
    public void setRefreshInternalProvider(IRefreshInternalProvider s) {
        logger.debug("Setting IRefreshInternalProvider to:" + s);
        this.topoRefreshService = s;
    }

    /**
     * called when OF protocol_plugin service IRefreshInternalProvider
     * is no longer available
     *
     * @param s Called by Dependency Manager as soon as the SAL
     * service is unavailable
     */
    public void unsetRefreshInternalProvider(IRefreshInternalProvider s) {
        if (this.topoRefreshService == s) {
            logger.debug("UNSetting IRefreshInternalProvider from:" + s);
            this.topoRefreshService = null;
        }
    }

    @Override
    public void edgeUpdate(Edge edge, UpdateType type, Set<Property> props) {
        if (this.salTopoService != null) {
            this.salTopoService.edgeUpdate(edge, type, props);
        }
    }

    @Override
    public void sollicitRefresh() {
        logger.debug("Got a request to refresh topology");
        topoRefreshService.requestRefresh(containerName);
    }

    @Override
    public void edgeOverUtilized(Edge edge) {
        if (this.salTopoService != null) {
            this.salTopoService.edgeOverUtilized(edge);
        }
    }

    @Override
    public void edgeUtilBackToNormal(Edge edge) {
        if (this.salTopoService != null) {
            this.salTopoService.edgeUtilBackToNormal(edge);
        }
    }
}
