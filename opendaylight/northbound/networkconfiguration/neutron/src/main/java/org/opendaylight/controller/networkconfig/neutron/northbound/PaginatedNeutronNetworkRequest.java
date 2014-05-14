/*
 * Copyright (C) 2014 Hewlett-Packard Development Company L.P
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Dave Tucker
 */

package org.opendaylight.controller.networkconfig.neutron.northbound;

import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)

public class PaginatedNeutronNetworkRequest {

    @XmlElement (name="networks")
    List<NeutronNetwork> networks;

    @XmlElement (name="network_links")
    List<NeutronPageLink> networkLinks;

    public PaginatedNeutronNetworkRequest() {
    }

    public PaginatedNeutronNetworkRequest(List<NeutronNetwork> networks, List<NeutronPageLink> networkLinks) {
        this.networks = networks;
        this.networkLinks = networkLinks;
    }

    public List<NeutronNetwork> getNetworks() {
        return networks;
    }

    public void setNetworks(List<NeutronNetwork> networks) {
        this.networks = networks;
    }

    public List<NeutronPageLink> getNetworkLinks() {
        return networkLinks;
    }

    public void setNetworkLinks(List<NeutronPageLink> networkLinks) {
        this.networkLinks = networkLinks;
    }
}
