package org.opendaylight.controller.sal.networkconfig.bridgedomain;

/**
 * Enum constant that is used as a key for the configuration parameters for BridgeDomains and Ports.
 * The main intention of having a constant type is to avoid fragmentation and find common grounds for
 * applications to rely on.
 *
 * This is set to expand based on various capabilities south-bound protocol might expose.
 * Not all of them be supported by all the plugins. But this gives a consolidated view of
 * all the supported feature configs and avoid config fragmentation.
 */
public enum ConfigConstants {
    TYPE("type"),
    VLAN("Vlan"),
    VLAN_MODE("vlan_mode"),
    TUNNEL_TYPE("Tunnel Type"),
    SOURCE_IP("Source IP"),
    DEST_IP("Destination IP"),
    MACADDRESS("MAC Address"),
    INTERFACE_IDENTIFIER("Interface Identifier"),
    MGMT("Management"),
    CUSTOM("Custom Configurations");

    private ConfigConstants(String name) {
        this.name = name;
    }

    private String name;

    public String toString() {
        return name;
    }
}