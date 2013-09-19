/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class NeutronRouter_Interface {
    // See OpenStack Network API v2.0 Reference for description of
    // annotated attributes

    @XmlElement (name="subnet_id")
    String subnetUUID;

    @XmlElement (name="port_id")
    String portUUID;

    @XmlElement (name="id")
    String id;

    @XmlElement (name="tenant_id")
    String tenantID;

    public NeutronRouter_Interface() {
    }

    public NeutronRouter_Interface(String subnetUUID, String portUUID) {
        this.subnetUUID = subnetUUID;
        this.portUUID = portUUID;
    }

    public String getSubnetUUID() {
        return subnetUUID;
    }

    public void setSubnetUUID(String subnetUUID) {
        this.subnetUUID = subnetUUID;
    }

    public String getPortUUID() {
        return portUUID;
    }

    public void setPortUUID(String portUUID) {
        this.portUUID = portUUID;
    }

    public void setID(String id) {
        this.id = id;
    }

    public void setTenantID(String tenantID) {
        this.tenantID = tenantID;
    }
}
