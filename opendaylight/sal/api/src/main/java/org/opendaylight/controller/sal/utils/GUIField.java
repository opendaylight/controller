
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.utils;

/**
 * GUI field constants
 *
 *
 *
 */
public enum GUIField {
    USER("User"), PASSWORD("Password"), ROLE("Role"), SERVERIP("Server address"), SERVERSECRET(
            "Server secret"), SERVERPROTOCOL("Server protocol"), NAME("Name"), CONTAINER(
            "Container"), SUBNET("Subnet"), GATEWAYIP("Gateway IP Address/Mask"), NODE(
            "Node"), NODEID("Node ID"), NODEMAC("Node MAC Address"), NODENAME(
            "Node Name"), SRCNODE("Source Node"), DSTNODE("Destination Node"), SRCPORT(
            "Source Port"), DSTPORT("Destination Port"), PORTS("Ports"), NODEPORTS(
            "Node/Ports"), SPANPORTS("Span Ports"), TIER("Tier"), MODE("Mode"), INPUTPORT(
            "Input Port"), ETHERTYPE("Ether Type"), DLSRCADDRESS("Source MAC"), DLDSTADDRESS(
            "Dest MAC"), VLANID("Vlan Id"), VLANPRIO("Vlan Priority"), NWSRCADDRESS(
            "Source IP"), NWDSTADDRESS("Dest IP"), NWPROTOCOL("Protocol"), NWPROTOCOLSHORT(
            "Proto"), NWTOSBITS("TOS"), TPSRCPORT("Transport Source Port"), TPDSTPORT(
            "Transport Dest Port"), TPSRCPORTSHORT("Source L4 Port"), TPDSTPORTSHORT(
            "Dest L4 Port"), METRIC("Metric"), STATUS("Status"), TAG("Tag"), TAGS(
            "Tags"), STATICVLAN("Static Vlan"), PRIORITY("Priority"), PORTGROUP(
            "Port Group"), COOKIE("Cookie"), ACTIONS("Actions"), ACTIVE(
            "Active"), IDLETIMEOUT("Idle Time Out"), HARDTIMEOUT(
            "Hard Time Out"), INHARDWARE("Install on Switch"), STATICROUTE(
            "Static Route"), NEXTHOPTYPE("NextHop Type"), NEXTHOP(
            "NextHop address");

    private GUIField(String name) {
        this.name = name;
    }

    private String name;

    public String toString() {
        return name;
    }
}
