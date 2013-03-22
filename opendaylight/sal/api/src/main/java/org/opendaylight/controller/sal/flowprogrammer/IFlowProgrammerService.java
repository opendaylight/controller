
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.flowprogrammer;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.Status;

/**
 * Interface for installing/removing flows on a network node
 *
 *
 *
 */
public interface IFlowProgrammerService {
    /**
     * Add a flow to the network node
     *
     * @param node
     * @param flow
     */
    Status addFlow(Node node, Flow flow);

    /**
     * Modify existing flow on the switch
     *
     * @param node
     * @param flow
     */
    Status modifyFlow(Node node, Flow oldflow, Flow newFlow);

    /**
     * Remove the flow from the network node
     * @param node
     * @param flow
     */
    Status removeFlow(Node node, Flow flow);

    /**
     * Remove all flows present on the network node
     * @param node
     */
    Status removeAllFlows(Node node);
}
