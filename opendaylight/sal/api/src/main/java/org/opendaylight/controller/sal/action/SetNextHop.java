package org.opendaylight.controller.sal.action;

import java.net.InetAddress;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class SetNextHop extends Action {
    @XmlElement
    private InetAddress address;

    /* Dummy constructor for JAXB */
    @SuppressWarnings("unused")
    private SetNextHop() {
    }

    public SetNextHop(InetAddress address) {
        type = ActionType.SET_NEXT_HOP;
        this.address = address;
    }

    public InetAddress getAddress() {
        return address;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((address == null) ? 0 : address.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SetNextHop other = (SetNextHop) obj;
        if (address == null) {
            if (other.address != null) {
                return false;
            }
        } else if (!address.equals(other.address)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return type + "[" + address.toString() + "]";
    }
}
