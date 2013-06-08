/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.hosttracker.northbound;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)

public class Hosts {
	@XmlElement (name="host")
	Set<HostNodeConnector> hostNodeConnector;
	
	public Hosts() {
	}
	public Hosts (Set<HostNodeConnector> hostNodeConnector) {
		this.hostNodeConnector = hostNodeConnector;
	}
	public Set<HostNodeConnector> getHostNodeConnector() {
		return hostNodeConnector;
	}
	public void setHostNodeConnector(Set<HostNodeConnector> hostNodeConnector) {
		this.hostNodeConnector = hostNodeConnector;
	}
}
