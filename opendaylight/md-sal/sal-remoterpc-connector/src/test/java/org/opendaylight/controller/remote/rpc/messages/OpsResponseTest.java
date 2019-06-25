/*
 * Copyright (c) 2016 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.messages;

import static org.junit.Assert.assertEquals;

import java.util.Optional;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;
import org.opendaylight.controller.remote.rpc.AbstractOpsTest;

/**
 * Unit tests for RpcResponse.
 *
 * @author Thomas Pantelis
 */
public class OpsResponseTest {

    @Test
    public void testSerialization() {
        ActionResponse expectedRpc = new ActionResponse(AbstractOpsTest.makeRPCOutput("serialization-test"));

        ActionResponse expectedAction = new ActionResponse(Optional.of(AbstractOpsTest.makeRPCOutput("serialization-test")));

        ActionResponse actualRpc = (ActionResponse) SerializationUtils.clone(expectedRpc);

        ActionResponse actualAction = (ActionResponse) SerializationUtils.clone(expectedAction);

        assertEquals("getResultNormalizedNode", expectedRpc.getResultNormalizedNode(),
                actualRpc.getResultNormalizedNode());

        assertEquals("getResultNormalizedNode", expectedAction.getResultNormalizedNode(),
                actualAction.getResultNormalizedNode());
    }
}
