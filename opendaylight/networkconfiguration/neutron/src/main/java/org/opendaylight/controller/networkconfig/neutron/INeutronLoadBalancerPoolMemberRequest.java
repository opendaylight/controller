/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron;

import javax.xml.bind.annotation.XmlElement;
import java.util.List;

public class INeutronLoadBalancerPoolMemberRequest {

    /**
     * See OpenStack Network API v2.0 Reference for description of
     * http://docs.openstack.org/api/openstack-network/2.0/content/
     */

    @XmlElement(name="member")
    NeutronLoadBalancerPoolMember singletonLoadBalancerPoolMember;

    @XmlElement(name="members")
    List<NeutronLoadBalancerPoolMember> bulkRequest;

    INeutronLoadBalancerPoolMemberRequest() {
    }

    public INeutronLoadBalancerPoolMemberRequest(List<NeutronLoadBalancerPoolMember> bulk) {
        bulkRequest = bulk;
        singletonLoadBalancerPoolMember = null;
    }

    INeutronLoadBalancerPoolMemberRequest(NeutronLoadBalancerPoolMember group) {
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