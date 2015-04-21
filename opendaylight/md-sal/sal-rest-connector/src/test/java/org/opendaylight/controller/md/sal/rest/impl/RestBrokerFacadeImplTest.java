/**
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.rest.impl;

import static org.junit.Assert.fail;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.rest.RestBrokerFacade;
import org.opendaylight.controller.md.sal.rest.RestConnectorProviderImpl;
import org.opendaylight.controller.md.sal.rest.common.TestRestconfUtils;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.streams.listeners.ListenerAdapter;
import org.opendaylight.controller.sal.streams.listeners.Notificator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

/**
 * sal-rest-connector
 * org.opendaylight.controller.md.sal.rest.impl
 *
 * Base test class method for quick testing of {@link RestBrokerFacade} implementation
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Apr 21, 2015
 */
@RunWith(MockitoJUnitRunner.class)
public class RestBrokerFacadeImplTest {

    private RestBrokerFacade restBroker;
    private SchemaContext schemaCx;
    private YangInstanceIdentifier rootYii;
    private RestConnectorProviderImpl restConnectorProvider;

    @Mock
    private ProviderSession session;
    @Mock
    private DOMMountPointService mPointService;
    @Mock
    private SchemaService schemaService;
    @Mock
    private DOMRpcService domRpcService;
    @Mock
    private DOMDataBroker domDataBroker;

    @Before
    public void initialization() {
        schemaCx = TestRestconfUtils.loadSchemaContext("/modules", null);
        Mockito.when(schemaService.getGlobalContext()).thenReturn(schemaCx);
        Mockito.when(session.getService(DOMMountPointService.class)).thenReturn(mPointService);
        Mockito.when(session.getService(SchemaService.class)).thenReturn(schemaService);
        Mockito.when(session.getService(DOMRpcService.class)).thenReturn(domRpcService);
        Mockito.when(session.getService(DOMDataBroker.class)).thenReturn(domDataBroker);
        restConnectorProvider = RestConnectorProviderImpl.getInstance();
        restConnectorProvider.setWebsocketPort(new PortNumber(8888));
        restConnectorProvider.onSessionInitiated(session);
        restConnectorProvider = RestConnectorProviderImpl.getInstance();
        rootYii = YangInstanceIdentifier.builder().build();
        restBroker = RestConnectorProviderImpl.getRestBroker();
    }

    @After
    public void closing() throws Exception {
        restConnectorProvider.close();
    }

    /**
     * Test method for {@link RestBrokerFacadeImpl#RestBrokerFacadeImpl(DOMDataBroker, DOMRpcService)}.
     */
    @Test(expected = NullPointerException.class)
    public void testRestBrokerFacadeImplNullInput() {
        final RestBrokerFacadeImpl failRestBroker = new RestBrokerFacadeImpl(null, null);
        fail("Expect NullPointerException for " + failRestBroker);
    }

    /**
     * Test method for {@link RestBrokerFacadeImpl#RestBrokerFacadeImpl(DOMDataBroker, DOMRpcService)}.
     */
    @Test(expected = NullPointerException.class)
    public void testRestBrokerFacadeImplNullRpcService() {
        final RestBrokerFacadeImpl failRestBroker = new RestBrokerFacadeImpl(domDataBroker, null);
        fail("Expect NullPointerException for " + failRestBroker);
    }

    /**
     * Test method for {@link RestBrokerFacadeImpl#readConfigurationData(YangInstanceIdentifier)}.
     */
    @Test
    public void testReadConfigurationDataYangInstanceIdentifier() {
        final NormalizedNode<?, ?> nn = Mockito.mock(NormalizedNode.class);
        final DOMDataBroker mockDataBroker = mockReadDefaultDataBroker(nn, rootYii, LogicalDatastoreType.CONFIGURATION);
        restBroker = new RestBrokerFacadeImpl(mockDataBroker, domRpcService);
        final NormalizedNode<?, ?> result = restBroker.readConfigurationData(rootYii);
        Assert.assertNotNull(result);
        Assert.assertEquals(nn, result);
    }

    /**
     * Test method for {@link RestBrokerFacadeImpl#readConfigurationData(DOMMountPoint, YangInstanceIdentifier)}.
     */
    @Test
    public void testReadConfigurationDataDOMMountPointYangInstanceIdentifier() {
        final NormalizedNode<?, ?> nn = Mockito.mock(NormalizedNode.class);
        final DOMDataBroker mockDataBroker = mockReadDefaultDataBroker(nn, rootYii, LogicalDatastoreType.CONFIGURATION);
        final DOMMountPoint mountPoint = Mockito.mock(DOMMountPoint.class);
        Mockito.when(mountPoint.getService(DOMDataBroker.class)).thenReturn(Optional.<DOMDataBroker> of(mockDataBroker));
        final NormalizedNode<?, ?> result = restBroker.readConfigurationData(mountPoint, rootYii);
        Assert.assertNotNull(result);
        Assert.assertEquals(nn, result);
    }

    /**
     * Test method for {@link RestBrokerFacadeImpl#readOperationalData(YangInstanceIdentifier)}.
     */
    @Test
    public void testReadOperationalDataYangInstanceIdentifier() {
        final NormalizedNode<?, ?> nn = Mockito.mock(NormalizedNode.class);
        final DOMDataBroker mockDataBroker = mockReadDefaultDataBroker(nn, rootYii, LogicalDatastoreType.OPERATIONAL);
        restBroker = new RestBrokerFacadeImpl(mockDataBroker, domRpcService);
        final NormalizedNode<?, ?> result = restBroker.readOperationalData(rootYii);
        Assert.assertNotNull(result);
        Assert.assertEquals(nn, result);
    }

    /**
     * Test method for {@link RestBrokerFacadeImpl#readOperationalData(DOMMountPoint, YangInstanceIdentifier)}.
     */
    @Test
    public void testReadOperationalDataDOMMountPointYangInstanceIdentifier() {
        final NormalizedNode<?, ?> nn = Mockito.mock(NormalizedNode.class);
        final DOMDataBroker mockDataBroker = mockReadDefaultDataBroker(nn, rootYii, LogicalDatastoreType.OPERATIONAL);
        final DOMMountPoint mountPoint = Mockito.mock(DOMMountPoint.class);
        Mockito.when(mountPoint.getService(DOMDataBroker.class)).thenReturn(Optional.<DOMDataBroker> of(mockDataBroker));
        final NormalizedNode<?, ?> result = restBroker.readOperationalData(mountPoint, rootYii);
        Assert.assertNotNull(result);
        Assert.assertEquals(nn, result);
    }

    /**
     * Test method for {@link RestBrokerFacadeImpl#commitConfigurationDataPut(YangInstanceIdentifier, NormalizedNode)}.
     */
    @Test
    public void testCommitConfigurationDataPutYangInstanceIdentifierNormalizedNodeOfQQ() {
        final NormalizedNode<?, ?> nn = Mockito.mock(NormalizedNode.class);
        final DOMDataBroker mockDataBroker = mockWriteDefaultDataBroker(rootYii);
        final DOMDataWriteTransaction wTx = mockDataBroker.newReadWriteTransaction();
        restBroker = new RestBrokerFacadeImpl(mockDataBroker, domRpcService);
        final CheckedFuture<Void, TransactionCommitFailedException> result = restBroker.commitConfigurationDataPut(rootYii, nn);
        Assert.assertNotNull(result);
        Mockito.verify(wTx, Mockito.times(1)).merge(LogicalDatastoreType.CONFIGURATION, rootYii, nn);
    }

    /**
     * Test method for {@link RestBrokerFacadeImpl#commitConfigurationDataPut(DOMMountPoint, YangInstanceIdentifier, NormalizedNode)}.
     */
    @Test
    public void testCommitConfigurationDataPutDOMMountPointYangInstanceIdentifierNormalizedNodeOfQQ() {
        final NormalizedNode<?, ?> nn = Mockito.mock(NormalizedNode.class);
        final DOMDataBroker mockDataBroker = mockWriteDefaultDataBroker(rootYii);
        final DOMDataWriteTransaction wTx = mockDataBroker.newReadWriteTransaction();
        final DOMMountPoint mountPoint = Mockito.mock(DOMMountPoint.class);
        Mockito.when(mountPoint.getService(DOMDataBroker.class)).thenReturn(Optional.<DOMDataBroker> of(mockDataBroker));
        Mockito.when(mountPoint.getSchemaContext()).thenReturn(schemaCx);
        final CheckedFuture<Void, TransactionCommitFailedException> result =
                restBroker.commitConfigurationDataPut(mountPoint, rootYii, nn);
        Assert.assertNotNull(result);
        Mockito.verify(wTx, Mockito.times(1)).merge(LogicalDatastoreType.CONFIGURATION, rootYii, nn);
    }

    /**
     * Test method for {@link RestBrokerFacadeImpl#commitConfigurationDataPost(YangInstanceIdentifier, NormalizedNode)}.
     */
    @Test
    public void testCommitConfigurationDataPostYangInstanceIdentifierNormalizedNodeOfQQ() {
        final NormalizedNode<?, ?> nn = Mockito.mock(NormalizedNode.class);
        final DOMDataBroker mockDataBroker = mockWriteDefaultDataBroker(rootYii);
        final DOMDataReadWriteTransaction wTx = mockDataBroker.newReadWriteTransaction();
        restBroker = new RestBrokerFacadeImpl(mockDataBroker, domRpcService);
        final CheckedFuture<Void, TransactionCommitFailedException> result =
                restBroker.commitConfigurationDataPost(rootYii, nn);
        Assert.assertNotNull(result);
        Mockito.verify(wTx, Mockito.timeout(1)).read(LogicalDatastoreType.CONFIGURATION, rootYii);
        Mockito.verify(wTx, Mockito.times(1)).put(LogicalDatastoreType.CONFIGURATION, rootYii, nn);
    }

    /**
     * Test method for {@link RestBrokerFacadeImpl#commitConfigurationDataPost(DOMMountPoint, YangInstanceIdentifier, NormalizedNode)}.
     */
    @Test
    public void testCommitConfigurationDataPostDOMMountPointYangInstanceIdentifierNormalizedNodeOfQQ() {
        final NormalizedNode<?, ?> nn = Mockito.mock(NormalizedNode.class);
        final DOMDataBroker mockDataBroker = mockWriteDefaultDataBroker(rootYii);
        final DOMDataReadWriteTransaction wTx = mockDataBroker.newReadWriteTransaction();
        final DOMMountPoint mountPoint = Mockito.mock(DOMMountPoint.class);
        Mockito.when(mountPoint.getService(DOMDataBroker.class)).thenReturn(Optional.<DOMDataBroker> of(mockDataBroker));
        Mockito.when(mountPoint.getSchemaContext()).thenReturn(schemaCx);
        final CheckedFuture<Void, TransactionCommitFailedException> result =
                restBroker.commitConfigurationDataPost(mountPoint, rootYii, nn);
        Assert.assertNotNull(result);
        Mockito.verify(wTx, Mockito.timeout(1)).read(LogicalDatastoreType.CONFIGURATION, rootYii);
        Mockito.verify(wTx, Mockito.times(1)).put(LogicalDatastoreType.CONFIGURATION, rootYii, nn);
    }

    /**
     * Test method for {@link RestBrokerFacadeImpl#commitConfigurationDataDelete(YangInstanceIdentifier)}.
     */
    @Test
    public void testCommitConfigurationDataDeleteYangInstanceIdentifier() {
        final DOMDataBroker mockDataBroker = mockDeleteDefaultDataBroker();
        final DOMDataWriteTransaction wTx = mockDataBroker.newWriteOnlyTransaction();
        restBroker = new RestBrokerFacadeImpl(mockDataBroker, domRpcService);
        final CheckedFuture<Void, TransactionCommitFailedException> result =
                restBroker.commitConfigurationDataDelete(rootYii);
        Assert.assertNotNull(result);
        Mockito.verify(wTx, Mockito.times(1)).delete(LogicalDatastoreType.CONFIGURATION, rootYii);
    }

    /**
     * Test method for {@link RestBrokerFacadeImpl#commitConfigurationDataDelete(DOMMountPoint, YangInstanceIdentifier)}.
     */
    @Test
    public void testCommitConfigurationDataDeleteDOMMountPointYangInstanceIdentifier() {
        final DOMDataBroker mockDataBroker = mockDeleteDefaultDataBroker();
        final DOMDataWriteTransaction wTx = mockDataBroker.newWriteOnlyTransaction();
        final DOMMountPoint mountPoint = Mockito.mock(DOMMountPoint.class);
        Mockito.when(mountPoint.getService(DOMDataBroker.class)).thenReturn(Optional.<DOMDataBroker> of(mockDataBroker));
        final CheckedFuture<Void, TransactionCommitFailedException> result =
                restBroker.commitConfigurationDataDelete(mountPoint, rootYii);
        Assert.assertNotNull(result);
        Mockito.verify(wTx, Mockito.times(1)).delete(LogicalDatastoreType.CONFIGURATION, rootYii);
    }

    /**
     * Test method for {@link RestBrokerFacadeImpl#invokeRpc(org.opendaylight.yangtools.yang.model.api.SchemaPath, NormalizedNode)}.
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    public void testInvokeRpc() throws InterruptedException, ExecutionException {
        final SchemaPath schemaPath = Mockito.mock(SchemaPath.class);
        final NormalizedNode<?, ?> nn = Mockito.mock(NormalizedNode.class);
        final DOMRpcResult rpcResult = Mockito.mock(DOMRpcResult.class);
        final CheckedFuture<DOMRpcResult, DOMRpcException> checkedFuture =
                Futures.immediateCheckedFuture(rpcResult);
        Mockito.when(domRpcService.invokeRpc(schemaPath, nn)).thenReturn(checkedFuture);
        final CheckedFuture<DOMRpcResult, DOMRpcException> result = restBroker.invokeRpc(schemaPath, nn);
        Assert.assertNotNull(result);
        Assert.assertEquals(rpcResult, result.get());
    }

    /**
     * Test method for {@link RestBrokerFacadeImpl#registerToListenDataChanges(LogicalDatastoreType, AsyncDataBroker.DataChangeScope, ListenerAdapter)}.
     */
    @Test
    public void testRegisterToListenDataChanges() {
        final LogicalDatastoreType ds = LogicalDatastoreType.OPERATIONAL;
        final DataChangeScope scope = DataChangeScope.BASE;
        final ListenerAdapter listener = Notificator.createListener(rootYii, "test");
        restBroker.registerToListenDataChanges(ds , scope , listener);
        Mockito.verify(domDataBroker, Mockito.times(1)).registerDataChangeListener(ds, rootYii, listener, scope);
    }

    private static DOMDataBroker mockReadDefaultDataBroker(final NormalizedNode<?, ?> nn, final YangInstanceIdentifier yii, final LogicalDatastoreType ds) {
        final DOMDataBroker mockDataBroker = Mockito.mock(DOMDataBroker.class);
        final DOMDataReadOnlyTransaction readOnlyTx = Mockito.mock(DOMDataReadOnlyTransaction.class);
        final CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> checkedFuture =
                Futures.immediateCheckedFuture(Optional.<NormalizedNode<?, ?>> of(nn));
        Mockito.when(readOnlyTx.read(ds, yii)).thenReturn(checkedFuture);
        Mockito.when(mockDataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTx);
        return mockDataBroker;
    }

    private static DOMDataBroker mockDeleteDefaultDataBroker() {
        final DOMDataBroker mockDataBroker = Mockito.mock(DOMDataBroker.class);
        final DOMDataWriteTransaction writeOnlyTx = Mockito.mock(DOMDataWriteTransaction.class);
        final CheckedFuture<Void, TransactionCommitFailedException> checkedFuture = Futures.immediateCheckedFuture(null);
        Mockito.when(writeOnlyTx.submit()).thenReturn(checkedFuture);
        Mockito.when(mockDataBroker.newWriteOnlyTransaction()).thenReturn(writeOnlyTx);
        return mockDataBroker;
    }

    private static DOMDataBroker mockWriteDefaultDataBroker(final YangInstanceIdentifier yii) {
        final DOMDataBroker mockDataBroker = Mockito.mock(DOMDataBroker.class);
        final DOMDataReadWriteTransaction readWriteTx = Mockito.mock(DOMDataReadWriteTransaction.class);
        final CheckedFuture<Void, TransactionCommitFailedException> checkedFuture = Futures.immediateCheckedFuture(null);
        final CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> checkedReadFuture =
                Futures.immediateCheckedFuture(Optional.<NormalizedNode<?, ?>> absent());
        Mockito.when(readWriteTx.read(LogicalDatastoreType.CONFIGURATION, yii)).thenReturn(checkedReadFuture);
        Mockito.when(readWriteTx.submit()).thenReturn(checkedFuture);
        Mockito.when(mockDataBroker.newReadWriteTransaction()).thenReturn(readWriteTx);
        return mockDataBroker;
    }
}
