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
import static org.junit.Assert.assertTrue;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.assertFutureEquals;

import akka.testkit.TestProbe;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.commands.ExistsTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ExistsTransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequestBuilder;
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
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;

public class RemoteProxyTransactionTest extends AbstractProxyTransactionTest<RemoteProxyTransaction> {

    @Override
    protected RemoteProxyTransaction createTransaction(final ProxyHistory parent, final TransactionIdentifier id,
                                                       final DataTreeSnapshot snapshot) {
        return new RemoteProxyTransaction(parent, TRANSACTION_ID, false, false, false);
    }

    @Override
    @Test
    public void testExists() throws Exception {
        final TransactionTester<RemoteProxyTransaction> tester = getTester();
        final FluentFuture<Boolean> exists = transaction.exists(PATH_1);
        final ExistsTransactionRequest req = tester.expectTransactionRequest(ExistsTransactionRequest.class);
        final boolean existsResult = true;
        tester.replySuccess(new ExistsTransactionSuccess(TRANSACTION_ID, req.getSequence(), existsResult));
        assertFutureEquals(existsResult, exists);
    }

    @Override
    @Test
    public void testRead() throws Exception {
        final TransactionTester<RemoteProxyTransaction> tester = getTester();
        final FluentFuture<Optional<NormalizedNode>> read = transaction.read(PATH_2);
        final ReadTransactionRequest req = tester.expectTransactionRequest(ReadTransactionRequest.class);
        final Optional<NormalizedNode> result = Optional.of(DATA_1);
        tester.replySuccess(new ReadTransactionSuccess(TRANSACTION_ID, req.getSequence(), result));
        assertFutureEquals(result, read);
    }

    @Override
    @Test
    public void testWrite() {
        final YangInstanceIdentifier path = PATH_1;
        testModification(() -> transaction.write(path, DATA_1), TransactionWrite.class, path);
    }

    @Override
    @Test
    public void testMerge() {
        final YangInstanceIdentifier path = PATH_2;
        testModification(() -> transaction.merge(path, DATA_2), TransactionMerge.class, path);
    }

    @Override
    @Test
    public void testDelete() {
        final YangInstanceIdentifier path = PATH_3;
        testModification(() -> transaction.delete(path), TransactionDelete.class, path);
    }

    @Override
    @Test
    public void testDirectCommit() throws Exception {
        transaction.seal();
        final ListenableFuture<Boolean> result = transaction.directCommit();
        final TransactionTester<RemoteProxyTransaction> tester = getTester();
        final ModifyTransactionRequest req = tester.expectTransactionRequest(ModifyTransactionRequest.class);
        assertTrue(req.getPersistenceProtocol().isPresent());
        assertEquals(PersistenceProtocol.SIMPLE, req.getPersistenceProtocol().get());
        tester.replySuccess(new TransactionCommitSuccess(TRANSACTION_ID, req.getSequence()));
        assertFutureEquals(true, result);
    }

    @Override
    @Test
    public void testCanCommit() {
        testRequestResponse(transaction::canCommit, ModifyTransactionRequest.class,
                TransactionCanCommitSuccess::new);
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
        final TestProbe probe = createProbe();
        final TransactionAbortRequest request = new TransactionAbortRequest(TRANSACTION_ID, 0L, probe.ref());
        testForwardToRemote(request, TransactionAbortRequest.class);

    }

    @Override
    public void testForwardToRemoteCommit() {
        final TestProbe probe = createProbe();
        final TransactionAbortRequest request = new TransactionAbortRequest(TRANSACTION_ID, 0L, probe.ref());
        testForwardToRemote(request, TransactionAbortRequest.class);
    }

    @Test
    public void testForwardToRemoteModifyCommitSimple() {
        final TestProbe probe = createProbe();
        final ModifyTransactionRequestBuilder builder =
                new ModifyTransactionRequestBuilder(TRANSACTION_ID, probe.ref());
        builder.setSequence(0L);
        builder.setCommit(false);
        final ModifyTransactionRequest request = builder.build();
        final ModifyTransactionRequest received = testForwardToRemote(request, ModifyTransactionRequest.class);
        assertEquals(request.getPersistenceProtocol(), received.getPersistenceProtocol());
        assertEquals(request.getModifications(), received.getModifications());
        assertEquals(request.getTarget(), received.getTarget());
    }

    @Test
    public void testForwardToRemoteModifyCommit3Phase() {
        final TestProbe probe = createProbe();
        final ModifyTransactionRequestBuilder builder =
                new ModifyTransactionRequestBuilder(TRANSACTION_ID, probe.ref());
        builder.setSequence(0L);
        builder.setCommit(true);
        final ModifyTransactionRequest request = builder.build();
        final ModifyTransactionRequest received = testForwardToRemote(request, ModifyTransactionRequest.class);
        assertEquals(request.getPersistenceProtocol(), received.getPersistenceProtocol());
        assertEquals(request.getModifications(), received.getModifications());
        assertEquals(request.getTarget(), received.getTarget());
    }

    @Test
    public void testForwardToRemoteModifyAbort() {
        final TestProbe probe = createProbe();
        final ModifyTransactionRequestBuilder builder =
                new ModifyTransactionRequestBuilder(TRANSACTION_ID, probe.ref());
        builder.setSequence(0L);
        builder.setAbort();
        final ModifyTransactionRequest request = builder.build();
        final ModifyTransactionRequest received = testForwardToRemote(request, ModifyTransactionRequest.class);
        assertEquals(request.getTarget(), received.getTarget());
        assertTrue(received.getPersistenceProtocol().isPresent());
        assertEquals(PersistenceProtocol.ABORT, received.getPersistenceProtocol().get());
    }

    @Test
    public void testForwardToRemoteModifyRead() {
        final TestProbe probe = createProbe();
        final ReadTransactionRequest request =
                new ReadTransactionRequest(TRANSACTION_ID, 0L, probe.ref(), PATH_1, false);
        final ReadTransactionRequest received = testForwardToRemote(request, ReadTransactionRequest.class);
        assertEquals(request.getTarget(), received.getTarget());
        assertEquals(request.getPath(), received.getPath());
    }

    @Test
    public void testForwardToRemoteModifyExists() {
        final TestProbe probe = createProbe();
        final ExistsTransactionRequest request =
                new ExistsTransactionRequest(TRANSACTION_ID, 0L, probe.ref(), PATH_1, false);
        final ExistsTransactionRequest received = testForwardToRemote(request, ExistsTransactionRequest.class);
        assertEquals(request.getTarget(), received.getTarget());
        assertEquals(request.getPath(), received.getPath());
    }

    @Test
    public void testForwardToRemoteModifyPreCommit() {
        final TestProbe probe = createProbe();
        final TransactionPreCommitRequest request =
                new TransactionPreCommitRequest(TRANSACTION_ID, 0L, probe.ref());
        final TransactionPreCommitRequest received = testForwardToRemote(request, TransactionPreCommitRequest.class);
        assertEquals(request.getTarget(), received.getTarget());
    }

    @Test
    public void testForwardToRemoteModifyDoCommit() {
        final TestProbe probe = createProbe();
        final TransactionDoCommitRequest request =
                new TransactionDoCommitRequest(TRANSACTION_ID, 0L, probe.ref());
        final TransactionDoCommitRequest received = testForwardToRemote(request, TransactionDoCommitRequest.class);
        assertEquals(request.getTarget(), received.getTarget());
    }


    private <T extends TransactionModification> void testModification(final Runnable modification, final Class<T> cls,
            final YangInstanceIdentifier expectedPath) {
        modification.run();
        final ModifyTransactionRequest request = transaction.commitRequest(false);
        final List<TransactionModification> modifications = request.getModifications();
        assertEquals(1, modifications.size());
        assertThat(modifications, hasItem(both(isA(cls)).and(hasPath(expectedPath))));
    }
}
