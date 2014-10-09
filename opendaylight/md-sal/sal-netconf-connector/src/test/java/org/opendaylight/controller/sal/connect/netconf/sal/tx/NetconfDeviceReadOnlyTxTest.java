/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connect.netconf.sal.tx;

import com.google.common.util.concurrent.Futures;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizer;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

public class NetconfDeviceReadOnlyTxTest {
    private final RemoteDeviceId id = new RemoteDeviceId("test-mount");

    @Mock
    private RpcImplementation rpc;
    @Mock
    private DataNormalizer normalizer;

    private YangInstanceIdentifier yangIId;
    private NetconfDeviceReadOnlyTx deviceReadOnlyTx;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        doReturn(Futures.<RpcResult<CompositeNode>>immediateFailedFuture(new IllegalStateException("Failed tx")))
                .doReturn(Futures.immediateFuture(RpcResultBuilder.<CompositeNode>success().build()))
                .when(rpc).invokeRpc(any(QName.class), any(CompositeNode.class));

        yangIId = YangInstanceIdentifier.builder().node(QName.create("namespace", "2012-12-12", "name")).build();
        doReturn(yangIId).when(normalizer).toLegacy(yangIId);
        deviceReadOnlyTx = new NetconfDeviceReadOnlyTx(rpc, normalizer, id);
    }

    @Test
    public void testRead() throws Exception {
        assertEquals(deviceReadOnlyTx, deviceReadOnlyTx.getIdentifier());
        deviceReadOnlyTx.exists(LogicalDatastoreType.OPERATIONAL, yangIId);
    }

}
