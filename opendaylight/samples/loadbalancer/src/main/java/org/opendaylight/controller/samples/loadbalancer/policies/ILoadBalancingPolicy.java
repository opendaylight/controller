/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.samples.loadbalancer.policies;

import org.opendaylight.controller.samples.loadbalancer.entities.Client;
import org.opendaylight.controller.samples.loadbalancer.entities.VIP;

/**
 * All new load balancer policies must implement this interface.
 */
public interface ILoadBalancingPolicy {
    
    /**
     * Returns IP address of the next pool member from the pool
     * to which the load balancer service can direct incoming packets.
     * @param source    source on the packet
     * @param dest      virtual IP (VIP) that is used as destination on the packet
     * @return	IP address of the next pool member which will serve
     *          all incoming traffic destined for the given VIP and with the given source
     *          information
     */
    public String getPoolMemberForClient(Client source, VIP dest);
    
}