/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.opendaylight.controller.sal.dom.broker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.opendaylight.controller.md.sal.dom.broker.compat.hydrogen.BackwardsCompatibleMountPoint;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.AbstractMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationException;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizer;
import org.opendaylight.controller.sal.core.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.core.api.data.DataProviderService;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class BackwardsCompatibleMountPointTest {
    private static final Logger log = LoggerFactory.getLogger(BackwardsCompatibleMountPointManagerTest.class);

    private static final YangInstanceIdentifier id = BackwardsCompatibleMountPointManagerTest.id;
    private final NormalizedNode<?, ?> normalizedNode = mockNormalizedNode();
    private final CompositeNode compositeNode = mockCompositeNode();

    @Mock
    private DataProviderService oldBroker;
    @Mock
    private SchemaContextProvider schemaContextProvider;
    @Mock
    private DataModificationTransaction mockTx;

    private BackwardsCompatibleMountPoint.BackwardsCompatibleDomStore backwardsCompatibleDomStore;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        stubSchemaProvider();
        stubOldBroker();
        backwardsCompatibleDomStore = new BackwardsCompatibleMountPoint.BackwardsCompatibleDomStore(oldBroker, schemaContextProvider);
    }

    private void stubOldBroker() {
        doReturn(compositeNode).when(oldBroker).readConfigurationData(id);
        doReturn(compositeNode).when(oldBroker).readOperationalData(id);
        doReturn(mockTx).when(oldBroker).beginTransaction();
        doNothing().when(mockTx).putConfigurationData(id, compositeNode);
        doNothing().when(mockTx).putOperationalData(id, compositeNode);
        doReturn(com.google.common.util.concurrent.Futures.immediateFuture(RpcResultBuilder.success(TransactionStatus.COMMITED))).when(mockTx).commit();
    }

    private CompositeNode mockCompositeNode() {
        final CompositeNode mock = mock(CompositeNode.class);
        doReturn("node").when(mock).toString();
        return mock;
    }

    private void stubSchemaProvider() {
        doReturn(BackwardsCompatibleMountPointManagerTest.mockSchemaContext()).when(schemaContextProvider).getSchemaContext();
    }

    @Test
    public void testBackwardsCompatibleBroker() throws Exception {
        backwardsCompatibleDomStore.newReadOnlyTransaction();
        backwardsCompatibleDomStore.newWriteOnlyTransaction();
        backwardsCompatibleDomStore.newReadWriteTransaction();
    }

    @Test
    public void testReadTransaction() throws Exception {
        final BackwardsCompatibleMountPoint.BackwardsCompatibleDomStore.BackwardsCompatibleReadTransaction tx =
                new BackwardsCompatibleMountPoint.BackwardsCompatibleDomStore.BackwardsCompatibleReadTransaction(oldBroker, mockNormalizer());

        ListenableFuture<Optional<NormalizedNode<?, ?>>> read = tx.read(LogicalDatastoreType.CONFIGURATION, id);
        assertEquals(normalizedNode, read.get().get());
        verify(oldBroker).readConfigurationData(id);

        read = tx.read(LogicalDatastoreType.OPERATIONAL, id);
        assertEquals(normalizedNode, read.get().get());

        verify(oldBroker).readOperationalData(id);
    }

    @Test
    public void testReadWriteTransactionOperational() throws Exception {
        final BackwardsCompatibleMountPoint.BackwardsCompatibleDomStore.BackwardsCompatibleWriteTransaction tx =
                new BackwardsCompatibleMountPoint.BackwardsCompatibleDomStore.BackwardsCompatibleWriteTransaction(oldBroker, mockNormalizer());

        verify(oldBroker).beginTransaction();

        tx.put(LogicalDatastoreType.CONFIGURATION, id, normalizedNode);
        verify(mockTx).putConfigurationData(id, compositeNode);

        tx.put(LogicalDatastoreType.CONFIGURATION, id, normalizedNode);
        verify(mockTx, times(2)).putConfigurationData(id, compositeNode);

        tx.commit();
        verify(mockTx).commit();
    }


    @Test
    public void testCannotPutOperational() throws Exception {
        final BackwardsCompatibleMountPoint.BackwardsCompatibleDomStore.BackwardsCompatibleWriteTransaction tx =
                new BackwardsCompatibleMountPoint.BackwardsCompatibleDomStore.BackwardsCompatibleWriteTransaction(oldBroker, mockNormalizer());

        try {
            tx.put(LogicalDatastoreType.OPERATIONAL, id, normalizedNode);
        } catch (IllegalArgumentException e) {
            // Cannot put operational data
            log.debug("", e);
            return;
        }

        fail("Should fail when putting operational data");
    }

    private DataNormalizer mockNormalizer() throws DataNormalizationException {
        final DataNormalizer mock = mock(DataNormalizer.class);
        doReturn(new AbstractMap.SimpleEntry<YangInstanceIdentifier, NormalizedNode<?, ?>>(id, normalizedNode))
                .when(mock).toNormalized(any(YangInstanceIdentifier.class), any(CompositeNode.class));
        doReturn(compositeNode).when(mock).toLegacy(any(YangInstanceIdentifier.class), any(NormalizedNode.class));
        doReturn(id).when(mock).toLegacy(any(YangInstanceIdentifier.class));
        return mock;
    }

    private NormalizedNode<?, ?> mockNormalizedNode() {
        final NormalizedNode<?, ?> mock = mock(NormalizedNode.class);
        doReturn("mockNormalizedNode").when(mock).toString();
        return mock;
    }
}
