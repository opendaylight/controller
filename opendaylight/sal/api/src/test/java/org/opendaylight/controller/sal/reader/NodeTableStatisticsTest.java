/*
 * Copyright (c) 2013 Big Switch Networks, Inc.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.reader;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.sal.core.NodeTable;
import org.opendaylight.controller.sal.utils.NodeCreator;
import org.opendaylight.controller.sal.utils.NodeTableCreator;

public class NodeTableStatisticsTest {

    @Test
    public void testNodeTableStatisticsMethods() {
        NodeTable nt = NodeTableCreator.createNodeTable(Byte.valueOf("2") , NodeCreator.createOFNode((long)20));
        NodeTableStatistics ntStats = new NodeTableStatistics();

        ntStats.setNodeTable(nt);
        ntStats.setActiveCount(100);
        ntStats.setLookupCount(200);
        ntStats.setMatchedCount(500);
        ntStats.setName("Test");

        Assert.assertTrue(ntStats.getNodeTable().equals(nt));
        Assert.assertTrue(ntStats.getActiveCount() == 100);
        Assert.assertTrue(ntStats.getLookupCount() == 200);
        Assert.assertTrue(ntStats.getMatchedCount() == 500);
        Assert.assertTrue(ntStats.getName().equals("Test"));
    }
}
