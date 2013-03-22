
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.reader;

import java.util.ArrayList;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.action.Output;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.controller.sal.reader.FlowOnNode;
import org.opendaylight.controller.sal.utils.EtherTypes;
import org.opendaylight.controller.sal.utils.NodeConnectorCreator;
import org.opendaylight.controller.sal.utils.NodeCreator;
	
public class FlowOnNodeTest {

		@Test
		public void testFlowOnNodeMethods () {
		Match match = new Match();
		NodeConnector inNC = NodeConnectorCreator.createNodeConnector((short)10, NodeCreator.createOFNode((long)10));
		NodeConnector outNC = NodeConnectorCreator.createNodeConnector((short)20, NodeCreator.createOFNode((long)20));
			
		match.setField(MatchType.DL_TYPE, EtherTypes.IPv4.shortValue());
		match.setField(MatchType.IN_PORT, inNC);
			
		Output output = new Output(outNC);
		ArrayList<Action> action = new ArrayList<Action>();
		action.add(output);
			
		Flow flow = new Flow (match, action);
		
		FlowOnNode flowOnNode = new FlowOnNode (flow);
	
		Assert.assertTrue(flowOnNode.getFlow().equals(flow));
		
		flowOnNode.setPacketCount((long)100);
		flowOnNode.setByteCount((long)800);
		flowOnNode.setTableId((byte)0x55);
		flowOnNode.setDurationNanoseconds(40);
		flowOnNode.setDurationSeconds(45);
			
		Assert.assertTrue(flowOnNode.getPacketCount() == 100);
		Assert.assertTrue(flowOnNode.getByteCount() == 800);
		Assert.assertTrue(flowOnNode.getDurationNanoseconds() == 40);
		Assert.assertTrue(flowOnNode.getDurationSeconds() == 45);
		Assert.assertTrue(flowOnNode.getTableId() == (byte)0x55);		
	}
}