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

import org.opendaylight.controller.containermanager.ContainerFlowConfig;


@XmlRootElement(name = "flow-spec-configs")
@XmlAccessorType(XmlAccessType.NONE)
public class FlowSpecConfigs {
    @XmlElement(name = "flow-spec-config")
    List<ContainerFlowConfig> containerFlowConfig;

    // To satisfy JAXB
    @SuppressWarnings("unused")
    private FlowSpecConfigs() {

    }

    public FlowSpecConfigs(List<ContainerFlowConfig> containerFlowConfig) {
        this.containerFlowConfig = containerFlowConfig;
    }

    public List<ContainerFlowConfig> getContainerFlowConfig() {
        return containerFlowConfig;
    }

    public void setContainerFlowConfig(List<ContainerFlowConfig> containerFlowConfig) {
        this.containerFlowConfig = containerFlowConfig;
    }
}
