
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.reader;

import org.junit.Test;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.reader.NodeConnectorStatistics;
import org.opendaylight.controller.sal.utils.NodeConnectorCreator;
import org.opendaylight.controller.sal.utils.NodeCreator;
import org.junit.Assert;

public class NodeConnectorStatisticsTest {

	@Test
	public void testNodeConnectorStatisticsMethods() {
		NodeConnector nc = NodeConnectorCreator.createNodeConnector((short)20, NodeCreator.createOFNode((long)20));
		NodeConnectorStatistics ncStats = new NodeConnectorStatistics();
		ncStats.setNodeConnector(nc);
		ncStats.setReceiveByteCount(800);
		ncStats.setReceiveCRCErrorCount(10);
		ncStats.setReceiveDropCount(5);
		ncStats.setReceiveErrorCount(20);
		ncStats.setReceiveFrameErrorCount(25);
		ncStats.setReceiveOverRunErrorCount(30);
		ncStats.setReceivePacketCount(100);
		ncStats.setTransmitByteCount(400);
		ncStats.setTransmitDropCount(15);
		ncStats.setTransmitErrorCount(18);
		ncStats.setTransmitPacketCount(50);
		ncStats.setCollisionCount(2);
		
		Assert.assertTrue(ncStats.getCollisionCount() == 2);
		Assert.assertTrue(ncStats.getTransmitPacketCount() == 50);
		Assert.assertTrue(ncStats.getTransmitErrorCount() == 18);
		Assert.assertTrue(ncStats.getTransmitDropCount() == 15);
		Assert.assertTrue(ncStats.getReceivePacketCount() == 100);
		Assert.assertTrue(ncStats.getReceiveOverRunErrorCount() == 30);
		Assert.assertTrue(ncStats.getReceiveFrameErrorCount() == 25);
		Assert.assertTrue(ncStats.getReceiveDropCount() == 5);
		Assert.assertTrue(ncStats.getReceiveCRCErrorCount() == 10);
		Assert.assertTrue(ncStats.getReceiveByteCount() == 800);
		Assert.assertTrue(ncStats.getNodeConnector().equals(nc));
	}
}

