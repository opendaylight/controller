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
 * Interface that defines the methods available to the functional modules above
 * SAL for installing/modifying/removing flows on a network node
 */
public interface IFlowProgrammerService {
    /**
     * Synchronously add a flow to the network node
     * 
     * @param node
     * @param flow
     */
    Status addFlow(Node node, Flow flow);

    /**
     * Synchronously modify existing flow on the switch
     * 
     * @param node
     * @param flow
     */
    Status modifyFlow(Node node, Flow oldFlow, Flow newFlow);

    /**
     * Synchronously remove the flow from the network node
     * 
     * @param node
     * @param flow
     */
    Status removeFlow(Node node, Flow flow);

    /**
     * Asynchronously add a flow to the network node
     * 
     * @param node
     * @param flow
     */
    Status addFlowAsync(Node node, Flow flow);

    /**
     * Asynchronously modify existing flow on the switch
     * 
     * @param node
     * @param flow
     */
    Status modifyFlowAsync(Node node, Flow oldFlow, Flow newFlow);

    /**
     * Asynchronously remove the flow from the network node
     * 
     * @param node
     * @param flow
     */
    Status removeFlowAsync(Node node, Flow flow);

    /**
     * Remove all flows present on the network node
     * 
     * @param node
     */
    Status removeAllFlows(Node node);

    /**
     * Send synchronous Barrier message 
     * 
     * @param node
     */
    Status sendBarrierMessage(Node node);
}
