/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.assertFutureEquals;

import java.util.Optional;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.commands.ExistsTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ExistsTransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.PersistenceProtocol;
import org.opendaylight.controller.cluster.access.commands.ReadTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ReadTransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionAbortRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionCanCommitSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionCommitSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionDelete;
import org.opendaylight.controller.cluster.access.commands.TransactionDoCommitRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionMerge;
import org.opendaylight.controller.cluster.access.commands.TransactionModification;
import org.opendaylight.controller.cluster.access.commands.TransactionPreCommitRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionPreCommitSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionWrite;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeSnapshot;

public class RemoteProxyTransactionTest extends AbstractProxyTransactionTest<RemoteProxyTransaction> {
    @Override
    protected RemoteProxyTransaction createTransaction(final ProxyHistory parent, final TransactionIdentifier id,
                                                       final DataTreeSnapshot snapshot) {
        mockForRemote();
        return new RemoteProxyTransaction(parent, TRANSACTION_ID, false, false, false);
    }

    @Override
    @Test
    public void testExists() throws Exception {
        final var tester = getTester();
        final var exists = transaction.exists(PATH_1);
        final var req = tester.expectTransactionRequest(ExistsTransactionRequest.class);
        final boolean existsResult = true;
        tester.replySuccess(new ExistsTransactionSuccess(TRANSACTION_ID, req.getSequence(), existsResult));
        assertFutureEquals(existsResult, exists);
    }

    @Override
    @Test
    public void testRead() throws Exception {
        final var tester = getTester();
        final var read = transaction.read(PATH_2);
        final var req = tester.expectTransactionRequest(ReadTransactionRequest.class);
        final Optional<NormalizedNode> result = Optional.of(DATA_1);
        tester.replySuccess(new ReadTransactionSuccess(TRANSACTION_ID, req.getSequence(), result));
        assertFutureEquals(result, read);
    }

    @Override
    @Test
    public void testWrite() {
        final var path = PATH_1;
        testModification(() -> transaction.write(path, DATA_1), TransactionWrite.class, path);
    }

    @Override
    @Test
    public void testMerge() {
        final var path = PATH_2;
        testModification(() -> transaction.merge(path, DATA_2), TransactionMerge.class, path);
    }

    @Override
    @Test
    public void testDelete() {
        final var path = PATH_3;
        testModification(() -> transaction.delete(path), TransactionDelete.class, path);
    }

    @Override
    @Test
    public void testDirectCommit() throws Exception {
        transaction.seal();
        final var result = transaction.directCommit();
        final var tester = getTester();
        final var req = tester.expectTransactionRequest(ModifyTransactionRequest.class);
        assertEquals(PersistenceProtocol.SIMPLE, req.persistenceProtocol());
        tester.replySuccess(new TransactionCommitSuccess(TRANSACTION_ID, req.getSequence()));
        assertFutureEquals(true, result);
    }

    @Override
    @Test
    public void testCanCommit() {
        testRequestResponse(transaction::canCommit, ModifyTransactionRequest.class, TransactionCanCommitSuccess::new);
    }

    @Override
    @Test
    public void testPreCommit() {
        testRequestResponse(transaction::preCommit, TransactionPreCommitRequest.class,
            TransactionPreCommitSuccess::new);
    }

    @Override
    @Test
    public void testDoCommit() {
        testRequestResponse(transaction::doCommit, TransactionDoCommitRequest.class, TransactionCommitSuccess::new);
    }

    @Override
    @Test
    public void testForwardToRemoteAbort() {
        final var probe = createProbe();
        final var request = new TransactionAbortRequest(TRANSACTION_ID, 0L, probe.ref());
        testForwardToRemote(request, TransactionAbortRequest.class);
    }

    @Override
    public void testForwardToRemoteCommit() {
        final var probe = createProbe();
        final var request = new TransactionAbortRequest(TRANSACTION_ID, 0L, probe.ref());
        testForwardToRemote(request, TransactionAbortRequest.class);
    }

    @Test
    public void testForwardToRemoteModifyCommitSimple() {
        final var probe = createProbe();
        final var request = ModifyTransactionRequest.builder(TRANSACTION_ID, probe.ref())
            .setSequence(0)
            .setCommit(false)
            .build();
        final var received = testForwardToRemote(request, ModifyTransactionRequest.class);
        assertEquals(request.persistenceProtocol(), received.persistenceProtocol());
        assertEquals(request.modifications(), received.modifications());
        assertEquals(request.getTarget(), received.getTarget());
    }

    @Test
    public void testForwardToRemoteModifyCommit3Phase() {
        final var probe = createProbe();
        final var request = ModifyTransactionRequest.builder(TRANSACTION_ID, probe.ref())
            .setSequence(0)
            .setCommit(true)
            .build();
        final var received = testForwardToRemote(request, ModifyTransactionRequest.class);
        assertEquals(request.persistenceProtocol(), received.persistenceProtocol());
        assertEquals(request.modifications(), received.modifications());
        assertEquals(request.getTarget(), received.getTarget());
    }

    @Test
    public void testForwardToRemoteModifyAbort() {
        final var probe = createProbe();
        final var request = ModifyTransactionRequest.builder(TRANSACTION_ID, probe.ref())
            .setSequence(0)
            .setAbort()
            .build();
        final var received = testForwardToRemote(request, ModifyTransactionRequest.class);
        assertEquals(request.getTarget(), received.getTarget());
        assertEquals(PersistenceProtocol.ABORT, received.persistenceProtocol());
    }

    @Test
    public void testForwardToRemoteModifyRead() {
        final var probe = createProbe();
        final var request = new ReadTransactionRequest(TRANSACTION_ID, 0L, probe.ref(), PATH_1, false);
        final var received = testForwardToRemote(request, ReadTransactionRequest.class);
        assertEquals(request.getTarget(), received.getTarget());
        assertEquals(request.getPath(), received.getPath());
    }

    @Test
    public void testForwardToRemoteModifyExists() {
        final var probe = createProbe();
        final var request = new ExistsTransactionRequest(TRANSACTION_ID, 0L, probe.ref(), PATH_1, false);
        final var received = testForwardToRemote(request, ExistsTransactionRequest.class);
        assertEquals(request.getTarget(), received.getTarget());
        assertEquals(request.getPath(), received.getPath());
    }

    @Test
    public void testForwardToRemoteModifyPreCommit() {
        final var probe = createProbe();
        final var request = new TransactionPreCommitRequest(TRANSACTION_ID, 0L, probe.ref());
        final var received = testForwardToRemote(request, TransactionPreCommitRequest.class);
        assertEquals(request.getTarget(), received.getTarget());
    }

    @Test
    public void testForwardToRemoteModifyDoCommit() {
        final var probe = createProbe();
        final var request = new TransactionDoCommitRequest(TRANSACTION_ID, 0L, probe.ref());
        final var received = testForwardToRemote(request, TransactionDoCommitRequest.class);
        assertEquals(request.getTarget(), received.getTarget());
    }


    private <T extends TransactionModification> void testModification(final Runnable modification, final Class<T> cls,
            final YangInstanceIdentifier expectedPath) {
        modification.run();
        final var request = transaction.commitRequest(false);
        final var modifications = request.modifications();
        assertEquals(1, modifications.size());
        assertThat(modifications, hasItem(both(isA(cls)).and(hasPath(expectedPath))));
    }
}
