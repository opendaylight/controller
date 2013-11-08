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

import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class NeutronNetworkRequest {
    // See OpenStack Network API v2.0 Reference for description of
    // annotated attributes

    @XmlElement(name="network")
    NeutronNetwork singletonNetwork;

    @XmlElement(name="networks")
    List<NeutronNetwork> bulkRequest;

    NeutronNetworkRequest() {
    }

    NeutronNetworkRequest(List<NeutronNetwork> bulk) {
        bulkRequest = bulk;
        singletonNetwork = null;
    }

    NeutronNetworkRequest(NeutronNetwork net) {
        singletonNetwork = net;
    }

    public NeutronNetwork getSingleton() {
        return singletonNetwork;
    }

    public boolean isSingleton() {
        return (singletonNetwork != null);
    }

    public List<NeutronNetwork> getBulk() {
        return bulkRequest;
    }
}
