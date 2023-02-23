/*
 * Copyright (c) 2019 Nordix Foundation.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.messages;

import static org.junit.Assert.assertEquals;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Test;
import org.opendaylight.controller.remote.rpc.AbstractOpsTest;

public class ExecuteOpsTest {

    @Test
    public void testOpsSerialization() {
        ExecuteRpc expected = ExecuteRpc.from(AbstractOpsTest.TEST_RPC_ID,
                AbstractOpsTest.makeRPCInput("serialization-test"));

        ExecuteRpc actual = SerializationUtils.clone(expected);

        assertEquals("getName", expected.getType(), actual.getType());
        assertEquals("getInputNormalizedNode", expected.getInput(), actual.getInput());
        assertEquals("getPath", expected.getType(), actual.getType());
    }
}
