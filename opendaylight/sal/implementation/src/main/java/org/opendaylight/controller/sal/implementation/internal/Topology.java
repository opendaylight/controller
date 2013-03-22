
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.implementation.internal;

import java.util.HashSet;
import java.util.Set;
import java.util.Collections;

import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.topology.IListenTopoUpdates;
import org.opendaylight.controller.sal.topology.IPluginInTopologyService;
import org.opendaylight.controller.sal.topology.IPluginOutTopologyService;
import org.opendaylight.controller.sal.topology.ITopologyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Topology implements IPluginOutTopologyService, ITopologyService {
    protected static final Logger logger = LoggerFactory
            .getLogger(Topology.class);
    private Set<IListenTopoUpdates> updateService = Collections
            .synchronizedSet(new HashSet<IListenTopoUpdates>());
    private Set<IPluginInTopologyService> pluginService = Collections
            .synchronizedSet(new HashSet<IPluginInTopologyService>());

    void setPluginService(IPluginInTopologyService s) {
        if (this.pluginService != null) {
            this.pluginService.add(s);
        }
    }

    void unsetPluginService(IPluginInTopologyService s) {
        if (this.pluginService != null) {
            this.pluginService.remove(s);
        }
    }

    void setUpdateService(IListenTopoUpdates s) {
        if (this.updateService != null) {
            this.updateService.add(s);
        }
    }

    void unsetUpdateService(IListenTopoUpdates s) {
        if (this.updateService != null) {
            this.updateService.remove(s);
        }
    }

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    void init() {
    }

    /**
     * Function called by the dependency manager when at least one
     * dependency become unsatisfied or when the component is shutting
     * down because for example bundle is being stopped.
     *
     */
    void destroy() {
        // Make sure to clear all the data structure we use to track
        // services
        if (this.updateService != null) {
            this.updateService.clear();
        }
        if (this.pluginService != null) {
            this.pluginService.clear();
        }
    }

    @Override
    public void sollicitRefresh() {
        synchronized (this.pluginService) {
            for (IPluginInTopologyService s : this.pluginService) {
                s.sollicitRefresh();
            }
        }
    }

    @Override
    public void edgeUpdate(Edge e, UpdateType type, Set<Property> props) {
        synchronized (this.updateService) {
            for (IListenTopoUpdates s : this.updateService) {
                s.edgeUpdate(e, type, props);
            }
        }
    }

    @Override
    public void edgeOverUtilized(Edge edge) {
        synchronized (this.updateService) {
            for (IListenTopoUpdates s : this.updateService) {
                s.edgeOverUtilized(edge);
            }
        }
    }

    @Override
    public void edgeUtilBackToNormal(Edge edge) {
        synchronized (this.updateService) {
            for (IListenTopoUpdates s : this.updateService) {
                s.edgeUtilBackToNormal(edge);
            }
        }
    }
}
