/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connect.netconf.sal.connect.util;

import org.junit.Test;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RemoteDeviceIdTest {
    @Test
    public void testRemoteDeviceId() throws Exception {
        ModuleIdentifier id = new ModuleIdentifier("fname", "iname");
        NodeKey key = new NodeKey(new NodeId(id.getInstanceName()));

        RemoteDeviceId remoteId = new RemoteDeviceId(id);

        assertEquals(key, remoteId.getBindingKey());
        assertNotNull(remoteId.getPath());
        assertNotNull(remoteId.getBindingPath());
        assertTrue(remoteId.equals(remoteId));
        assertFalse(remoteId.equals(null));
        assertTrue(new RemoteDeviceId(id).equals(remoteId));
        assertEquals(remoteId.hashCode(), new RemoteDeviceId(id).hashCode());
    }
}
