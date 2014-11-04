/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron.northbound;

import org.opendaylight.controller.networkconfig.neutron.NeutronLoadBalancerListener;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;


@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)

public class NeutronLoadBalancerListenerRequest {
    /**
     * See OpenStack Network API v2.0 Reference for description of
     * http://docs.openstack.org/api/openstack-network/2.0/content/
     */

    @XmlElement(name="listener")
    NeutronLoadBalancerListener singletonLoadBalancerListener;

    @XmlElement(name="listeners")
    List<NeutronLoadBalancerListener> bulkRequest;

    NeutronLoadBalancerListenerRequest() {
    }

    NeutronLoadBalancerListenerRequest(List<NeutronLoadBalancerListener> bulk) {
        bulkRequest = bulk;
        singletonLoadBalancerListener = null;
    }

    NeutronLoadBalancerListenerRequest(NeutronLoadBalancerListener group) {
        singletonLoadBalancerListener = group;
    }

    public List<NeutronLoadBalancerListener> getBulk() {
        return bulkRequest;
    }

    public NeutronLoadBalancerListener getSingleton() {
        return singletonLoadBalancerListener;
    }

    public boolean isSingleton() {
        return (singletonLoadBalancerListener != null);
    }
}