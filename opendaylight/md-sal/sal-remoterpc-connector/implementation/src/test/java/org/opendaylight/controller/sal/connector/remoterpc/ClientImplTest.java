/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connector.remoterpc;

import com.google.common.base.Optional;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.controller.sal.connector.api.RpcRouter;
import org.opendaylight.controller.sal.connector.remoterpc.api.RouteChangeListener;
import org.opendaylight.controller.sal.connector.remoterpc.api.RoutingTable;
import org.opendaylight.controller.sal.connector.remoterpc.api.RoutingTableException;
import org.opendaylight.controller.sal.connector.remoterpc.api.SystemException;
import org.opendaylight.controller.sal.connector.remoterpc.dto.Message;
import org.opendaylight.controller.sal.connector.remoterpc.utils.MessagingUtil;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

import java.io.IOException;
import java.util.*;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class ClientImplTest {
  RoutingTableProvider routingTableProvider;
  ClientImpl client;
  ClientRequestHandler mockHandler;

  @Before
  public void setUp() throws Exception {

    //mock routing table
    routingTableProvider = mock(RoutingTableProvider.class);
    RoutingTable<RpcRouter.RouteIdentifier, String> mockRoutingTable = new MockRoutingTable<String, String>();
    Optional<RoutingTable<RpcRouter.RouteIdentifier, String>> optionalRoutingTable = Optional.fromNullable(mockRoutingTable);
    when(routingTableProvider.getRoutingTable()).thenReturn(optionalRoutingTable);

    //mock ClientRequestHandler
    mockHandler = mock(ClientRequestHandler.class);

    client = new ClientImpl(mockHandler);
    client.setRoutingTableProvider(routingTableProvider);

  }

  @After
  public void tearDown() throws Exception {

  }

  @Test
  public void getRoutingTableProvider_Call_ShouldReturnMockProvider() throws Exception {
    Assert.assertEquals(routingTableProvider, client.getRoutingTableProvider());

  }

  @Test
  public void testStart() throws Exception {

  }

  @Test
  public void testStop() throws Exception {

  }

  @Test
  public void testClose() throws Exception {

  }

  //@Test
  public void invokeRpc_NormalCall_ShouldReturnSuccess() throws Exception {

    when(mockHandler.handle(any(Message.class))).
            thenReturn(MessagingUtil.createEmptyMessage());

    RpcResult<CompositeNode> result = client.invokeRpc(null, null);

    Assert.assertTrue(result.isSuccessful());
    Assert.assertTrue(result.getErrors().isEmpty());
    Assert.assertNull(result.getResult());
  }

  //@Test
  public void invokeRpc_HandlerThrowsException_ShouldReturnError() throws Exception {

    when(mockHandler.handle(any(Message.class))).
            thenThrow(new IOException());

    RpcResult<CompositeNode> result = client.invokeRpc(null, null);

    Assert.assertFalse(result.isSuccessful());
    Assert.assertFalse(result.getErrors().isEmpty());
    Assert.assertNull(result.getResult());
  }

}
