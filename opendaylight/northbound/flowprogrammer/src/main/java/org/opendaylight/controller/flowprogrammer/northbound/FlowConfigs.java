/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.flowprogrammer.northbound;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.forwardingrulesmanager.FlowConfig;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)

public class FlowConfigs {
	@XmlElement
	List<FlowConfig> flowConfig;
	//To satisfy JAXB
	private FlowConfigs() {
		
	}
	
	public FlowConfigs(List<FlowConfig> flowConfig) {
		this.flowConfig = flowConfig;
	}

	public List<FlowConfig> getFlowConfig() {
		return flowConfig;
	}

	public void setFlowConfig(List<FlowConfig> flowConfig) {
		this.flowConfig = flowConfig;
	}
}
