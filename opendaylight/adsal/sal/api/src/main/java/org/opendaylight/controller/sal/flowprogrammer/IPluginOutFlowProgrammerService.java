/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.flowprogrammer;

import org.opendaylight.controller.sal.core.Node;

/**
 * This interface defines the methods the protocol plugin must implement to
 * inform the SAL layer about the asynchronous messages related to flow
 * programming coming from the network nodes.
 */
@Deprecated
public interface IPluginOutFlowProgrammerService {
    /**
     * Inform SAL that the flow on the specified node has been removed Consumer
     * has to expect this notification only for flows which were installed with
     * an idle or hard timeout specified.
     *
     * @param node
     *            the network node on which the flow got removed
     * @param flow
     *            the flow that got removed. Note: It may contain only the Match
     *            and flow parameters fields. Actions may not be present.
     */
    public void flowRemoved(Node node, Flow flow);

    /**
     * Inform SAL that an error message has been received from a switch
     * regarding a flow message previously sent to the switch. A Request ID
     * associated with the offending message is also returned.
     *
     * @param node
     *            the network node on which the error reported
     * @param rid
     *            the offending message request id
     * @param err
     *            the error message
     */
    public void flowErrorReported(Node node, long rid, Object err);
}
