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

import org.opendaylight.controller.networkconfig.neutron.NeutronFloatingIP;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class NeutronFloatingIPRequest {
    // See OpenStack Network API v2.0 Reference for description of
    // annotated attributes

    @XmlElement(name="floatingip")
    NeutronFloatingIP singletonFloatingIP;

    @XmlElement(name="floatingips")
    List<NeutronFloatingIP> bulkRequest;

    NeutronFloatingIPRequest() {
    }

    NeutronFloatingIPRequest(List<NeutronFloatingIP> bulk) {
        bulkRequest = bulk;
        singletonFloatingIP = null;
    }

    NeutronFloatingIPRequest(NeutronFloatingIP singleton) {
        bulkRequest = null;
        singletonFloatingIP = singleton;
    }

    public NeutronFloatingIP getSingleton() {
        return singletonFloatingIP;
    }

    public boolean isSingleton() {
        return (singletonFloatingIP != null);
    }
}
