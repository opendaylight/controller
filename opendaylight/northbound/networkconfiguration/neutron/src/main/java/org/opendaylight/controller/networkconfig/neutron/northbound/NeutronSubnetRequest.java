/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron.northbound;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.networkconfig.neutron.NeutronSubnet;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)

public class NeutronSubnetRequest implements INeutronRequest {
    // See OpenStack Network API v2.0 Reference for description of
    // annotated attributes

    @XmlElement(name="subnet")
    NeutronSubnet singletonSubnet;

    @XmlElement(name="subnets")
    List<NeutronSubnet> bulkRequest;

    @XmlElement(name="subnets_links")
    List<NeutronPageLink> links;

    NeutronSubnetRequest() {
    }

    public NeutronSubnetRequest(List<NeutronSubnet> bulkRequest, List<NeutronPageLink> links) {
        this.bulkRequest = bulkRequest;
        this.links = links;
        this.singletonSubnet = null;
    }

    NeutronSubnetRequest(List<NeutronSubnet> bulk) {
        bulkRequest = bulk;
        singletonSubnet = null;
        links = null;
    }

    NeutronSubnetRequest(NeutronSubnet subnet) {
        singletonSubnet = subnet;
        bulkRequest = null;
        links = null;
    }

    public NeutronSubnet getSingleton() {
        return singletonSubnet;
    }

    public List<NeutronSubnet> getBulk() {
        return bulkRequest;
    }

    public boolean isSingleton() {
        return (singletonSubnet != null);
    }
}
