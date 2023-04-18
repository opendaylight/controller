/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.TRANSACTION_ID;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.assertFutureEquals;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.getWithTimeout;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.controller.cluster.access.commands.CommitLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionCommitSuccess;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.tree.api.CursorAwareDataTreeModification;

public class ClientTransactionTest extends AbstractClientHandleTest<ClientTransaction> {

    private static final YangInstanceIdentifier PATH = YangInstanceIdentifier.builder()
            .node(QName.create("ns-1", "node-1"))
            .build();
    private static final ContainerNode DATA = Builders.containerBuilder()
            .withNodeIdentifier(NodeIdentifier.create(PATH.getLastPathArgument().getNodeType()))
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
    public void testExists() throws Exception {
        final FluentFuture<Boolean> exists = getHandle().exists(PATH);
        verify(modification).readNode(PATH);
        assertEquals(Boolean.TRUE, getWithTimeout(exists));
    }

    @Test
    public void testRead() throws Exception {
        final FluentFuture<Optional<NormalizedNode>> resultFuture = getHandle().read(PATH);
        verify(modification).readNode(PATH);
        assertEquals(Optional.of(DATA), getWithTimeout(resultFuture));
    }

    @Test
    public void testDelete() {
        getHandle().delete(PATH);
        verify(modification).delete(PATH);
    }

    @Test
    public void testMerge() {
        getHandle().merge(PATH, DATA);
        verify(modification).merge(PATH, DATA);
    }

    @Test
    public void testWrite() {
        getHandle().write(PATH, DATA);
        verify(modification).write(PATH, DATA);
    }

    @Test
    public void testReadyEmpty() throws Exception {
        final DOMStoreThreePhaseCommitCohort cohort = getHandle().ready();
        assertFutureEquals(Boolean.TRUE, cohort.canCommit());
        assertFutureEquals(Empty.value(), cohort.preCommit());
        assertFutureEquals(CommitInfo.empty(), cohort.commit());
    }

    @Test
    public void testReady() throws Exception {
        getHandle().write(PATH, DATA);
        final DOMStoreThreePhaseCommitCohort cohort = getHandle().ready();
        final TransactionCommitSuccess response = new TransactionCommitSuccess(TRANSACTION_ID, 0L);
        final ListenableFuture<Boolean> actual = cohort.canCommit();
        final CommitLocalTransactionRequest request =
                backendRespondToRequest(CommitLocalTransactionRequest.class, response);
        assertEquals(modification, request.getModification());
        assertFutureEquals(Boolean.TRUE, actual);
        assertFutureEquals(Empty.value(), cohort.preCommit());
        assertFutureEquals(CommitInfo.empty(), cohort.commit());
    }

    @Test
    public void testReadyNoFurtherOperationsAllowed() throws Exception {
        getHandle().ready();
        checkClosed();
    }

}
