/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.assertOperationThrowsException;

import com.google.common.base.Ticker;
import com.google.common.base.VerifyException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.opendaylight.controller.cluster.access.commands.AbortLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequest;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeSnapshot;

class LocalReadOnlyProxyTransactionTest extends LocalProxyTransactionTest<LocalReadOnlyProxyTransaction> {
    private DataTreeSnapshot snapshot;

    @Override
    @SuppressWarnings("checkstyle:hiddenField")
    LocalReadOnlyProxyTransaction createTransaction(final ProxyHistory parent, final TransactionIdentifier id,
            final DataTreeSnapshot snapshot) {
        when(snapshot.readNode(PATH_1)).thenReturn(Optional.of(DATA_1));
        when(snapshot.readNode(PATH_3)).thenReturn(Optional.empty());
        this.snapshot = snapshot;
        return new LocalReadOnlyProxyTransaction(parent, id, this.snapshot);
    }

    @Test
    void testIsSnapshotOnly() {
        assertTrue(transaction.isSnapshotOnly());
    }

    @Test
    void testReadOnlyView() {
        assertSame(snapshot, transaction.readOnlyView());
    }

    @Test
    @Override
    void testDirectCommit() {
        assertThrows(UnsupportedOperationException.class, () -> transaction.directCommit());
    }

    @Test
    @Override
    void testCanCommit() {
        assertThrows(UnsupportedOperationException.class,
            () -> transaction.canCommit(new VotingFuture<>(new Object(), 1)));
    }

    @Test
    @Override
    void testPreCommit() {
        assertThrows(UnsupportedOperationException.class,
            () -> transaction.preCommit(new VotingFuture<>(new Object(), 1)));
    }

    @Test
    @Override
    void testDoCommit() {
        assertThrows(UnsupportedOperationException.class,
            () -> transaction.doCommit(new VotingFuture<>(new Object(), 1)));
    }

    @Test
    @Override
    void testDelete() {
        assertThrows(UnsupportedOperationException.class, () -> transaction.delete(PATH_1));
    }

    @Override
    void testMerge() {
        assertThrows(UnsupportedOperationException.class, () -> transaction.merge(PATH_1, DATA_1));
    }

    @Test
    @Override
    void testWrite() {
        assertThrows(UnsupportedOperationException.class, () -> transaction.write(PATH_1, DATA_1));
    }

    @Test
    void testDoDelete() {
        assertThrows(UnsupportedOperationException.class, () -> transaction.doDelete(PATH_1));
    }

    @Test
    void testDoMerge() {
        assertThrows(UnsupportedOperationException.class, () -> transaction.doMerge(PATH_1, DATA_1));
    }

    @Test
    void testDoWrite() {
        assertThrows(UnsupportedOperationException.class, () -> transaction.doWrite(PATH_1, DATA_1));
    }

    @Test
    void testCommitRequest() {
        assertThrows(UnsupportedOperationException.class, () -> transaction.commitRequest(true));
    }

    @Test
    void testApplyModifyTransactionRequest() {
        final var probe = createProbe();
        final var request = ModifyTransactionRequest.builder(TRANSACTION_ID, probe.ref())
            .setSequence(0)
            .setAbort()
            .build();
        transaction.replayModifyTransactionRequest(request, mock(), Ticker.systemTicker().read());
        getTester().expectTransactionRequest(AbortLocalTransactionRequest.class);
    }

    @Test
    void testApplyModifyTransactionRequestNotAbort() throws Exception {
        final var probe = createProbe();
        final var request = ModifyTransactionRequest.builder(TRANSACTION_ID, probe.ref())
            .setSequence(0)
            .setReady()
            .build();
        assertOperationThrowsException(() -> transaction.replayModifyTransactionRequest(request, mock(),
            Ticker.systemTicker().read()), VerifyException.class);
    }
}
