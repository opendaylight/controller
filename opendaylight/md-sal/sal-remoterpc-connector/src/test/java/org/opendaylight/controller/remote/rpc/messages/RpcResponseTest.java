/*
 * Copyright (c) 2016 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.messages;

import static org.junit.Assert.assertEquals;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;
import org.opendaylight.controller.remote.rpc.AbstractRpcTest;

/**
 * Unit tests for RpcResponse.
 *
 * @author Thomas Pantelis
 */
public class RpcResponseTest {

    @Test
    public void testSerialization() {
        RpcResponse expected = new RpcResponse(AbstractRpcTest.makeRPCOutput("serialization-test"));

        RpcResponse actual = (RpcResponse) SerializationUtils.clone(expected);

        assertEquals("getResultNormalizedNode", expected.getResultNormalizedNode(), actual.getResultNormalizedNode());
    }
}
