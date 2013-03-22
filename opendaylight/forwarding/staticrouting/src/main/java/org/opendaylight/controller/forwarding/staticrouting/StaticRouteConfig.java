
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.forwarding.staticrouting;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.opendaylight.controller.sal.utils.GUIField;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;

/**
 * This class defines all the necessary configuration information for a static route.
 */
public class StaticRouteConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String regexSubnet = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])[/](\\d|[12]\\d|3[0-2])$";
    private static final String regexIP = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
    private static final String regexDatapathID = "^([0-9a-fA-F]{1,2}[:-]){7}[0-9a-fA-F]{1,2}$";
    private static final String regexDatapathIDLong = "^[0-9a-fA-F]{1,16}$";
    private static final String prettyFields[] = { GUIField.NAME.toString(),
            GUIField.STATICROUTE.toString(), GUIField.NEXTHOP.toString() };
    private transient String nextHopType; // Ignoring NextHopType for now. Supporting just the next-hop IP-Address feature for now.
    // Order matters: JSP file expects following fields in the following order
    private String name;
    private String staticRoute; // A.B.C.D/MM  Where A.B.C.D is the Default Gateway IP (L3) or ARP Querier IP (L2)
    private String nextHop; // NextHop IP-Address (or) datapath ID/port list: xx:xx:xx:xx:xx:xx:xx:xx/a,b,c-m,r-t,y

    /**
     * Create a static route configuration  with no specific information.
     */
    public StaticRouteConfig() {
        super();
        nextHopType = StaticRoute.NextHopType.IPADDRESS.toString();
    }

    /**
     * Create a static route configuration with all the information.
     * @param name The name (String) of the static route config
     * @param staticRoute The string representation of the route. e.g. 192.168.1.1/24
     * @param nextHop The string representation of the next hop IP address. e.g. 10.10.1.1
     */
    public StaticRouteConfig(String name, String staticRoute, String nextHop) {
        super();
        this.name = name;
        this.staticRoute = staticRoute;
        this.nextHop = nextHop;
    }

    /**
     * Get the name of the StaticRouteConfig.
     * @return: The name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of the StaticRouteConfig.
     * @param name The name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the string representation of the static route.
     * @return The string representation of the static route
     */
    public String getStaticRoute() {
        return staticRoute;
    }

    /**
     * Set the static route of the StaticRouteConfig.
     * @param staticRoute The string representation of the static route
     */
    public void setStaticRoute(String staticRoute) {
        this.staticRoute = staticRoute;
    }

    /**
     * Get the string representation of the next hop address type.
     * @return The string representation of the next hop address type
     */
    public String getNextHopType() {
        if (nextHopType == null)
            return StaticRoute.NextHopType.IPADDRESS.toString();
        return nextHopType;
    }

    /**
     * Set the next hop address type.
     * @param nextHopType The string representation of the next hop address type
     */
    public void setNextHopType(String nextHopType) {
        this.nextHopType = nextHopType;
    }

    /**
     * Get all the supported next hop address types.
     * @return The list of supported next hop address types
     */
    public static List<String> getSupportedNextHopTypes() {
        List<String> s = new ArrayList<String>();
        for (StaticRoute.NextHopType nh : StaticRoute.NextHopType.values()) {
            s.add(nh.toString());
        }
        return s;
    }

    /**
     * Get the next hop address
     * @return The string represenation of the next hop address
     */
    public String getNextHop() {
        return nextHop;
    }

    /**
     * Set the next hop address.
     * @param nextHop: The string representation of the next hop address to be set
     */
    public void setNextHop(String nextHop) {
        this.nextHop = nextHop;
    }

    /**
     * Return a string with text indicating if the config is valid.
     * @return SUCCESS if the config is valid
     */
    public Status isValid() {
        if ((name == null) || (name.trim().length() < 1)) {
            return new Status(StatusCode.BADREQUEST,
            		"Invalid Static Route name");
        }
        if (!isValidStaticRouteEntry()) {
            return new Status(StatusCode.BADREQUEST,
            		"Invalid Static Route entry. Please use the " +
            		"IPAddress/mask format. Default gateway " +
            		"(0.0.0.0/0) is NOT supported.");
        }
        if (!isValidNextHop()) {
            return new Status(StatusCode.BADREQUEST,
            		"Invalid NextHop IP Address configuration. " +
            				"Please use the X.X.X.X format.");
        }

        return new Status(StatusCode.SUCCESS, null);
    }

    private boolean isValidAddress(String address) {
        if ((address != null) && address.matches(regexIP)) {
            return true;
        }
        return false;
    }

    private boolean isValidStaticRouteEntry() {
        if ((staticRoute != null) && staticRoute.matches(regexSubnet)) {
            return true;
        }
        return false;
    }

    private boolean isValidNextHop() {
        if (getNextHopType().equalsIgnoreCase(
                StaticRoute.NextHopType.IPADDRESS.toString())) {
            return isValidNextHopIP();
        } else if (getNextHopType().equalsIgnoreCase(
                StaticRoute.NextHopType.SWITCHPORT.toString())) {
            return isValidSwitchId();
        }
        return false;
    }

    private boolean isValidNextHopIP() {
        return isValidAddress(nextHop);
    }

    private boolean isValidSwitchId(String switchId) {
        return (switchId != null && (switchId.matches(regexDatapathID) || switchId
                .matches(regexDatapathIDLong)));
    }

    private boolean isValidSwitchId() {
        if (getNextHopType().equalsIgnoreCase(
                StaticRoute.NextHopType.SWITCHPORT.toString())) {
            String pieces[] = nextHop.split("/");
            if (pieces.length < 2)
                return false;
            return isValidSwitchId(pieces[0]);
        }
        return false;
    }

    /**
     * Return the IP address of the static route.
     * @return The IP address
     */
    public InetAddress getStaticRouteIP() {
        if (!isValidStaticRouteEntry())
            return null;
        InetAddress ip = null;
        try {
            ip = InetAddress.getByName(staticRoute.split("/")[0]);
        } catch (UnknownHostException e1) {
            return null;
        }
        return ip;
    }

    /**
     * Return the bit-mask length of the static route.
     * @return The bit-mask length
     */
    public Short getStaticRouteMask() {
        Short maskLen = 0;
        if (isValidStaticRouteEntry()) {
            String[] s = staticRoute.split("/");
            maskLen = (s.length == 2) ? Short.valueOf(s[1]) : 32;
        }
        return maskLen;
    }

    /**
     * Return the IP address of the next hop.
     * @return the IP address
     */
    public InetAddress getNextHopIP() {
        if ((getNextHopType()
                .equalsIgnoreCase(StaticRoute.NextHopType.IPADDRESS.toString()))
                && isValidNextHopIP()) {
            InetAddress ip = null;
            try {
                ip = InetAddress.getByName(nextHop);
            } catch (UnknownHostException e1) {
                return null;
            }
            return ip;
        }
        return null;
    }

/**
 * Return the switch ID and the port ID of the next hop address.
 * @return The switchID (Long) and PortID (Short) in the map
 */
    public Map<Long, Short> getNextHopSwitchPorts() {
        // codedSwitchPorts = xx:xx:xx:xx:xx:xx:xx:xx/port-number
        if (getNextHopType().equalsIgnoreCase(
                StaticRoute.NextHopType.SWITCHPORT.toString())) {
            Map<Long, Short> sp = new HashMap<Long, Short>(1);
            String pieces[] = nextHop.split("/");
            sp.put(getSwitchIDLong(pieces[0]), Short.valueOf(pieces[1]));
            return sp;
        }
        return null;
    }

    private long getSwitchIDLong(String switchId) {
        int radix = 16;
        String switchString = "0";

        if (isValidSwitchId(switchId)) {
            if (switchId.contains(":")) {
                // Handle the 00:00:AA:BB:CC:DD:EE:FF notation
                switchString = switchId.replace(":", "");
            } else if (switchId.contains("-")) {
                // Handle the 00-00-AA-BB-CC-DD-EE-FF notation
                switchString = switchId.replace("-", "");
            } else {
                // Handle the 0123456789ABCDEF notation
                switchString = switchId;
            }
        }
        return Long.parseLong(switchString, radix);
    }

    /**
     * Return all the field names of the config.
     * @return The list containing all the field names
     */
    public static List<String> getFieldsNames() {
        List<String> fieldList = new ArrayList<String>();
        for (Field fld : StaticRouteConfig.class.getDeclaredFields()) {
            fieldList.add(fld.getName());
        }
        //remove the 6 static fields + NextHopType
        for (short i = 0; i < 7; i++) {
            fieldList.remove(0);
        }
        return fieldList;
    }

    /**
     * Return all the GUI field names of the config.
     * @return The list containing all the GUI field names
     */
    public static List<String> getGuiFieldsNames() {
        List<String> fieldList = new ArrayList<String>();
        for (String str : prettyFields) {
            fieldList.add(str);
        }
        return fieldList;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public String toString() {
        return "StaticRouteConfig [name=" + name + ", staticRoute="
                + staticRoute + ", nextHopType=" + nextHopType + ", nextHop="
                + nextHop + "]";
    }
}
