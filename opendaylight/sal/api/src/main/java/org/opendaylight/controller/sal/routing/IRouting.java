
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.routing;

import java.util.Map;

import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.Path;

public interface IRouting {

    /**
     * Returns a Path leading from the source to the destination
     * @param src: source Node
     * @param dst: destination Node
     * @return: Path
     */
    public Path getRoute(Node src, Node dst);

    /**
     * Returns a Max ThroughPut Path leading from the source to the destination
     * @param src: source Node
     * @param dst: destination Node
     * @return: MTPath
     */
    public Path getMaxThroughputRoute(Node src, Node dst);

    /**
     * Returns a Path leading from the source to the destination that meets the specified bandwidth
     * @param src: source Node
     * @param dst: destination Node
     * @param Bw: bandwidth
     * @return: Path
     */
    public Path getRoute(Node src, Node dst, Short Bw);

    /**
     * Remove all routes and reset all state. USE CAREFULLY!
     */
    public void clear();

    /**
     * Remove all Max Throughput Routes and reset all state. USE CAREFULLY!
     */
    public void clearMaxThroughput();

    /**
     * Initialization For Max Throughput
     * @param EdgeWeightMap: Map containing Edge and Corresponding
     * Weight. Optional Param - if null, implementation specific weight
     * calculation will be used.
     */
    public void initMaxThroughput(Map<Edge, Number> EdgeWeightMap);

}
