/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.networkconfig.neutron.northbound;

import org.opendaylight.controller.networkconfig.neutron.NeutronLoadBalancerPoolMember;

import javax.xml.bind.annotation.XmlElement;
import java.util.List;

public class NeutronLoadBalancerPoolMemberRequest {

    /**
     * See OpenStack Network API v2.0 Reference for description of
     * http://docs.openstack.org/api/openstack-network/2.0/content/
     */

    @XmlElement(name="member")
    NeutronLoadBalancerPoolMember singletonLoadBalancerPoolMember;

    @XmlElement(name="members")
    List<NeutronLoadBalancerPoolMember> bulkRequest;

    NeutronLoadBalancerPoolMemberRequest() {
    }

    NeutronLoadBalancerPoolMemberRequest(List<NeutronLoadBalancerPoolMember> bulk) {
        bulkRequest = bulk;
        singletonLoadBalancerPoolMember = null;
    }

    NeutronLoadBalancerPoolMemberRequest(NeutronLoadBalancerPoolMember group) {
        singletonLoadBalancerPoolMember = group;
    }

    public List<NeutronLoadBalancerPoolMember> getBulk() {
        return bulkRequest;
    }

    public NeutronLoadBalancerPoolMember getSingleton() {
        return singletonLoadBalancerPoolMember;
    }

    public boolean isSingleton() {
        return (singletonLoadBalancerPoolMember != null);
    }
}