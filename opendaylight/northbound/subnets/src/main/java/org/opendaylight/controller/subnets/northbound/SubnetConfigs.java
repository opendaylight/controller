/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.subnets.northbound;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.switchmanager.SubnetConfig;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)

public class SubnetConfigs {
	@XmlElement
	List<SubnetConfig> subnetConfig;
	//To satisfy JAXB
	private SubnetConfigs() {
		
	}
	
	public SubnetConfigs(List<SubnetConfig> subnetConfig) {
		this.subnetConfig = subnetConfig;
	}

	public List<SubnetConfig> getSubnetConfig() {
		return subnetConfig;
	}

	public void setSubnetConfig(List<SubnetConfig> subnetConfig) {
		this.subnetConfig = subnetConfig;
	}
}
