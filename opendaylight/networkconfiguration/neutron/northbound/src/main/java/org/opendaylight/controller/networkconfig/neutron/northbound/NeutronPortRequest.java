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
import org.opendaylight.controller.networkconfig.neutron.NeutronPort;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class NeutronPortRequest implements INeutronRequest<NeutronPort> {
    // See OpenStack Network API v2.0 Reference for description of
    // annotated attributes

    @XmlElement(name="port")
    NeutronPort singletonPort;

    @XmlElement(name="ports")
    List<NeutronPort> bulkRequest;

    @XmlElement(name="ports_links")
    List<NeutronPageLink> links;

    NeutronPortRequest() {
    }

    public NeutronPortRequest(List<NeutronPort> bulkRequest, List<NeutronPageLink> links) {
        this.bulkRequest = bulkRequest;
        this.links = links;
        this.singletonPort = null;
    }

    NeutronPortRequest(List<NeutronPort> bulk) {
        bulkRequest = bulk;
        singletonPort = null;
    }

    NeutronPortRequest(NeutronPort port) {
        singletonPort = port;
    }

    @Override
    public NeutronPort getSingleton() {
        return singletonPort;
    }

    @Override
    public boolean isSingleton() {
        return (singletonPort != null);
    }

    @Override
    public List<NeutronPort> getBulk() {
        return bulkRequest;
    }
}
