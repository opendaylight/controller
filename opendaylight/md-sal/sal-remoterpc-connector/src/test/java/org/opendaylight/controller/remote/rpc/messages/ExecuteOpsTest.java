/*
 * Copyright (c) 2019 Nordix Foundation.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc.messages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;
import org.opendaylight.controller.remote.rpc.AbstractOpsTest;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class ExecuteOpsTest {

    @Test
    public void testOpsSerialization() {
        final YangInstanceIdentifier action_path = YangInstanceIdentifier.create(
                new YangInstanceIdentifier.NodeIdentifier(AbstractOpsTest.TEST_RPC_ID.getType().getLastComponent()));
        ExecuteOps expected = ExecuteOps.from(AbstractOpsTest.TEST_RPC_ID.getType().getLastComponent(),
                new DOMDataTreeIdentifier(LogicalDatastoreType.OPERATIONAL, action_path),
                AbstractOpsTest.makeRPCInput("serialization-test"), true);

        ExecuteOps actual = (ExecuteOps) SerializationUtils.clone(expected);

        assertEquals("getName", expected.getName(), actual.getName());
        assertEquals("getInputNormalizedNode", expected.getInputNormalizedNode(), actual.getInputNormalizedNode());
        assertEquals("getPath", expected.getPath(), actual.getPath());
        assertTrue(expected.getIsRpcMessage() ==  actual.getIsRpcMessage());
    }
}
