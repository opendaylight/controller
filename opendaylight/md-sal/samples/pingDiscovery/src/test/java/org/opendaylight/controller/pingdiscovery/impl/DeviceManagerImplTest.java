/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * Tests the IcmpDiscoveryServiceImpl class
 * 
 * @author Devin Avery
 * @author Greg Hall
 */
package org.opendaylight.controller.pingdiscovery.impl;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.controller.sample.pingdiscovery.DeviceMountHandler;
import org.opendaylight.controller.sample.pingdiscovery.impl.DeviceManagerImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class DeviceManagerImplTest {

    @Mock private DataProviderService mockDataBrokerService;
    @Mock private DeviceMountHandler mockRpcProvider;
    @Mock DataModificationTransaction mockDataModTxn ;
    private DeviceManagerImpl deviceManagerImpl = new DeviceManagerImpl() ;
    private RpcResult<TransactionStatus> resultCompNode ;
    private ListenableFuture<RpcResult<TransactionStatus>> futureCompNode;

    private String nodeIdStr = new String("10.10.10.10") ;
    private InstanceIdentifier<Node> nodePath ;
    public DeviceManagerImplTest() {
        MockitoAnnotations.initMocks(this);
    }

    @Before
    public void testInit() {
        deviceManagerImpl.setDataBrokerService(mockDataBrokerService);
        deviceManagerImpl.setRpcProvider(mockRpcProvider);
        resultCompNode = Rpcs.getRpcResult(true, TransactionStatus.SUBMITED,
                Collections.<RpcError> emptySet());
        futureCompNode = Futures.immediateFuture(resultCompNode);

        nodePath = InstanceIdentifier.<Nodes>builder(Nodes.class)
                .<Node, NodeKey>child(Node.class, new NodeKey(new NodeId( nodeIdStr ))).toInstance();
    }
    @Test
    public void testCreateDevice() {
        when( mockDataBrokerService.readConfigurationData(nodePath)).thenReturn(null) ;
        when( mockDataBrokerService.beginTransaction()).thenReturn(mockDataModTxn) ;
        when( mockDataModTxn.commit()).thenReturn(futureCompNode) ;
        //        when( mockRpcProvider.mountIcmpDataNode(nodeIdStr)).thenReturn(null) ;

        boolean result = deviceManagerImpl.createDevice(nodeIdStr) ;
        assertTrue( result ) ;
    }
}
