package org.opendaylight.controller.networkconfig.neutron;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlEnum
public enum NeutronSecurityGroupRule_Ethertype {
    // See OpenStack Network API v2.0 Reference for description of
    // annotated attributes

    @XmlEnumValue("IPv4") IPv4 {
        @Override
        public String toString() {
            return "IPv4";
        }
    },
    @XmlEnumValue("IPv6") IPv6 {
        @Override
        public String toString() {
            return "IPv6";
        }
    }
}
