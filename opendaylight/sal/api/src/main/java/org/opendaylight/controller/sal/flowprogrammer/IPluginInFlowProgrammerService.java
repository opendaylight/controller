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
 * @file IPluginOutFlowProgrammer.java
 *
 * @brief Flow programmer interface to be implemented by protocol plugins
 */
public interface IPluginInFlowProgrammerService {
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
     * @param rid
     */
    Status addFlowAsync(Node node, Flow flow, long rid);

    /**
     * Asynchronously modify existing flow on the switch
     *
     * @param node
     * @param flow
     * @param rid
     */
    Status modifyFlowAsync(Node node, Flow oldFlow, Flow newFlow, long rid);

    /**
     * Asynchronously remove the flow from the network node
     *
     * @param node
     * @param flow
     * @param rid
     */
    Status removeFlowAsync(Node node, Flow flow, long rid);

    /**
     * Remove all flows present on the network node
     *
     * @param node
     */
    Status removeAllFlows(Node node);

    /**
     * Send Barrier message synchronously. The caller will be blocked until the
     * Barrier reply arrives.
     *
     * @param node
     */
    Status syncSendBarrierMessage(Node node);

    /**
     * Send Barrier message asynchronously. The caller is not blocked.
     *
     * @param node
     */
    Status asyncSendBarrierMessage(Node node);
}
