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

import org.opendaylight.controller.samples.loadbalancer.entities.VIP;

/**
 * JAX-RS resource for handling details of all the available VIPs
 * in response to respective REST API requests.
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)

public class VIPs {
    
    @XmlElement (name="vip")
    Set<VIP> loadBalancerVIPs;
    
    public VIPs() {}
    
    
    public VIPs (Set<VIP> loadBalancerVIPs) {
        this.loadBalancerVIPs = loadBalancerVIPs;
    }
    
    /**
     * @return the loadBalancerVIPs
     */
    public Set<VIP> getLoadBalancerVIPs() {
        return loadBalancerVIPs;
    }
    
    /**
     * @param loadBalancerVIPs the loadBalancerVIPs to set
     */
    
    public void setLoadBalancerVIPs(Set<VIP> loadBalancerVIPs) {
        this.loadBalancerVIPs = loadBalancerVIPs;
    }
}