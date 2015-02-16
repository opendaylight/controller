/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connect.netconf.sal.tx;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.net.InetSocketAddress;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationException;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizer;
import org.opendaylight.controller.sal.connect.netconf.util.NetconfBaseOps;
import org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class ReadOnlyTxTest {

    private static final YangInstanceIdentifier path = YangInstanceIdentifier.create();

    @Mock
    private RpcImplementation rpc;
    @Mock
    private DataNormalizer normalizer;
    @Mock
    private CompositeNode mockedNode;

    @Before
    public void setUp() throws DataNormalizationException {
        MockitoAnnotations.initMocks(this);
        doReturn(path).when(normalizer).toLegacy(any(YangInstanceIdentifier.class));
        doReturn(com.google.common.util.concurrent.Futures.immediateFuture(RpcResultBuilder.success(mockedNode).build())).when(rpc).invokeRpc(any(org.opendaylight.yangtools.yang.common.QName.class), any(CompositeNode.class));
        doReturn("node").when(mockedNode).toString();
    }

    @Test
    public void testRead() throws Exception {
        final NetconfBaseOps netconfOps = new NetconfBaseOps(rpc);

        final ReadOnlyTx readOnlyTx = new ReadOnlyTx(netconfOps, normalizer, new RemoteDeviceId("a", new InetSocketAddress("localhost", 196)));

        readOnlyTx.read(LogicalDatastoreType.CONFIGURATION, YangInstanceIdentifier.create());
        verify(rpc).invokeRpc(Mockito.same(NetconfMessageTransformUtil.NETCONF_GET_CONFIG_QNAME), any(CompositeNode.class));
        readOnlyTx.read(LogicalDatastoreType.OPERATIONAL, path);
        verify(rpc).invokeRpc(Mockito.same(NetconfMessageTransformUtil.NETCONF_GET_QNAME), any(CompositeNode.class));
    }
}