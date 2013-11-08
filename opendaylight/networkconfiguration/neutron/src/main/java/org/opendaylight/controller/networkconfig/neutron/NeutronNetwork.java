/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "network")
@XmlAccessorType(XmlAccessType.NONE)

public class NeutronNetwork {
    // See OpenStack Network API v2.0 Reference for description of
    // annotated attributes

    @XmlElement (name="id")
    String networkUUID;              // network UUID

    @XmlElement (name="name")
    String networkName;              // name

    @XmlElement (defaultValue="true", name="admin_state_up")
    Boolean adminStateUp;             // admin state up (true/false)

    @XmlElement (defaultValue="false", name="shared")
    Boolean shared;                   // shared network or not

    @XmlElement (name="tenant_id")
    String tenantID;                 // tenant for this network

    @XmlElement (defaultValue="false", namespace="router", name="external")
    Boolean routerExternal;           // network external or not

    @XmlElement (defaultValue="flat", namespace="provider", name="network_type")
    String providerNetworkType;      // provider network type (flat or vlan)

    @XmlElement (namespace="provider", name="physical_network")
    String providerPhysicalNetwork;  // provider physical network (name)

    @XmlElement (namespace="provider", name="segmentation_id")
    String providerSegmentationID;   // provide segmentation ID (vlan ID)

    @XmlElement (name="status")
    String status;                   // status (read-only)

    @XmlElement (name="subnets")
    List<String> subnets;            // subnets (read-only)

    /* This attribute lists the ports associated with an instance
     * which is needed for determining if that instance can be deleted
     */

    List<NeutronPort> myPorts;

    public NeutronNetwork() {
        myPorts = new ArrayList<NeutronPort>();
    }

    public void initDefaults() {
        subnets = new ArrayList<String>();
        if (status == null) {
            status = "ACTIVE";
        }
        if (adminStateUp == null) {
            adminStateUp = true;
        }
        if (shared == null) {
            shared = false;
        }
        if (routerExternal == null) {
            routerExternal = false;
        }
        if (providerNetworkType == null) {
            providerNetworkType = "flat";
        }
    }

    public String getID() { return networkUUID; }

    public String getNetworkUUID() {
        return networkUUID;
    }

    public void setNetworkUUID(String networkUUID) {
        this.networkUUID = networkUUID;
    }

    public String getNetworkName() {
        return networkName;
    }

    public void setNetworkName(String networkName) {
        this.networkName = networkName;
    }

    public boolean isAdminStateUp() {
        return adminStateUp;
    }

    public Boolean getAdminStateUp() { return adminStateUp; }

    public void setAdminStateUp(boolean newValue) {
        adminStateUp = newValue;
    }

    public boolean isShared() { return shared; }

    public Boolean getShared() { return shared; }

    public void setShared(boolean newValue) {
        shared = newValue;
    }

    public String getTenantID() {
        return tenantID;
    }

    public void setTenantID(String tenantID) {
        this.tenantID = tenantID;
    }

    public boolean isRouterExternal() { return routerExternal; }

    public Boolean getRouterExternal() { return routerExternal; }

    public void setRouterExternal(boolean newValue) {
        routerExternal = newValue;
    }

    public String getProviderNetworkType() {
        return providerNetworkType;
    }

    public void setProviderNetworkType(String providerNetworkType) {
        this.providerNetworkType = providerNetworkType;
    }

    public String getProviderPhysicalNetwork() {
        return providerPhysicalNetwork;
    }

    public void setProviderPhysicalNetwork(String providerPhysicalNetwork) {
        this.providerPhysicalNetwork = providerPhysicalNetwork;
    }

    public String getProviderSegmentationID() {
        return providerSegmentationID;
    }

    public void setProviderSegmentationID(String providerSegmentationID) {
        this.providerSegmentationID = providerSegmentationID;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getSubnets() {
        return subnets;
    }

    public void setSubnets(List<String> subnets) {
        this.subnets = subnets;
    }

    public void addSubnet(String uuid) {
        subnets.add(uuid);
    }

    public void removeSubnet(String uuid) {
        subnets.remove(uuid);
    }

    public List<NeutronPort> getPortsOnNetwork() {
        return myPorts;
    }

    public void addPort(NeutronPort port) {
        myPorts.add(port);
    }

    public void removePort(NeutronPort port) {
        myPorts.remove(port);
    }

    /**
     * This method copies selected fields from the object and returns them
     * as a new object, suitable for marshaling.
     *
     * @param fields
     *            List of attributes to be extracted
     * @return an OpenStackNetworks object with only the selected fields
     * populated
     */

    public NeutronNetwork extractFields(List<String> fields) {
        NeutronNetwork ans = new NeutronNetwork();
        Iterator<String> i = fields.iterator();
        while (i.hasNext()) {
            String s = i.next();
            if (s.equals("id")) {
                ans.setNetworkUUID(this.getNetworkUUID());
            }
            if (s.equals("name")) {
                ans.setNetworkName(this.getNetworkName());
            }
            if (s.equals("admin_state_up")) {
                ans.setAdminStateUp(adminStateUp);
            }
            if (s.equals("status")) {
                ans.setStatus(this.getStatus());
            }
            if (s.equals("subnets")) {
                List<String> subnetList = new ArrayList<String>();
                subnetList.addAll(this.getSubnets());
                ans.setSubnets(subnetList);
            }
            if (s.equals("shared")) {
                ans.setShared(shared);
            }
            if (s.equals("tenant_id")) {
                ans.setTenantID(this.getTenantID());
            }
        }
        return ans;
    }

}

