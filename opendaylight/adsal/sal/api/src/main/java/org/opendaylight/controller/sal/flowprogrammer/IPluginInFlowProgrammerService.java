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
 * This interface defines the flow programmer methods to be implemented by
 * protocol plugins
 */
@Deprecated
public interface IPluginInFlowProgrammerService {
    /**
     * Synchronously add a flow to the network node
     *
     * @param node
     *            the network node
     *            {@link org.opendaylight.controller.sal.core.Node} on which the
     *            flow got added
     * @param flow
     *            the flow
     *            {@link org.opendaylight.controller.sal.flowprogrammer.Flow}
     *            that got added
     * @return Status the operation status
     *         {@link org.opendaylight.controller.sal.utils.Status}
     */
    Status addFlow(Node node, Flow flow);

    /**
     * Synchronously modify existing flow on the switch
     *
     * @param node
     *            the network node
     *            {@link org.opendaylight.controller.sal.core.Node} on which the
     *            flow got modified
     * @param flow
     *            the flow
     *            {@link org.opendaylight.controller.sal.flowprogrammer.Flow}
     *            that got modified
     * @return Status the operation status
     *         {@link org.opendaylight.controller.sal.utils.Status}
     */
    Status modifyFlow(Node node, Flow oldFlow, Flow newFlow);

    /**
     * Synchronously remove the flow from the network node
     *
     * @param node
     *            the network node
     *            {@link org.opendaylight.controller.sal.core.Node} on which the
     *            flow got removed
     * @param flow
     *            the flow
     *            {@link org.opendaylight.controller.sal.flowprogrammer.Flow}
     *            that got removed
     * @return Status the operation status
     *         {@link org.opendaylight.controller.sal.utils.Status}
     */
    Status removeFlow(Node node, Flow flow);

    /**
     * Asynchronously add a flow to the network node
     *
     * @param node
     *            the network node
     *            {@link org.opendaylight.controller.sal.core.Node} on which the
     *            flow got added
     * @param flow
     *            the flow
     *            {@link org.opendaylight.controller.sal.flowprogrammer.Flow}
     *            that got added
     * @param rid
     *            the request id
     * @return Status the operation status
     *         {@link org.opendaylight.controller.sal.utils.Status}
     */
    Status addFlowAsync(Node node, Flow flow, long rid);

    /**
     * Asynchronously modify existing flow on the switch
     *
     * @param node
     *            the network node
     *            {@link org.opendaylight.controller.sal.core.Node} on which the
     *            flow got modified
     * @param oldFlow
     *            the original flow
     *            {@link org.opendaylight.controller.sal.flowprogrammer.Flow}
     * @param newFlow
     *            the new flow
     *            {@link org.opendaylight.controller.sal.flowprogrammer.Flow}
     * @param rid
     *            the request id
     * @return Status the operation status
     *         {@link org.opendaylight.controller.sal.utils.Status}
     */
    Status modifyFlowAsync(Node node, Flow oldFlow, Flow newFlow, long rid);

    /**
     * Asynchronously remove the flow from the network node
     *
     * @param node
     *            the network node
     *            {@link org.opendaylight.controller.sal.core.Node} on which the
     *            flow got removed
     * @param flow
     *            the flow
     *            {@link org.opendaylight.controller.sal.flowprogrammer.Flow}
     *            that got removed
     * @param rid
     *            the request id
     * @return Status the operation status
     *         {@link org.opendaylight.controller.sal.utils.Status}
     */
    Status removeFlowAsync(Node node, Flow flow, long rid);

    /**
     * Remove all flows present on the network node
     *
     * @param node
     *            the network node
     *            {@link org.opendaylight.controller.sal.core.Node} on which the
     *            flow got removed
     * @return Status the operation status
     *         {@link org.opendaylight.controller.sal.utils.Status}
     */
    Status removeAllFlows(Node node);

    /**
     * Send Barrier message synchronously. The caller will be blocked until the
     * Barrier reply arrives.
     *
     * @param node
     *            the network node
     *            {@link org.opendaylight.controller.sal.core.Node}
     * @return Status the operation status
     *         {@link org.opendaylight.controller.sal.utils.Status}
     */
    Status syncSendBarrierMessage(Node node);

    /**
     * Send Barrier message asynchronously. The caller is not blocked.
     *
     * @param node
     *            the network node
     *            {@link org.opendaylight.controller.sal.core.Node}
     * @return Status the operation status
     *         {@link org.opendaylight.controller.sal.utils.Status}
     */
    Status asyncSendBarrierMessage(Node node);
}
