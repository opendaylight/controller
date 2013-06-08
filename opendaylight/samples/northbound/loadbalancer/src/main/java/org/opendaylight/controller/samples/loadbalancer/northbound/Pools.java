/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.samples.loadbalancer.northbound;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.samples.loadbalancer.entities.Pool;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
/**
 * JAX-RS resource for handling details of all the available pools 
 * in response to respective REST API requests.
 */

public class Pools {
    
    @XmlElement (name="pool")
    Set<Pool> loadBalancerPools;
    
    public Pools() {
    }
    
    public Pools (Set<Pool> loadBalancerPools) {
        this.loadBalancerPools = loadBalancerPools;
    }
    
    /**
     * @return the loadBalancerPools
     */
    public Set<Pool> getLoadBalancerPools() {
        return loadBalancerPools;
    }
    
    /**
     * @param loadBalancerPools the loadBalancerPools to set
     */
    public void setLoadBalancerPools(Set<Pool> loadBalancerPools) {
        this.loadBalancerPools = loadBalancerPools;
    }
}
