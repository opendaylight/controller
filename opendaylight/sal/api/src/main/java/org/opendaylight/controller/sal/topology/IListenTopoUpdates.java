
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
 * @file   IListenTopoUpdates.java
 *
 * @brief  Topology notifications provided by SAL toward the application
 *
 * For example an application that wants to keep up to date with the
 * updates coming from SAL it will register in the OSGi service
 * registry this interface (on a per-container base) and SAL will call it
 * providing the update
 */

/**
 * Topology notifications provided by SAL toward the application
 *
 */
public interface IListenTopoUpdates {
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
     *
     * @param edge The edge which bandwidth usage is above the safety level
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
