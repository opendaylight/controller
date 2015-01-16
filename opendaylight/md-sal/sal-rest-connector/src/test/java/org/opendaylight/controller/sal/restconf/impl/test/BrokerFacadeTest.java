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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import java.util.concurrent.Future;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.sal.core.api.Broker.ConsumerSession;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.controller.sal.restconf.impl.RestconfError;
import org.opendaylight.controller.sal.streams.listeners.ListenerAdapter;
import org.opendaylight.controller.sal.streams.listeners.Notificator;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;

/**
 * Unit tests for BrokerFacade.
 *
 * @author Thomas Pantelis
 */
public class BrokerFacadeTest {

    @Mock
    DOMDataBroker domDataBroker;

    @Mock
    ConsumerSession mockConsumerSession;

    @Mock
    DOMMountPoint mockMountInstance;

    BrokerFacade brokerFacade = BrokerFacade.getInstance();

    CompositeNode dataNode;

    NormalizedNode<?, ?> dummyNode = createDummyNode("test:module", "2014-01-09", "interfaces");
    CheckedFuture<Optional<NormalizedNode<?, ?>>,ReadFailedException> dummyNodeInFuture = wrapDummyNode(dummyNode);

    QName qname = TestUtils.buildQName("interfaces","test:module", "2014-01-09");

    YangInstanceIdentifier instanceID = YangInstanceIdentifier.builder().node(qname).build();

    @Mock
    DOMDataReadOnlyTransaction rTransaction;

    @Mock
    DOMDataWriteTransaction wTransaction;

    @Mock
    DOMDataReadWriteTransaction rwTransaction;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        // TODO it is started before every test method
        brokerFacade.setDomDataBroker(domDataBroker);
        brokerFacade.setContext(mockConsumerSession);
        when(domDataBroker.newReadOnlyTransaction()).thenReturn(rTransaction);
        when(domDataBroker.newWriteOnlyTransaction()).thenReturn(wTransaction);
        when(domDataBroker.newReadWriteTransaction()).thenReturn(rwTransaction);

        dataNode = TestUtils.prepareCompositeNodeWithIetfInterfacesInterfacesData();

        ControllerContext.getInstance().setSchemas(TestUtils.loadSchemaContext("/full-versions/test-module"));

    }

    private CheckedFuture<Optional<NormalizedNode<?, ?>>,ReadFailedException> wrapDummyNode(final NormalizedNode<?, ?> dummyNode) {
        return  Futures.immediateCheckedFuture(Optional.<NormalizedNode<?, ?>> of(dummyNode));
    }

    private CheckedFuture<Boolean,ReadFailedException> wrapExistence(Boolean exists) {
        return  Futures.immediateCheckedFuture(exists);
    }


    /**
     * Value of this node shouldn't be important for testing purposes
     */
    private NormalizedNode<?, ?> createDummyNode(final String namespace, final String date, final String localName) {
        return Builders.containerBuilder()
                .withNodeIdentifier(new NodeIdentifier(QName.create(namespace, date, localName))).build();
    }

    @Test
    public void testReadConfigurationData() {
        when(rTransaction.read(any(LogicalDatastoreType.class), any(YangInstanceIdentifier.class))).thenReturn(
                dummyNodeInFuture);

        NormalizedNode<?, ?> actualNode = brokerFacade.readConfigurationData(instanceID);

        assertSame("readConfigurationData", dummyNode, actualNode);
    }

    @Test
    public void testReadOperationalData() {
        when(rTransaction.read(any(LogicalDatastoreType.class), any(YangInstanceIdentifier.class))).thenReturn(
                dummyNodeInFuture);

        NormalizedNode<?, ?> actualNode = brokerFacade.readOperationalData(instanceID);

        assertSame("readOperationalData", dummyNode, actualNode);
    }

    @Test(expected = RestconfDocumentedException.class)
    public void testReadOperationalDataWithNoDataBroker() {
        brokerFacade.setDomDataBroker(null);

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

    @Ignore
    @Test
    public void testCommitConfigurationDataPut() {
        CheckedFuture<Void, TransactionCommitFailedException> expFuture = mock(CheckedFuture.class);

        when(wTransaction.submit()).thenReturn(expFuture);

        Future<Void> actualFuture = brokerFacade.commitConfigurationDataPut(instanceID, dummyNode);

        assertSame("commitConfigurationDataPut", expFuture, actualFuture);

        InOrder inOrder = inOrder(domDataBroker, wTransaction);
        inOrder.verify(domDataBroker).newWriteOnlyTransaction();
        inOrder.verify(wTransaction).put(LogicalDatastoreType.CONFIGURATION, instanceID, dummyNode);
        inOrder.verify(wTransaction).submit();
    }

    @Test
    public void testCommitConfigurationDataPost() {
        CheckedFuture<Void, TransactionCommitFailedException> expFuture = mock(CheckedFuture.class);

        NormalizedNode<?, ?> dummyNode2 = createDummyNode("dummy:namespace2", "2014-07-01", "dummy local name2");

        when(rwTransaction.read(eq(LogicalDatastoreType.CONFIGURATION), any(YangInstanceIdentifier.class))).thenReturn(
                wrapDummyNode(dummyNode2));

        when(rwTransaction.exists(eq(LogicalDatastoreType.CONFIGURATION), any(YangInstanceIdentifier.class))).thenReturn(
            wrapExistence(true));


        when(rwTransaction.submit()).thenReturn(expFuture);

        CheckedFuture<Void, TransactionCommitFailedException> actualFuture = brokerFacade.commitConfigurationDataPost(
                instanceID, dummyNode);

        assertSame("commitConfigurationDataPost", expFuture, actualFuture);

        InOrder inOrder = inOrder(domDataBroker, rwTransaction);
        inOrder.verify(domDataBroker).newReadWriteTransaction();
        inOrder.verify(rwTransaction).merge(LogicalDatastoreType.CONFIGURATION, instanceID, dummyNode);
        inOrder.verify(rwTransaction).submit();
    }

    @Test(expected = RestconfDocumentedException.class)
    public void testCommitConfigurationDataPostAlreadyExists() {
        when(rwTransaction.read(eq(LogicalDatastoreType.CONFIGURATION), any(YangInstanceIdentifier.class))).thenReturn(
                dummyNodeInFuture);
        try {
            brokerFacade.commitConfigurationDataPost(instanceID, dummyNode);
        } catch (RestconfDocumentedException e) {
            assertEquals("getErrorTag", RestconfError.ErrorTag.DATA_EXISTS, e.getErrors().get(0).getErrorTag());
            throw e;
        }
    }

    @Test
    public void testCommitConfigurationDataDelete() {
        CheckedFuture<Void, TransactionCommitFailedException> expFuture = mock(CheckedFuture.class);

        when(wTransaction.submit()).thenReturn(expFuture);

        NormalizedNode<?, ?> dummyNode2 = createDummyNode("dummy:namespace2", "2014-07-01", "dummy local name2");


        CheckedFuture<Void, TransactionCommitFailedException> actualFuture = brokerFacade
                .commitConfigurationDataDelete(instanceID);

        assertSame("commitConfigurationDataDelete", expFuture, actualFuture);

        InOrder inOrder = inOrder(domDataBroker, wTransaction);
        inOrder.verify(domDataBroker).newWriteOnlyTransaction();
        inOrder.verify(wTransaction).delete(eq(LogicalDatastoreType.CONFIGURATION), any(YangInstanceIdentifier.class));
        inOrder.verify(wTransaction).submit();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRegisterToListenDataChanges() {
        ListenerAdapter listener = Notificator.createListener(instanceID, "stream");

        ListenerRegistration<DOMDataChangeListener> mockRegistration = mock(ListenerRegistration.class);

        when(
                domDataBroker.registerDataChangeListener(any(LogicalDatastoreType.class), eq(instanceID), eq(listener),
                        eq(DataChangeScope.BASE))).thenReturn(mockRegistration);

        brokerFacade.registerToListenDataChanges(LogicalDatastoreType.CONFIGURATION, DataChangeScope.BASE, listener);

        verify(domDataBroker).registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, instanceID, listener,
                DataChangeScope.BASE);

        assertEquals("isListening", true, listener.isListening());

        brokerFacade.registerToListenDataChanges(LogicalDatastoreType.CONFIGURATION, DataChangeScope.BASE, listener);
        verifyNoMoreInteractions(domDataBroker);

    }
}
