package org.opendaylight.controller.networkconfig.neutron;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlEnum
public enum NeutronSecurityGroupRule_Protocol {
    // See OpenStack Network API v2.0 Reference for description of
    // annotated attributes

    @XmlEnumValue("null") NULL{
        @Override
        public String toString() {
            return "null";
        }
    },
    @XmlEnumValue("tcp") TCP {
        @Override
        public String toString() {
            return "tcp";
        }
    },
    @XmlEnumValue("upd") UDP {
        @Override
        public String toString() {
            return "upd";
        }
    },
    @XmlEnumValue("icmp") ICMP {
        @Override
        public String toString() {
            return "icmp";
        }
    }
}