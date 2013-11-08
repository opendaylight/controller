/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)

public class NeutronRouter {
    // See OpenStack Network API v2.0 Reference for description of
    // annotated attributes
    @XmlElement (name="id")
    String routerUUID;

    @XmlElement (name="name")
    String name;

    @XmlElement (defaultValue="true", name="admin_state_up")
    Boolean adminStateUp;

    @XmlElement (name="status")
    String status;

    @XmlElement (name="tenant_id")
    String tenantID;

    @XmlElement (name="external_gateway_info")
    NeutronRouter_NetworkReference externalGatewayInfo;

    /* Holds a map of OpenStackRouterInterfaces by subnet UUID
     * used for internal mapping to DOVE
     */
    HashMap<String, NeutronRouter_Interface> interfaces;

    public NeutronRouter() {
        interfaces = new HashMap<String, NeutronRouter_Interface>();
    }

    public String getID() { return routerUUID; }

    public String getRouterUUID() {
        return routerUUID;
    }

    public void setRouterUUID(String routerUUID) {
        this.routerUUID = routerUUID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isAdminStateUp() {
        if (adminStateUp == null) {
            return true;
        }
        return adminStateUp;
    }

    public Boolean getAdminStateUp() { return adminStateUp; }

    public void setAdminStateUp(Boolean newValue) {
        adminStateUp = newValue;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTenantID() {
        return tenantID;
    }

    public void setTenantID(String tenantID) {
        this.tenantID = tenantID;
    }

    public NeutronRouter_NetworkReference getExternalGatewayInfo() {
        return externalGatewayInfo;
    }

    public void setExternalGatewayInfo(NeutronRouter_NetworkReference externalGatewayInfo) {
        this.externalGatewayInfo = externalGatewayInfo;
    }

    /**
     * This method copies selected fields from the object and returns them
     * as a new object, suitable for marshaling.
     *
     * @param fields
     *            List of attributes to be extracted
     * @return an OpenStackRouters object with only the selected fields
     * populated
     */

    public NeutronRouter extractFields(List<String> fields) {
        NeutronRouter ans = new NeutronRouter();
        Iterator<String> i = fields.iterator();
        while (i.hasNext()) {
            String s = i.next();
            if (s.equals("id")) {
                ans.setRouterUUID(this.getRouterUUID());
            }
            if (s.equals("name")) {
                ans.setName(this.getName());
            }
            if (s.equals("admin_state_up")) {
                ans.setAdminStateUp(this.getAdminStateUp());
            }
            if (s.equals("status")) {
                ans.setStatus(this.getStatus());
            }
            if (s.equals("tenant_id")) {
                ans.setTenantID(this.getTenantID());
            }
            if (s.equals("external_gateway_info")) {
                ans.setExternalGatewayInfo(this.getExternalGatewayInfo());
            }
        }
        return ans;
    }

    public HashMap<String, NeutronRouter_Interface> getInterfaces() {
        return interfaces;
    }

    public void addInterface(String s, NeutronRouter_Interface i) {
        interfaces.put(s, i);
    }

    public void removeInterface(String s) {
        interfaces.remove(s);
    }

    public void initDefaults() {
        adminStateUp = true;
    }
}
