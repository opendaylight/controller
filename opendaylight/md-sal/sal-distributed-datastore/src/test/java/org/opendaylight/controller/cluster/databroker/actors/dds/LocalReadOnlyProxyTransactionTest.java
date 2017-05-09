/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static org.mockito.Mockito.when;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.assertOperationThrowsException;

import akka.testkit.TestProbe;
import com.google.common.base.Ticker;
import com.google.common.base.VerifyException;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.commands.AbortLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequestBuilder;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;

public class LocalReadOnlyProxyTransactionTest extends LocalProxyTransactionTest<LocalReadOnlyProxyTransaction> {

    private DataTreeSnapshot snapshot;

    @Override
    protected LocalReadOnlyProxyTransaction createTransaction(final ProxyHistory parent,
                                                              final TransactionIdentifier id,
                                                              final DataTreeSnapshot snapshot) {
        when(snapshot.readNode(PATH_1)).thenReturn(com.google.common.base.Optional.of(DATA_1));
        when(snapshot.readNode(PATH_3)).thenReturn(com.google.common.base.Optional.absent());
        this.snapshot = snapshot;
        return new LocalReadOnlyProxyTransaction(parent, id, this.snapshot);
    }

    @Test
    public void testIsSnapshotOnly() {
        Assert.assertTrue(transaction.isSnapshotOnly());
    }

    @Test
    public void testReadOnlyView() {
        Assert.assertEquals(snapshot, transaction.readOnlyView());
    }

    @Override
    @Test(expected = UnsupportedOperationException.class)
    public void testDirectCommit() throws Exception {
        transaction.directCommit();
    }

    @Override
    @Test(expected = UnsupportedOperationException.class)
    public void testCanCommit() throws Exception {
        transaction.canCommit(new VotingFuture<>(new Object(), 1));
    }

    @Override
    @Test(expected = UnsupportedOperationException.class)
    public void testPreCommit() throws Exception {
        transaction.preCommit(new VotingFuture<>(new Object(), 1));
    }

    @Override
    @Test(expected = UnsupportedOperationException.class)
    public void testDoCommit() throws Exception {
        transaction.doCommit(new VotingFuture<>(new Object(), 1));
    }

    @Override
    @Test(expected = UnsupportedOperationException.class)
    public void testDelete() {
        transaction.delete(PATH_1);
    }

    @Override
    @Test(expected = UnsupportedOperationException.class)
    public void testMerge() {
        transaction.merge(PATH_1, DATA_1);
    }

    @Override
    @Test(expected = UnsupportedOperationException.class)
    public void testWrite() {
        transaction.write(PATH_1, DATA_1);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDoDelete() {
        transaction.doDelete(PATH_1);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDoMerge() {
        transaction.doMerge(PATH_1, DATA_1);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDoWrite() {
        transaction.doWrite(PATH_1, DATA_1);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCommitRequest() {
        transaction.commitRequest(true);
    }

    @Test
    public void testApplyModifyTransactionRequest() throws Exception {
        final TestProbe probe = createProbe();
        final ModifyTransactionRequestBuilder builder =
                new ModifyTransactionRequestBuilder(TRANSACTION_ID, probe.ref());
        builder.setSequence(0);
        builder.setAbort();
        final ModifyTransactionRequest request = builder.build();
        transaction.replayModifyTransactionRequest(request, createCallbackMock(), Ticker.systemTicker().read());
        getTester().expectTransactionRequest(AbortLocalTransactionRequest.class);
    }

    @Test
    public void testApplyModifyTransactionRequestNotAbort() throws Exception {
        final TestProbe probe = createProbe();
        final ModifyTransactionRequestBuilder builder =
                new ModifyTransactionRequestBuilder(TRANSACTION_ID, probe.ref());
        builder.setSequence(0);
        builder.setReady();
        final ModifyTransactionRequest request = builder.build();
        assertOperationThrowsException(() -> transaction.replayModifyTransactionRequest(request, createCallbackMock(),
            Ticker.systemTicker().read()), VerifyException.class);
    }
}