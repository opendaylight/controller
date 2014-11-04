/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron.northbound;

import org.opendaylight.controller.networkconfig.neutron.NeutronLoadBalancer;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;


@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)

public class NeutronLoadBalancerRequest {
    /**
     * See OpenStack Network API v2.0 Reference for description of
     * http://docs.openstack.org/api/openstack-network/2.0/content/
     */

    @XmlElement(name="loadbalancer")
    NeutronLoadBalancer singletonLoadBalancer;

    @XmlElement(name="loadbalancers")
    List<NeutronLoadBalancer> bulkRequest;

    NeutronLoadBalancerRequest() {
    }

    NeutronLoadBalancerRequest(List<NeutronLoadBalancer> bulk) {
        bulkRequest = bulk;
        singletonLoadBalancer = null;
    }

    NeutronLoadBalancerRequest(NeutronLoadBalancer group) {
        singletonLoadBalancer = group;
    }

    public List<NeutronLoadBalancer> getBulk() {
        return bulkRequest;
    }

    public NeutronLoadBalancer getSingleton() {
        return singletonLoadBalancer;
    }

    public boolean isSingleton() {
        return (singletonLoadBalancer != null);
    }
}