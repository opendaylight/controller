/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved. 
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.topology.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.NodeCreator;
import org.opendaylight.controller.switchmanager.SwitchConfig;
import org.opendaylight.controller.topology.web.Topology.NodeBean;
		
public class TopologyTest {

	@Test
	public void testCreateNodeBean() {
		Topology topology = new Topology();
		Node node = NodeCreator.createOFNode(new Long(3));
		String description = "foo";
		
		NodeBean bean = topology.createNodeBean(description, node);
		
		assertNotNull(bean);
		assertEquals(bean.id, node.toString());
		assertEquals(bean.name, "foo");
		
		bean = topology.createNodeBean(null, node);
		
		assertNotNull(bean);
		assertEquals(bean.id, node.toString());
		assertEquals(bean.name, bean.id);
		
		bean = topology.createNodeBean("   ", node);
		
		assertNotNull(bean);
		assertEquals(bean.id, node.toString());
		assertEquals(bean.name, bean.id);
	}

}
