/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.TRANSACTION_ID;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.assertFutureEquals;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.assertOperationThrowsException;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.getWithTimeout;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.controller.cluster.access.commands.CommitLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionCommitSuccess;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteCursor;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.CursorAwareDataTreeModification;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;

public class ClientTransactionTest extends AbstractClientHandleTest<ClientTransaction> {

    private static final YangInstanceIdentifier PATH = YangInstanceIdentifier.builder()
            .node(QName.create("ns-1", "node-1"))
            .build();
    private static final NormalizedNode<?, ?> DATA = Builders.containerBuilder()
            .withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(PATH.getLastPathArgument().getNodeType()))
            .build();

    @Mock
    private CursorAwareDataTreeModification modification;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        when(getDataTreeSnapshot().newModification()).thenReturn(modification);
        when(modification.readNode(PATH)).thenReturn(Optional.of(DATA));
    }

    @Override
    protected ClientTransaction createHandle(final AbstractClientHistory parent) {
        return parent.createTransaction();
    }

    @Override
    protected void doHandleOperation(final ClientTransaction transaction) {
        transaction.read(PATH);
    }

    @Test
    public void testOpenCloseCursor() throws Exception {
        final DOMDataTreeWriteCursor cursor = getHandle().openCursor();
        getHandle().closeCursor(cursor);
        getHandle().openCursor().delete(PATH.getLastPathArgument());
        verify(modification).delete(PATH);
    }

    @Test
    public void testOpenSecondCursor() throws Exception {
        getHandle().openCursor();
        assertOperationThrowsException(getHandle()::openCursor, IllegalStateException.class);
    }

    @Test
    public void testExists() throws Exception {
        final CheckedFuture<Boolean, ReadFailedException> exists = getHandle().exists(PATH);
        verify(modification).readNode(PATH);
        Assert.assertTrue(getWithTimeout(exists));
    }

    @Test
    public void testRead() throws Exception {
        final CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> resultFuture = getHandle().read(PATH);
        verify(modification).readNode(PATH);
        final Optional<NormalizedNode<?, ?>> result = getWithTimeout(resultFuture);
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(DATA, result.get());
    }

    @Test
    public void testDelete() throws Exception {
        getHandle().delete(PATH);
        verify(modification).delete(PATH);
    }

    @Test
    public void testMerge() throws Exception {
        getHandle().merge(PATH, DATA);
        verify(modification).merge(PATH, DATA);
    }

    @Test
    public void testWrite() throws Exception {
        getHandle().write(PATH, DATA);
        verify(modification).write(PATH, DATA);
    }

    @Test
    public void testReadyEmpty() throws Exception {
        final DOMStoreThreePhaseCommitCohort cohort = getHandle().ready();
        assertFutureEquals(true, cohort.canCommit());
        assertFutureEquals(null, cohort.preCommit());
        assertFutureEquals(null, cohort.commit());
    }

    @Test
    public void testReady() throws Exception {
        getHandle().write(PATH, DATA);
        final DOMStoreThreePhaseCommitCohort cohort = getHandle().ready();
        final TransactionCommitSuccess response = new TransactionCommitSuccess(TRANSACTION_ID, 0L);
        final ListenableFuture<Boolean> actual = cohort.canCommit();
        final CommitLocalTransactionRequest request =
                backendRespondToRequest(CommitLocalTransactionRequest.class, response);
        Assert.assertEquals(modification, request.getModification());
        assertFutureEquals(true, actual);
        assertFutureEquals(null, cohort.preCommit());
        assertFutureEquals(null, cohort.commit());
    }

    @Test
    public void testReadyNoFurtherOperationsAllowed() throws Exception {
        getHandle().ready();
        checkClosed();
    }

}