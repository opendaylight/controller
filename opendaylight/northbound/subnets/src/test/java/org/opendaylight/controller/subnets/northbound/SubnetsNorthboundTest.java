/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subnets.northbound;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.switchmanager.SubnetConfig;

public class SubnetsNorthboundTest {

    @Test
    public void testSubnetConfigs() {
        SubnetConfigs sc1 = new SubnetConfigs(null);
        Assert.assertNull(sc1.getSubnetConfig());

        ArrayList<SubnetConfig> list = new ArrayList<SubnetConfig>();
        SubnetConfig s1 = new SubnetConfig();
        list.add(s1);
        sc1.setSubnetConfig(list);
        Assert.assertTrue(sc1.getSubnetConfig().equals(list));

        sc1.setSubnetConfig(null);
        Assert.assertNull(sc1.getSubnetConfig());
    }

}
