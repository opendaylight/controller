/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.forwarding.staticrouting.northbound;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.forwardingrulesmanager.FlowConfig;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)

public class StaticRoutes {
	@XmlElement
	List<StaticRoute> staticRoute;
	//To satisfy JAXB
	private StaticRoutes() {
		
	}
	
	public StaticRoutes(List<StaticRoute> staticRoute) {
		this.staticRoute = staticRoute;
	}

	public List<StaticRoute> getFlowConfig() {
		return staticRoute;
	}

	public void setFlowConfig(List<StaticRoute> staticRoute) {
		this.staticRoute = staticRoute;
	}
}
