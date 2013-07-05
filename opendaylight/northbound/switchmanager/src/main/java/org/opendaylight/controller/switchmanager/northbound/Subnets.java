package org.opendaylight.controller.switchmanager.northbound;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.switchmanager.SubnetConfig;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)

public class Subnets {
    @XmlElement
    List<SubnetConfig> subnetConfigs;
    //To satisfy JAXB
    private Subnets() {

    }

    public Subnets(List<SubnetConfig> subnetConfigs) {
            this.subnetConfigs = subnetConfigs;
    }

    public List<SubnetConfig> getSubnetConfigs() {
            return subnetConfigs;
    }

    public void setSubnetConfigs(List<SubnetConfig> subnetConfigs) {
            this.subnetConfigs = subnetConfigs;
    }
}
