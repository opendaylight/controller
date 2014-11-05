/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.flowprogrammer.northbound;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.forwardingrulesmanager.FlowConfig;

public class FlowProgrammerNorthboundTest {

    @Test
    public void testFlowConfigs() {
        FlowConfigs fc = new FlowConfigs(null);
        Assert.assertNull(fc.getFlowConfig());

        FlowConfig conf = new FlowConfig();
        FlowConfig conf2 = new FlowConfig();
        ArrayList<FlowConfig> list = new ArrayList<FlowConfig>();

        list.add(conf);
        list.add(conf2);
        FlowConfigs fc2 = new FlowConfigs(list);
        Assert.assertTrue(fc2.getFlowConfig().equals(list));

        fc.setFlowConfig(list);
        Assert.assertTrue(fc.getFlowConfig().equals(fc2.getFlowConfig()));

        fc.setFlowConfig(null);
        Assert.assertNull(fc.getFlowConfig());

    }

}
