
package org.opendaylight.controller.containermanager.northbound;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.containermanager.ContainerConfig;


@XmlRootElement(name = "container-config-list")
@XmlAccessorType(XmlAccessType.NONE)
public class ContainerConfigs {
        @XmlElement(name = "container-config")
    List<ContainerConfig> containerConfig;

    //To satisfy JAXB
    @SuppressWarnings("unused")
    private ContainerConfigs() {

    }


    public ContainerConfigs(List<ContainerConfig> containerconfig) {
        this.containerConfig = containerconfig;
    }


    public List<ContainerConfig> getcontainerConfig() {
        return containerConfig;
    }

    public void setcontainerConfig(List<ContainerConfig> containerConfig) {
        this.containerConfig = containerConfig;
    }
}
