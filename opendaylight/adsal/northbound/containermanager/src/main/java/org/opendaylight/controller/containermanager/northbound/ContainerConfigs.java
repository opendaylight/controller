
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.controller.containermanager.northbound;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.containermanager.ContainerConfig;


@XmlRootElement(name = "containerConfig-list")
@XmlAccessorType(XmlAccessType.NONE)
public class ContainerConfigs {
        @XmlElement(name = "containerConfig")
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
