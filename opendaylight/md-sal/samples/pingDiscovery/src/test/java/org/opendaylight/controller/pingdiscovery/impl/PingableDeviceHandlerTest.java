/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * Tests the PingableDeviceHandler class
 * 
 * @author Devin Avery
 * @author Greg Hall
 */
package org.opendaylight.controller.pingdiscovery.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sample.pingdiscovery.PingService;
import org.opendaylight.controller.sample.pingdiscovery.impl.PingableDeviceHandler;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.device.ip.rev140515.Node1;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.device.ip.rev140515.Node1Builder;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.data.rev140515.Icmpdata;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.data.rev140515.IcmpdataBuilder;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.data.rev140515.SendPingNowOutput;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.data.rev140515.SendPingNowOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;

import com.google.common.util.concurrent.ListenableFuture;

public class PingableDeviceHandlerTest {

    private final String nodeIdReachableStr = new String("our:test:ReachablenodeId");
    private final String nodeIdNotReachableStr = new String("our:test:NotReachablenodeId");
    private PingableDeviceHandler pingDevHandler;

    @Mock private DataBrokerService mockDataBroker;
    @Mock private BindingIndependentMappingService mockMIMapSvs;
    @Mock private PingService mockPingService;

    @Mock private CompositeNode readResultSuccess;
    @Mock private CompositeNode readResultFailed;

    @Mock CompositeNode inputNode ;

    private Node opDataNodeReachable;
    private Node opDataNodeNotReachable;

    private InstanceIdentifier<Node> nodePathReachable;
    private InstanceIdentifier<Node> nodePathNotReachable;

    private org.opendaylight.yangtools.yang.data.api.InstanceIdentifier readPath;

    private Icmpdata icmpOutputSuccess;
    private Icmpdata icmpOutputFailed;

    public PingableDeviceHandlerTest() {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Private method to setup the input objects for testing readOperationalData
     * 
     * @return instance id of profile
     */
    private void setupReadOperationalData() {

        NodeId nodeId = new NodeId("nodeId");
        Node1 node1 = new Node1Builder().setIpaddress(
                new IpAddress(new Ipv4Address("192.168.1.28"))).build();

        opDataNodeReachable = new NodeBuilder().setId(nodeId).setKey(new NodeKey(nodeId))
                .addAugmentation(Node1.class, node1).build();

        node1 = new Node1Builder().setIpaddress(
                new IpAddress(new Ipv4Address("192.168.1.29"))).build();

        opDataNodeNotReachable = new NodeBuilder().setId(nodeId)
                .setKey(new NodeKey(nodeId)).addAugmentation(Node1.class, node1).build();

        nodePathReachable = InstanceIdentifier
                .<Nodes> builder(Nodes.class)
                .<Node, NodeKey> child(Node.class,
                        new NodeKey(new NodeId(nodeIdReachableStr))).toInstance();

        nodePathNotReachable = InstanceIdentifier
                .<Nodes> builder(Nodes.class)
                .<Node, NodeKey> child(Node.class,
                        new NodeKey(new NodeId(nodeIdNotReachableStr))).toInstance();

        QName qname = QName.create("http://opendaylight.org/samples/icmp/data",
                "2014-05-21", "icmpdata");

        NodeIdentifier pathArg = new NodeIdentifier(qname);
        List<PathArgument> list = new ArrayList<PathArgument>();
        list.add(pathArg);
        readPath = new org.opendaylight.yangtools.yang.data.api.InstanceIdentifier(list);

        icmpOutputSuccess = new IcmpdataBuilder().setIsAvailable(true).build();
        icmpOutputFailed = new IcmpdataBuilder().setIsAvailable(false).build();

    }

    @Before
    public void testInit() {
        System.out.println("Test Init - " + this);

    }

    /**
     * Test the happy path through readOperationalData where the device is reachable.
     * 
     */
    @Test
    public void testReadOperationalData_success() {

        setupReadOperationalData();

        when(mockDataBroker.readOperationalData(nodePathReachable)).thenReturn(
                opDataNodeReachable);

        when(mockPingService.ping("192.168.1.28")).thenReturn(105D);

        when(mockMIMapSvs.toDataDom(eq(icmpOutputSuccess))).thenReturn(readResultSuccess);

        setupHandler(nodeIdReachableStr);

        CompositeNode result = pingDevHandler.readOperationalData(readPath);
        assertNotNull(result);
        assertEquals(readResultSuccess, result);
    }

    /**
     * Test the happy path through readOperationalData where the device is not reachable.
     * 
     */
    @Test
    public void testReadOperationalData_fail() {

        setupReadOperationalData();

        when(mockDataBroker.readOperationalData(nodePathNotReachable)).thenReturn(
                opDataNodeNotReachable);
        when(mockPingService.ping("192.168.1.29")).thenReturn(PingService.NOT_FOUND);

        when(mockMIMapSvs.toDataDom(eq(icmpOutputFailed))).thenReturn(readResultFailed);

        setupHandler(nodeIdNotReachableStr);

        CompositeNode result = pingDevHandler.readOperationalData(readPath);
        assertNotNull(result);
        assertEquals(readResultFailed, result);
    }

    /**
     * 
     */
    private void setupHandler(String nodeIdStr) {
        pingDevHandler = new PingableDeviceHandler(nodeIdStr);
        // initialize the manager impl
        pingDevHandler.setDataBrokerService(mockDataBroker);
        pingDevHandler.setMappingService(mockMIMapSvs);
        pingDevHandler.setPingService(mockPingService);
    }

    @Test
    public void testInvokeRpc_success() throws InterruptedException, ExecutionException {
        setupReadOperationalData() ;

        QName rpcQName = QName.create("http://opendaylight.org/samples/icmp/data",
                "2014-05-21", "rtt");
        setupHandler(nodeIdReachableStr) ;

        when(mockPingService.ping("192.168.1.28")).thenReturn(105D);
        when(mockDataBroker.readOperationalData(nodePathReachable)).thenReturn(
                opDataNodeReachable);

        SendPingNowOutput output = new SendPingNowOutputBuilder().setRtt(
                Math.round(105D)).build();

        when(mockMIMapSvs.toDataDom(eq(output))).thenReturn(readResultSuccess);

        ListenableFuture<RpcResult<CompositeNode>> resultFuture  =
                pingDevHandler.invokeRpc( rpcQName, inputNode );

        assertNotNull(resultFuture) ;
        assertEquals( readResultSuccess, resultFuture.get().getResult() ) ;

    }

    @Test
    public void testInvokeRpc_failure() throws InterruptedException, ExecutionException {
        setupReadOperationalData() ;

        QName rpcQName = QName.create("http://opendaylight.org/samples/icmp/data",
                "2014-05-21", "rtt");
        setupHandler(nodeIdNotReachableStr) ;

        when(mockPingService.ping("192.168.1.29")).thenReturn(PingService.NOT_FOUND);
        when(mockDataBroker.readOperationalData(nodePathNotReachable)).thenReturn(
                opDataNodeNotReachable);

        SendPingNowOutput output = new SendPingNowOutputBuilder().build();
        when(mockMIMapSvs.toDataDom(eq(output))).thenReturn(readResultFailed);

        ListenableFuture<RpcResult<CompositeNode>> resultFuture  =
                pingDevHandler.invokeRpc( rpcQName, inputNode );

        assertNotNull(resultFuture) ;
        assertEquals( readResultFailed, resultFuture.get().getResult() ) ;
    }

}
