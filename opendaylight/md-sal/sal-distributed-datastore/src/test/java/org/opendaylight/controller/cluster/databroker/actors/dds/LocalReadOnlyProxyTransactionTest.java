/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.assertOperationThrowsException;

import com.google.common.base.Ticker;
import com.google.common.base.VerifyException;
import java.util.Optional;
import org.apache.pekko.testkit.TestProbe;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.commands.AbortLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequest;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeSnapshot;

public class LocalReadOnlyProxyTransactionTest extends LocalProxyTransactionTest<LocalReadOnlyProxyTransaction> {
    private DataTreeSnapshot snapshot;

    @Override
    @SuppressWarnings("checkstyle:hiddenField")
    protected LocalReadOnlyProxyTransaction createTransaction(final ProxyHistory parent, final TransactionIdentifier id,
            final DataTreeSnapshot snapshot) {
        when(snapshot.readNode(PATH_1)).thenReturn(Optional.of(DATA_1));
        when(snapshot.readNode(PATH_3)).thenReturn(Optional.empty());
        this.snapshot = snapshot;
        return new LocalReadOnlyProxyTransaction(parent, id, this.snapshot);
    }

    @Test
    public void testIsSnapshotOnly() {
        assertTrue(transaction.isSnapshotOnly());
    }

    @Test
    public void testReadOnlyView() {
        assertEquals(snapshot, transaction.readOnlyView());
    }

    @Test
    @Override
    public void testDirectCommit() {
        assertThrows(UnsupportedOperationException.class, () -> transaction.directCommit());
    }

    @Test
    @Override
    public void testCanCommit() {
        assertThrows(UnsupportedOperationException.class,
            () -> transaction.canCommit(new VotingFuture<>(new Object(), 1)));
    }

    @Test
    @Override
    public void testPreCommit() {
        assertThrows(UnsupportedOperationException.class,
            () -> transaction.preCommit(new VotingFuture<>(new Object(), 1)));
    }

    @Test
    @Override
    public void testDoCommit() {
        assertThrows(UnsupportedOperationException.class,
            () -> transaction.doCommit(new VotingFuture<>(new Object(), 1)));
    }

    @Test
    @Override
    public void testDelete() {
        assertThrows(UnsupportedOperationException.class, () -> transaction.delete(PATH_1));
    }

    @Override
    public void testMerge() {
        assertThrows(UnsupportedOperationException.class, () -> transaction.merge(PATH_1, DATA_1));
    }

    @Test
    @Override
    public void testWrite() {
        assertThrows(UnsupportedOperationException.class, () -> transaction.write(PATH_1, DATA_1));
    }

    @Test
    public void testDoDelete() {
        assertThrows(UnsupportedOperationException.class, () -> transaction.doDelete(PATH_1));
    }

    @Test
    public void testDoMerge() {
        assertThrows(UnsupportedOperationException.class, () -> transaction.doMerge(PATH_1, DATA_1));
    }

    @Test
    public void testDoWrite() {
        assertThrows(UnsupportedOperationException.class, () -> transaction.doWrite(PATH_1, DATA_1));
    }

    @Test
    public void testCommitRequest() {
        assertThrows(UnsupportedOperationException.class, () -> transaction.commitRequest(true));
    }

    @Test
    public void testApplyModifyTransactionRequest() {
        final TestProbe probe = createProbe();
        final ModifyTransactionRequest request = ModifyTransactionRequest.builder(TRANSACTION_ID, probe.ref())
            .setSequence(0)
            .setAbort()
            .build();
        transaction.replayModifyTransactionRequest(request, createCallbackMock(), Ticker.systemTicker().read());
        getTester().expectTransactionRequest(AbortLocalTransactionRequest.class);
    }

    @Test
    public void testApplyModifyTransactionRequestNotAbort() throws Exception {
        final TestProbe probe = createProbe();
        final ModifyTransactionRequest request = ModifyTransactionRequest.builder(TRANSACTION_ID, probe.ref())
            .setSequence(0)
            .setReady()
            .build();
        assertOperationThrowsException(() -> transaction.replayModifyTransactionRequest(request, createCallbackMock(),
            Ticker.systemTicker().read()), VerifyException.class);
    }
}
