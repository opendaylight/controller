/*
 * Copyright (c) ${year} Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Futures;
import java.util.Map;
import java.util.concurrent.Future;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.core.api.Broker.ConsumerSession;
import org.opendaylight.controller.sal.core.api.data.DataBrokerService;
import org.opendaylight.controller.sal.core.api.data.DataChangeListener;
import org.opendaylight.controller.sal.core.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.core.api.mount.MountInstance;
import org.opendaylight.controller.sal.rest.impl.XmlToCompositeNodeProvider;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.controller.sal.restconf.impl.RestconfError;
import org.opendaylight.controller.sal.streams.listeners.ListenerAdapter;
import org.opendaylight.controller.sal.streams.listeners.Notificator;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;

/**
 * Unit tests for BrokerFacade.
 *
 * @author Thomas Pantelis
 */
public class BrokerFacadeTest {

    @Mock
    DataBrokerService dataBroker;

    @Mock
    DataModificationTransaction mockTransaction;

    @Mock
    ConsumerSession mockConsumerSession;

    @Mock
    MountInstance mockMountInstance;

    BrokerFacade brokerFacade = BrokerFacade.getInstance();

    CompositeNode dataNode = TestUtils.readInputToCnSn("/parts/ietf-interfaces_interfaces.xml",
            XmlToCompositeNodeProvider.INSTANCE);

    QName qname = QName.create("node");

    InstanceIdentifier instanceID = InstanceIdentifier.builder().node(qname).toInstance();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        brokerFacade.setDataService(dataBroker);
        brokerFacade.setContext(mockConsumerSession);
    }

    @Test
    public void testReadConfigurationData() {
        when(dataBroker.readConfigurationData(instanceID)).thenReturn(dataNode);

        CompositeNode actualNode = brokerFacade.readConfigurationData(instanceID);

        assertSame("readConfigurationData", dataNode, actualNode);
    }

    @Test
    public void testReadConfigurationDataBehindMountPoint() {
        when(mockMountInstance.readConfigurationData(instanceID)).thenReturn(dataNode);

        CompositeNode actualNode = brokerFacade.readConfigurationDataBehindMountPoint(mockMountInstance, instanceID);

        assertSame("readConfigurationDataBehindMountPoint", dataNode, actualNode);
    }

    @Test
    public void testReadOperationalData() {
        when(dataBroker.readOperationalData(instanceID)).thenReturn(dataNode);

        CompositeNode actualNode = brokerFacade.readOperationalData(instanceID);

        assertSame("readOperationalData", dataNode, actualNode);
    }

    @Test
    public void testReadOperationalDataBehindMountPoint() {
        when(mockMountInstance.readOperationalData(instanceID)).thenReturn(dataNode);

        CompositeNode actualNode = brokerFacade.readOperationalDataBehindMountPoint(mockMountInstance, instanceID);

        assertSame("readOperationalDataBehindMountPoint", dataNode, actualNode);
    }

    @Test(expected = RestconfDocumentedException.class)
    public void testReadOperationalDataWithNoDataBroker() {
        brokerFacade.setDataService(null);

        brokerFacade.readOperationalData(instanceID);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInvokeRpc() throws Exception {
        RpcResult<CompositeNode> expResult = mock(RpcResult.class);
        Future<RpcResult<CompositeNode>> future = Futures.immediateFuture(expResult);
        when(mockConsumerSession.rpc(qname, dataNode)).thenReturn(future);

        Future<RpcResult<CompositeNode>> actualFuture = brokerFacade.invokeRpc(qname, dataNode);
        assertNotNull("Future is null", actualFuture);
        RpcResult<CompositeNode> actualResult = actualFuture.get();

        assertSame("invokeRpc", expResult, actualResult);
    }

    @Test(expected = RestconfDocumentedException.class)
    public void testInvokeRpcWithNoConsumerSession() {
        brokerFacade.setContext(null);

        brokerFacade.invokeRpc(qname, dataNode);
    }

    @Test
    public void testCommitConfigurationDataPut() {
        Future<RpcResult<TransactionStatus>> expFuture = Futures.immediateFuture(null);

        when(dataBroker.beginTransaction()).thenReturn(mockTransaction);
        mockTransaction.putConfigurationData(instanceID, dataNode);
        when(mockTransaction.commit()).thenReturn(expFuture);

        Future<RpcResult<TransactionStatus>> actualFuture = brokerFacade.commitConfigurationDataPut(instanceID,
                dataNode);

        assertSame("invokeRpc", expFuture, actualFuture);

        InOrder inOrder = inOrder(dataBroker, mockTransaction);
        inOrder.verify(dataBroker).beginTransaction();
        inOrder.verify(mockTransaction).putConfigurationData(instanceID, dataNode);
        inOrder.verify(mockTransaction).commit();
    }

    @Test
    public void testCommitConfigurationDataPutBehindMountPoint() {
        Future<RpcResult<TransactionStatus>> expFuture = Futures.immediateFuture(null);

        when(mockMountInstance.beginTransaction()).thenReturn(mockTransaction);
        mockTransaction.putConfigurationData(instanceID, dataNode);
        when(mockTransaction.commit()).thenReturn(expFuture);

        Future<RpcResult<TransactionStatus>> actualFuture = brokerFacade.commitConfigurationDataPutBehindMountPoint(
                mockMountInstance, instanceID, dataNode);

        assertSame("invokeRpc", expFuture, actualFuture);

        InOrder inOrder = inOrder(mockMountInstance, mockTransaction);
        inOrder.verify(mockMountInstance).beginTransaction();
        inOrder.verify(mockTransaction).putConfigurationData(instanceID, dataNode);
        inOrder.verify(mockTransaction).commit();
    }

    @Test
    public void testCommitConfigurationDataPost() {
        Future<RpcResult<TransactionStatus>> expFuture = Futures.immediateFuture(null);

        Map<InstanceIdentifier, CompositeNode> nodeMap = new ImmutableMap.Builder<InstanceIdentifier, CompositeNode>()
                .put(instanceID, dataNode).build();

        when(dataBroker.beginTransaction()).thenReturn(mockTransaction);
        mockTransaction.putConfigurationData(instanceID, dataNode);
        when(mockTransaction.getCreatedConfigurationData()).thenReturn(nodeMap);
        when(mockTransaction.commit()).thenReturn(expFuture);

        Future<RpcResult<TransactionStatus>> actualFuture = brokerFacade.commitConfigurationDataPost(instanceID,
                dataNode);

        assertSame("commitConfigurationDataPut", expFuture, actualFuture);

        InOrder inOrder = inOrder(dataBroker, mockTransaction);
        inOrder.verify(dataBroker).beginTransaction();
        inOrder.verify(mockTransaction).putConfigurationData(instanceID, dataNode);
        inOrder.verify(mockTransaction).commit();
    }

    @Test(expected = RestconfDocumentedException.class)
    public void testCommitConfigurationDataPostAlreadyExists() {
        when(dataBroker.beginTransaction()).thenReturn(mockTransaction);
        mockTransaction.putConfigurationData(instanceID, dataNode);
        when(mockTransaction.readConfigurationData(instanceID)).thenReturn(dataNode);
        try {
            brokerFacade.commitConfigurationDataPost(instanceID, dataNode);
        } catch (RestconfDocumentedException e) {
            assertEquals("getErrorTag", RestconfError.ErrorTag.DATA_EXISTS, e.getErrors().get(0).getErrorTag());
            throw e;
        }
    }

    @Test
    public void testCommitConfigurationDataPostBehindMountPoint() {
        Future<RpcResult<TransactionStatus>> expFuture = Futures.immediateFuture(null);

        Map<InstanceIdentifier, CompositeNode> nodeMap = new ImmutableMap.Builder<InstanceIdentifier, CompositeNode>()
                .put(instanceID, dataNode).build();

        when(mockMountInstance.beginTransaction()).thenReturn(mockTransaction);
        mockTransaction.putConfigurationData(instanceID, dataNode);
        when(mockTransaction.getCreatedConfigurationData()).thenReturn(nodeMap);
        when(mockTransaction.commit()).thenReturn(expFuture);

        Future<RpcResult<TransactionStatus>> actualFuture = brokerFacade.commitConfigurationDataPostBehindMountPoint(
                mockMountInstance, instanceID, dataNode);

        assertSame("commitConfigurationDataPostBehindMountPoint", expFuture, actualFuture);

        InOrder inOrder = inOrder(mockMountInstance, mockTransaction);
        inOrder.verify(mockMountInstance).beginTransaction();
        inOrder.verify(mockTransaction).putConfigurationData(instanceID, dataNode);
        inOrder.verify(mockTransaction).commit();
    }

    @Test(expected = RestconfDocumentedException.class)
    public void testCommitConfigurationDataPostBehindMountPointAlreadyExists() {

        when(mockMountInstance.beginTransaction()).thenReturn(mockTransaction);
        mockTransaction.putConfigurationData(instanceID, dataNode);
        when(mockTransaction.readConfigurationData(instanceID)).thenReturn(dataNode);
        try {
            brokerFacade.commitConfigurationDataPostBehindMountPoint(mockMountInstance, instanceID, dataNode);
        } catch (RestconfDocumentedException e) {
            assertEquals("getErrorTag", RestconfError.ErrorTag.DATA_EXISTS, e.getErrors().get(0).getErrorTag());
            throw e;
        }
    }

    @Test
    public void testCommitConfigurationDataDelete() {
        Future<RpcResult<TransactionStatus>> expFuture = Futures.immediateFuture(null);

        when(dataBroker.beginTransaction()).thenReturn(mockTransaction);
        when(mockTransaction.readConfigurationData(any(InstanceIdentifier.class))).thenReturn(
                ImmutableCompositeNode.builder().toInstance());
        mockTransaction.removeConfigurationData(instanceID);
        when(mockTransaction.commit()).thenReturn(expFuture);

        Future<RpcResult<TransactionStatus>> actualFuture = brokerFacade.commitConfigurationDataDelete(instanceID);

        assertSame("commitConfigurationDataDelete", expFuture, actualFuture);

        InOrder inOrder = inOrder(dataBroker, mockTransaction);
        inOrder.verify(dataBroker).beginTransaction();
        inOrder.verify(mockTransaction).removeConfigurationData(instanceID);
        inOrder.verify(mockTransaction).commit();
    }

    @Test
    public void testCommitConfigurationDataDeleteBehindMountPoint() {
        Future<RpcResult<TransactionStatus>> expFuture = Futures.immediateFuture(null);

        when(mockMountInstance.beginTransaction()).thenReturn(mockTransaction);
        when(mockTransaction.readConfigurationData(any(InstanceIdentifier.class))).thenReturn(
                ImmutableCompositeNode.builder().toInstance());
        mockTransaction.removeConfigurationData(instanceID);
        when(mockTransaction.commit()).thenReturn(expFuture);

        Future<RpcResult<TransactionStatus>> actualFuture = brokerFacade.commitConfigurationDataDeleteBehindMountPoint(
                mockMountInstance, instanceID);

        assertSame("commitConfigurationDataDeleteBehindMountPoint", expFuture, actualFuture);

        InOrder inOrder = inOrder(mockMountInstance, mockTransaction);
        inOrder.verify(mockMountInstance).beginTransaction();
        inOrder.verify(mockTransaction).removeConfigurationData(instanceID);
        inOrder.verify(mockTransaction).commit();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRegisterToListenDataChanges() {
        ListenerAdapter listener = Notificator.createListener(instanceID, "stream");

        ListenerRegistration<DataChangeListener> mockRegistration = mock(ListenerRegistration.class);
        when(dataBroker.registerDataChangeListener(instanceID, listener)).thenReturn(mockRegistration);

        brokerFacade.registerToListenDataChanges(listener);

        verify(dataBroker).registerDataChangeListener(instanceID, listener);

        assertEquals("isListening", true, listener.isListening());

        brokerFacade.registerToListenDataChanges(listener);
        verifyNoMoreInteractions(dataBroker);
    }
}
