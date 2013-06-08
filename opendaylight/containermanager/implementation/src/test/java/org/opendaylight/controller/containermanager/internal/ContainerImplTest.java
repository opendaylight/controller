
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.containermanager.internal;

import static org.junit.Assert.*;

import java.util.Hashtable;

import org.apache.felix.dm.impl.ComponentImpl;
import org.junit.Test;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.NodeCreator;

public class ContainerImplTest {

	@Test
	public void test() {
		
		ContainerImpl container1 = new ContainerImpl();
				
		//Create Component for init
		ComponentImpl component1 = new ComponentImpl(null, null, null);
		component1.setInterface("serviceTestName", null);

		//container1 does not have name yet
		container1.init(component1);
		assertNull(container1.getName());
		
		//Sets container1 name to TestName
		Hashtable<String, String> properties = new Hashtable<String, String>();
		properties.put("dummyKey", "dummyValue");
		properties.put("containerName", "TestName");
		component1.setInterface("serviceTestName", properties);

		container1.init(component1);
		assertEquals("TestName", container1.getName());
		
		//getContainerFlows always returns null for now
		assertNull(container1.getContainerFlows());
		
		//getTag always returns 0 for now
		Node n = NodeCreator.createOFNode(1L);
		assertEquals(0, container1.getTag(n));
		
		//getNodeConnectors always returns null for now
		assertNull(container1.getNodeConnectors());
		
		//getNodes always returns null for now
		assertNull(container1.getNodes());
		
	}

}
