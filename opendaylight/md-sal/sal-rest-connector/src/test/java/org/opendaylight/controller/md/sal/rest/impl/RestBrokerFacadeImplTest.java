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
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.rest.AbstractRestConnectorTest;
import org.opendaylight.controller.md.sal.rest.RestBrokerFacade;
import org.opendaylight.controller.md.sal.rest.RestConnectorProviderImpl;
import org.opendaylight.controller.sal.streams.listeners.ListenerAdapter;
import org.opendaylight.controller.sal.streams.listeners.Notificator;
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
public class RestBrokerFacadeImplTest extends AbstractRestConnectorTest {

    private RestBrokerFacade restBroker;
    private SchemaContext schemaCx;
    private YangInstanceIdentifier rootYii;

    @Before
    @Override
    public void initialization() {
        super.initialization();
        schemaCx = RestConnectorProviderImpl.getSchemaContext();
        rootYii = YangInstanceIdentifier.builder().build();
        restBroker = RestConnectorProviderImpl.getRestBroker();
    }

    @After
    @Override
    public void closing() throws Exception {
        super.closing();
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
        final RestBrokerFacadeImpl failRestBroker = new RestBrokerFacadeImpl(mockDataBroker, null);
        fail("Expect NullPointerException for " + failRestBroker);
    }

    /**
     * Test method for {@link RestBrokerFacadeImpl#readConfigurationData(YangInstanceIdentifier)}.
     */
    @Test
    public void testReadConfigurationDataYangInstanceIdentifier() {
        final NormalizedNode<?, ?> nn = Mockito.mock(NormalizedNode.class);
        mockReadDefaultDataBroker(nn, rootYii, LogicalDatastoreType.CONFIGURATION);
        restBroker = new RestBrokerFacadeImpl(mockDataBroker, mockRpcService);
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
        mockReadDefaultDataBroker(nn, rootYii, LogicalDatastoreType.CONFIGURATION);
        final DOMMountPoint mountPoint = Mockito.mock(DOMMountPoint.class);
        Mockito.when(mountPoint.getService(DOMDataBroker.class))
                .thenReturn(Optional.<DOMDataBroker> of(mockDataBroker));
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
        mockReadDefaultDataBroker(nn, rootYii, LogicalDatastoreType.OPERATIONAL);
        restBroker = new RestBrokerFacadeImpl(mockDataBroker, mockRpcService);
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
        mockReadDefaultDataBroker(nn, rootYii, LogicalDatastoreType.OPERATIONAL);
        final DOMMountPoint mountPoint = Mockito.mock(DOMMountPoint.class);
        Mockito.when(mountPoint.getService(DOMDataBroker.class))
                .thenReturn(Optional.<DOMDataBroker> of(mockDataBroker));
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
        mockWriteDefaultDataBroker(rootYii);
        final DOMDataWriteTransaction wTx = mockDataBroker.newReadWriteTransaction();
        restBroker = new RestBrokerFacadeImpl(mockDataBroker, mockRpcService);
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
        mockWriteDefaultDataBroker(rootYii);
        final DOMDataWriteTransaction wTx = mockDataBroker.newReadWriteTransaction();
        final DOMMountPoint mountPoint = Mockito.mock(DOMMountPoint.class);
        Mockito.when(mountPoint.getService(DOMDataBroker.class))
                .thenReturn(Optional.<DOMDataBroker> of(mockDataBroker));
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
        mockWriteDefaultDataBroker(rootYii);
        final DOMDataReadWriteTransaction wTx = mockDataBroker.newReadWriteTransaction();
        restBroker = new RestBrokerFacadeImpl(mockDataBroker, mockRpcService);
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
        mockWriteDefaultDataBroker(rootYii);
        final DOMDataReadWriteTransaction wTx = mockDataBroker.newReadWriteTransaction();
        final DOMMountPoint mountPoint = Mockito.mock(DOMMountPoint.class);
        Mockito.when(mountPoint.getService(DOMDataBroker.class))
                .thenReturn(Optional.<DOMDataBroker> of(mockDataBroker));
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
        mockDeleteDefaultDataBroker();
        final DOMDataWriteTransaction wTx = mockDataBroker.newWriteOnlyTransaction();
        restBroker = new RestBrokerFacadeImpl(mockDataBroker, mockRpcService);
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
        mockDeleteDefaultDataBroker();
        final DOMDataWriteTransaction wTx = mockDataBroker.newWriteOnlyTransaction();
        final DOMMountPoint mountPoint = Mockito.mock(DOMMountPoint.class);
        Mockito.when(mountPoint.getService(DOMDataBroker.class))
                .thenReturn(Optional.<DOMDataBroker> of(mockDataBroker));
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
        Mockito.when(mockRpcService.invokeRpc(schemaPath, nn)).thenReturn(checkedFuture);
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
        Mockito.verify(mockDataBroker, Mockito.times(1)).registerDataChangeListener(ds, rootYii, listener, scope);
    }
}
