
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.topology;

import java.util.Set;

import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;

/**
 * @file   IPluginOutTopologyService.java
 *
 * @brief  Methods that are invoked from Protocol Plugin toward SAL
 *
 * Every time a protocol plugin update the topology, it will call this
 * service provided by SAL so the update can migrate upward toward the
 * applications
 */

/**
 * Methods that are invoked from Protocol Plugin toward SAL
 *
 */
public interface IPluginOutTopologyService {
    /**
     * Called to update on Edge in the topology graph
     *
     * @param e Edge being updated
     * @param type Type of update
     * @param props Properties of the edge, like BandWidth and/or Latency etc.
     */
    public void edgeUpdate(Edge e, UpdateType type, Set<Property> props);

    /**
     * Called when an Edge utilization is above the safety threshold
     * configured on the controller
     * @param edge
     */
    public void edgeOverUtilized(Edge edge);

    /**
     * Called when the Edge utilization is back to normal, below the safety
     * threshold level configured on the controller
     *
     * @param edge
     */
    public void edgeUtilBackToNormal(Edge edge);
}
