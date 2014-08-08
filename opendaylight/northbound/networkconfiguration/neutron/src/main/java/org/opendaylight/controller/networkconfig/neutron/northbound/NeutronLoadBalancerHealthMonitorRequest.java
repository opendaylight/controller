/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron.northbound;

import org.opendaylight.controller.networkconfig.neutron.NeutronLoadBalancerHealthMonitor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;


@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)

public class NeutronLoadBalancerHealthMonitorRequest {
    /**
     * See OpenStack Network API v2.0 Reference for description of
     * http://docs.openstack.org/api/openstack-network/2.0/content/
     */

    @XmlElement(name="healthmonitor")
    NeutronLoadBalancerHealthMonitor singletonLoadBalancerHealthMonitor;

    @XmlElement(name="healthmonitors")
    List<NeutronLoadBalancerHealthMonitor> bulkRequest;

    NeutronLoadBalancerHealthMonitorRequest() {
    }

    NeutronLoadBalancerHealthMonitorRequest(List<NeutronLoadBalancerHealthMonitor> bulk) {
        bulkRequest = bulk;
        singletonLoadBalancerHealthMonitor = null;
    }

    NeutronLoadBalancerHealthMonitorRequest(NeutronLoadBalancerHealthMonitor group) {
        singletonLoadBalancerHealthMonitor = group;
    }

    public List<NeutronLoadBalancerHealthMonitor> getBulk() {
        return bulkRequest;
    }

    public NeutronLoadBalancerHealthMonitor getSingleton() {
        return singletonLoadBalancerHealthMonitor;
    }

    public boolean isSingleton() {
        return (singletonLoadBalancerHealthMonitor != null);
    }
}