
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.containermanager.internal;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Test;
import org.opendaylight.controller.sal.utils.GlobalConstants;

public class ContainerManagerTest {

	@Test
	public void test() {
		ContainerManager cm = new ContainerManager();
		
		cm.init();
		
		ArrayList<String> names = (ArrayList<String>) cm.getContainerNames();
		assertEquals(1, names.size());
		assertEquals(GlobalConstants.DEFAULT.toString(), names.get(0));
		
		assertFalse(cm.hasNonDefaultContainer());
		assertNull(cm.saveContainerConfig());
		
		cm.destroy();

	}

}
