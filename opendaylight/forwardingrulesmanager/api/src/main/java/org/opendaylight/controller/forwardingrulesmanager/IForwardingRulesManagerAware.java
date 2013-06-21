/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.forwardingrulesmanager;

/**
 * The interface which describes the methods forwarding rules manager will call
 * for notifying the listeners of policy installation updates.
 */
public interface IForwardingRulesManagerAware {

    /**
     * Inform the listeners that a troubleshooting information was added or
     * removed for the specified policy.
     *
     * @param policyName
     *            the policy affected
     * @param add
     *            true if the troubleshooting information was added, false
     *            otherwise
     */
    public void policyUpdate(String policyName, boolean add);

    /**
     * Inform listeners that the network node has notified us about a failure in
     * executing the controller generated asynchronous request identified by the
     * passed unique id.
     *
     * @param requestId
     *            the unique id associated with the request which failed to be
     *            executed on the network node
     * @param error
     *            the string describing the error reported by the network node
     */
    public void requestFailed(long requestId, String error);

}
