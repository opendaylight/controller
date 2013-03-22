/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.statistics.northbound;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)

public class AllFlowStatistics {
	@XmlElement
	List<FlowStatistics> flowStatistics;
	//To satisfy JAXB
	private AllFlowStatistics() {
	}
	
	public AllFlowStatistics(List<FlowStatistics> flowStatistics) {
		this.flowStatistics = flowStatistics;
	}

	public List<FlowStatistics> getFlowStatistics() {
		return flowStatistics;
	}

	public void setFlowStatistics(List<FlowStatistics> flowStatistics) {
		this.flowStatistics = flowStatistics;
	}

}
