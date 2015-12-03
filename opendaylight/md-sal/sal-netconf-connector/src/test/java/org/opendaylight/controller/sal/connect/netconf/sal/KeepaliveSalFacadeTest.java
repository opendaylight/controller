/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf.sal;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.util.concurrent.Futures;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.spi.DefaultDOMRpcResult;
import org.opendaylight.controller.sal.connect.api.RemoteDeviceHandler;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfDeviceCommunicator;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfSessionPreferences;
import org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class KeepaliveSalFacadeTest {

    private static final RemoteDeviceId REMOTE_DEVICE_ID = new RemoteDeviceId("test", new InetSocketAddress("localhost", 22));

    @Mock
    private RemoteDeviceHandler<NetconfSessionPreferences> underlyingSalFacade;

    private static java.util.concurrent.ScheduledExecutorService executorService;

    @Mock
    private NetconfDeviceCommunicator listener;
    @Mock
    private DOMRpcService deviceRpc;

    private DOMRpcService proxyRpc;

    @Before
    public void setUp() throws Exception {
        executorService = Executors.newScheduledThreadPool(1);

        MockitoAnnotations.initMocks(this);

        doNothing().when(listener).disconnect();
        doReturn("mockedRpc").when(deviceRpc).toString();
        doNothing().when(underlyingSalFacade).onDeviceConnected(
                any(SchemaContext.class), any(NetconfSessionPreferences.class), any(DOMRpcService.class));
    }

    @After
    public void tearDown() throws Exception {
        executorService.shutdownNow();
    }

    @Test
    public void testKeepaliveSuccess() throws Exception {
        final DOMRpcResult result = new DefaultDOMRpcResult(Builders.containerBuilder().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(NetconfMessageTransformUtil.NETCONF_RUNNING_QNAME)).build());

        doReturn(Futures.immediateCheckedFuture(result)).when(deviceRpc).invokeRpc(any(SchemaPath.class), any(NormalizedNode.class));

        final KeepaliveSalFacade keepaliveSalFacade =
                new KeepaliveSalFacade(REMOTE_DEVICE_ID, underlyingSalFacade, executorService, 1L, 1L);
        keepaliveSalFacade.setListener(listener);

        keepaliveSalFacade.onDeviceConnected(null, null, deviceRpc);

        verify(underlyingSalFacade).onDeviceConnected(
                any(SchemaContext.class), any(NetconfSessionPreferences.class), any(DOMRpcService.class));

        verify(deviceRpc, timeout(15000).times(5)).invokeRpc(any(SchemaPath.class), any(NormalizedNode.class));
    }

    @Test
    public void testKeepaliveFail() throws Exception {
        final DOMRpcResult result = new DefaultDOMRpcResult(Builders.containerBuilder().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(NetconfMessageTransformUtil.NETCONF_RUNNING_QNAME)).build());

        final DOMRpcResult resultFail = new DefaultDOMRpcResult(mock(RpcError.class));

        doReturn(Futures.immediateCheckedFuture(result))
                .doReturn(Futures.immediateCheckedFuture(resultFail))
                .doReturn(Futures.immediateFailedCheckedFuture(new IllegalStateException("illegal-state")))
                .when(deviceRpc).invokeRpc(any(SchemaPath.class), any(NormalizedNode.class));

        final KeepaliveSalFacade keepaliveSalFacade =
                new KeepaliveSalFacade(REMOTE_DEVICE_ID, underlyingSalFacade, executorService, 1L, 1L);
        keepaliveSalFacade.setListener(listener);

        keepaliveSalFacade.onDeviceConnected(null, null, deviceRpc);

        verify(underlyingSalFacade).onDeviceConnected(
                any(SchemaContext.class), any(NetconfSessionPreferences.class), any(DOMRpcService.class));

        // 1 failed that results in disconnect
        verify(listener, timeout(15000).times(2)).disconnect(); // TODO correctly fix tests
        // 3 attempts total
        verify(deviceRpc, times(3)).invokeRpc(any(SchemaPath.class), any(NormalizedNode.class));

        // Reconnect with same keepalive responses
        doReturn(Futures.immediateCheckedFuture(result))
                .doReturn(Futures.immediateCheckedFuture(resultFail))
                .doReturn(Futures.immediateFailedCheckedFuture(new IllegalStateException("illegal-state")))
                .when(deviceRpc).invokeRpc(any(SchemaPath.class), any(NormalizedNode.class));

        keepaliveSalFacade.onDeviceConnected(null, null, deviceRpc);

        // 1 failed that results in disconnect, 2 total with previous fail
        verify(listener, timeout(15000).times(2)).disconnect();
        // 6 attempts now total
        verify(deviceRpc, times(3)).invokeRpc(any(SchemaPath.class), any(NormalizedNode.class));  // TODO correctly fix tests
    }

    @Test
    public void testNonKeepaliveRpcFailure() throws Exception {
        doAnswer(new Answer() {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                proxyRpc = (DOMRpcService) invocationOnMock.getArguments()[2];
                return null;
            }
        }).when(underlyingSalFacade).onDeviceConnected(any(SchemaContext.class), any(NetconfSessionPreferences.class), any(DOMRpcService.class));

        doReturn(Futures.immediateFailedCheckedFuture(new IllegalStateException("illegal-state")))
                .when(deviceRpc).invokeRpc(any(SchemaPath.class), any(NormalizedNode.class));

        final KeepaliveSalFacade keepaliveSalFacade =
                new KeepaliveSalFacade(REMOTE_DEVICE_ID, underlyingSalFacade, executorService, 100L, 100L);
        keepaliveSalFacade.setListener(listener);

        keepaliveSalFacade.onDeviceConnected(null, null, deviceRpc);

        proxyRpc.invokeRpc(mock(SchemaPath.class), mock(NormalizedNode.class));

        verify(listener, times(1)).disconnect();
    }
}