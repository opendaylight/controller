/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow;

import java.util.List;
import java.util.Set;

import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.topology.TopoEdgeUpdate;

/**
 * The Interface provides Edge updates to the topology listeners
 */
public interface ITopologyServiceShimListener {
    /**
     * Called to update on Edge in the topology graph
     *
     * @param topoedgeupdateList
     *            List of topoedgeupdates Each topoedgeupdate includes edge, its
     *            Properties ( BandWidth and/or Latency etc) and update type.
     */
    public void edgeUpdate(List<TopoEdgeUpdate> topoedgeupdateList);

    /**
     * Called when an Edge utilization is above the safe threshold configured on
     * the controller
     *
     * @param {@link org.opendaylight.controller.sal.core.Edge}
     */
    public void edgeOverUtilized(Edge edge);

    /**
     * Called when the Edge utilization is back to normal, below the safety
     * threshold level configured on the controller
     *
     * @param {@link org.opendaylight.controller.sal.core.Edge}
     */
    public void edgeUtilBackToNormal(Edge edge);
}
