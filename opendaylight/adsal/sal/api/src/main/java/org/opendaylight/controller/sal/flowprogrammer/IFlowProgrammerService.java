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
@Deprecated
public interface IFlowProgrammerService {
    /**
     * Synchronously add a flow to the network node
     *
     * @param node
     *            The target network node
     * @param flow
     *            The flow to install
     * @return The status of this request
     */
    Status addFlow(Node node, Flow flow);

    /**
     * Synchronously modify existing flow on the switch
     *
     * @param node
     *            The target network node
     * @param oldFlow
     *            The existing flow to modify
     * @param newFlow
     *            The new flow to install
     * @return The status of this request
     */
    Status modifyFlow(Node node, Flow oldFlow, Flow newFlow);

    /**
     * Synchronously remove the flow from the network node
     *
     * @param node
     *            The target network node
     * @param flow
     *            The flow to remove
     * @return The status of this request
     */
    Status removeFlow(Node node, Flow flow);

    /**
     * Asynchronously add a flow to the network node
     *
     * @param node
     *            The target network node
     * @param flow
     *            The flow to install
     * @return The status of this request containing the unique request id
     */
    Status addFlowAsync(Node node, Flow flow);

    /**
     * Asynchronously modify existing flow on the switch
     *
     * @param node
     *            The target network node
     * @param oldFlow
     *            The existing flow to modify
     * @param newFlow
     *            The new flow to install
     * @return The status of this request containing the unique request id
     */
    Status modifyFlowAsync(Node node, Flow oldFlow, Flow newFlow);

    /**
     * Asynchronously remove the flow from the network node
     *
     * @param node
     *            The target network node
     * @param flow
     *            The flow to remove
     * @return The status of this request containing the unique request id
     */
    Status removeFlowAsync(Node node, Flow flow);

    /**
     * Remove all flows present on the network node
     *
     * @param node
     *            The target network node
     * @return The status of this request containing the unique request id
     */
    Status removeAllFlows(Node node);

    /**
     * Send Barrier message synchronously. The caller will be blocked until the
     * solicitation response arrives.
     *
     * Solicit the network node to report whether all the requests sent so far
     * are completed. When this call is done, caller knows that all past flow
     * operations requested to the node in asynchronous fashion were satisfied
     * by the network node and that in case of any failure, a message was sent
     * to the controller.
     *
     * @param node
     *            The network node to solicit
     * @return The status of this request containing the unique request id
     */
    Status syncSendBarrierMessage(Node node);

    /**
     * Send Barrier message asynchronously. The caller is not blocked.
     *
     * Solicit the network node to report whether all the requests sent so far
     * are completed. When this call is done, caller knows that all past flow
     * operations requested to the node in asynchronous fashion were satisfied
     * by the network node and that in case of any failure, a message was sent
     * to the controller.
     *
     * @param node
     *            The network node to solicit
     * @return The status of this request containing the unique request id
     */
    Status asyncSendBarrierMessage(Node node);
}
