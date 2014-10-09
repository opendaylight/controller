/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connect.netconf.sal;

import com.google.common.util.concurrent.ListenableFuture;
import java.net.URI;
import java.util.concurrent.Executor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.controller.sal.connect.api.MessageTransformer;
import org.opendaylight.controller.sal.connect.api.RemoteDeviceCommunicator;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

public class NetconfDeviceRpcTest {

    @Mock
    private RemoteDeviceCommunicator deviceCommunicator;
    @Mock
    private MessageTransformer transformer;
    @Mock
    private CompositeNode compositeNode;
    @Mock
    private ListenableFuture listenableFuture;

    private QName qname;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        NetconfMessage message = new NetconfMessage(XmlUtil.newDocument());
        qname = new QName(new URI("scheme","ssp","fragment"), "lname");
        doReturn("").when(compositeNode).toString();
        doReturn(message).when(transformer).toRpcRequest(any(QName.class), any(CompositeNode.class));
        doReturn(listenableFuture).when(deviceCommunicator).sendRequest(anyObject(), any(QName.class));
        doNothing().when(listenableFuture).addListener(any(Runnable.class), any(Executor.class));
    }

    @Test
    public void testNetconfDeviceRpc() throws Exception {
        NetconfDeviceRpc deviceRpc = new NetconfDeviceRpc(deviceCommunicator, transformer);
        deviceRpc.invokeRpc(qname, compositeNode);
    }
}
