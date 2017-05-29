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
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.assertFutureEquals;

import akka.testkit.TestProbe;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import org.junit.Assert;
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
import org.opendaylight.mdsal.common.api.ReadFailedException;
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
        final CheckedFuture<Boolean, ReadFailedException> exists = transaction.exists(PATH_1);
        final ExistsTransactionRequest req = tester.expectTransactionRequest(ExistsTransactionRequest.class);
        final boolean existsResult = true;
        tester.replySuccess(new ExistsTransactionSuccess(TRANSACTION_ID, req.getSequence(), existsResult));
        assertFutureEquals(existsResult, exists);
    }

    @Override
    @Test
    public void testRead() throws Exception {
        final TransactionTester<RemoteProxyTransaction> tester = getTester();
        final CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read = transaction.read(PATH_2);
        final ReadTransactionRequest req = tester.expectTransactionRequest(ReadTransactionRequest.class);
        final Optional<NormalizedNode<?, ?>> result = Optional.of(DATA_1);
        tester.replySuccess(new ReadTransactionSuccess(TRANSACTION_ID, req.getSequence(), result));
        assertFutureEquals(result, read);
    }

    @Override
    @Test
    public void testWrite() throws Exception {
        final YangInstanceIdentifier path = PATH_1;
        testModification(() -> transaction.write(path, DATA_1), TransactionWrite.class, path);
    }

    @Override
    @Test
    public void testMerge() throws Exception {
        final YangInstanceIdentifier path = PATH_2;
        testModification(() -> transaction.merge(path, DATA_2), TransactionMerge.class, path);
    }

    @Override
    @Test
    public void testDelete() throws Exception {
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
        Assert.assertTrue(req.getPersistenceProtocol().isPresent());
        Assert.assertEquals(PersistenceProtocol.SIMPLE, req.getPersistenceProtocol().get());
        tester.replySuccess(new TransactionCommitSuccess(TRANSACTION_ID, req.getSequence()));
        assertFutureEquals(true, result);
    }

    @Override
    @Test
    public void testCanCommit() throws Exception {
        testRequestResponse(transaction::canCommit, ModifyTransactionRequest.class,
                TransactionCanCommitSuccess::new);
    }

    @Override
    @Test
    public void testPreCommit() throws Exception {
        testRequestResponse(transaction::preCommit, TransactionPreCommitRequest.class,
                TransactionPreCommitSuccess::new);
    }

    @Override
    @Test
    public void testDoCommit() throws Exception {
        testRequestResponse(transaction::doCommit, TransactionDoCommitRequest.class, TransactionCommitSuccess::new);
    }

    @Override
    @Test
    public void testForwardToRemoteAbort() throws Exception {
        final TestProbe probe = createProbe();
        final TransactionAbortRequest request = new TransactionAbortRequest(TRANSACTION_ID, 0L, probe.ref());
        testForwardToRemote(request, TransactionAbortRequest.class);

    }

    @Override
    public void testForwardToRemoteCommit() throws Exception {
        final TestProbe probe = createProbe();
        final TransactionAbortRequest request = new TransactionAbortRequest(TRANSACTION_ID, 0L, probe.ref());
        testForwardToRemote(request, TransactionAbortRequest.class);
    }

    @Test
    public void testForwardToRemoteModifyCommitSimple() throws Exception {
        final TestProbe probe = createProbe();
        final ModifyTransactionRequestBuilder builder =
                new ModifyTransactionRequestBuilder(TRANSACTION_ID, probe.ref());
        builder.setSequence(0L);
        builder.setCommit(false);
        final ModifyTransactionRequest request = builder.build();
        final ModifyTransactionRequest received = testForwardToRemote(request, ModifyTransactionRequest.class);
        Assert.assertEquals(request.getPersistenceProtocol(), received.getPersistenceProtocol());
        Assert.assertEquals(request.getModifications(), received.getModifications());
        Assert.assertEquals(request.getTarget(), received.getTarget());
    }

    @Test
    public void testForwardToRemoteModifyCommit3Phase() throws Exception {
        final TestProbe probe = createProbe();
        final ModifyTransactionRequestBuilder builder =
                new ModifyTransactionRequestBuilder(TRANSACTION_ID, probe.ref());
        builder.setSequence(0L);
        builder.setCommit(true);
        final ModifyTransactionRequest request = builder.build();
        final ModifyTransactionRequest received = testForwardToRemote(request, ModifyTransactionRequest.class);
        Assert.assertEquals(request.getPersistenceProtocol(), received.getPersistenceProtocol());
        Assert.assertEquals(request.getModifications(), received.getModifications());
        Assert.assertEquals(request.getTarget(), received.getTarget());
    }

    @Test
    public void testForwardToRemoteModifyAbort() throws Exception {
        final TestProbe probe = createProbe();
        final ModifyTransactionRequestBuilder builder =
                new ModifyTransactionRequestBuilder(TRANSACTION_ID, probe.ref());
        builder.setSequence(0L);
        builder.setAbort();
        final ModifyTransactionRequest request = builder.build();
        final ModifyTransactionRequest received = testForwardToRemote(request, ModifyTransactionRequest.class);
        Assert.assertEquals(request.getTarget(), received.getTarget());
        Assert.assertTrue(received.getPersistenceProtocol().isPresent());
        Assert.assertEquals(PersistenceProtocol.ABORT, received.getPersistenceProtocol().get());
    }

    @Test
    public void testForwardToRemoteModifyRead() throws Exception {
        final TestProbe probe = createProbe();
        final ReadTransactionRequest request =
                new ReadTransactionRequest(TRANSACTION_ID, 0L, probe.ref(), PATH_1, false);
        final ReadTransactionRequest received = testForwardToRemote(request, ReadTransactionRequest.class);
        Assert.assertEquals(request.getTarget(), received.getTarget());
        Assert.assertEquals(request.getPath(), received.getPath());
    }

    @Test
    public void testForwardToRemoteModifyExists() throws Exception {
        final TestProbe probe = createProbe();
        final ExistsTransactionRequest request =
                new ExistsTransactionRequest(TRANSACTION_ID, 0L, probe.ref(), PATH_1, false);
        final ExistsTransactionRequest received = testForwardToRemote(request, ExistsTransactionRequest.class);
        Assert.assertEquals(request.getTarget(), received.getTarget());
        Assert.assertEquals(request.getPath(), received.getPath());
    }

    @Test
    public void testForwardToRemoteModifyPreCommit() throws Exception {
        final TestProbe probe = createProbe();
        final TransactionPreCommitRequest request =
                new TransactionPreCommitRequest(TRANSACTION_ID, 0L, probe.ref());
        final TransactionPreCommitRequest received = testForwardToRemote(request, TransactionPreCommitRequest.class);
        Assert.assertEquals(request.getTarget(), received.getTarget());
    }

    @Test
    public void testForwardToRemoteModifyDoCommit() throws Exception {
        final TestProbe probe = createProbe();
        final TransactionDoCommitRequest request =
                new TransactionDoCommitRequest(TRANSACTION_ID, 0L, probe.ref());
        final TransactionDoCommitRequest received = testForwardToRemote(request, TransactionDoCommitRequest.class);
        Assert.assertEquals(request.getTarget(), received.getTarget());
    }


    private <T extends TransactionModification> void testModification(final Runnable modification,
                                                                      final Class<T> cls,
                                                                      final YangInstanceIdentifier expectedPath) {
        modification.run();
        final ModifyTransactionRequest request = transaction.commitRequest(false);
        final List<TransactionModification> modifications = request.getModifications();
        Assert.assertEquals(1, modifications.size());
        Assert.assertThat(modifications, hasItem(both(isA(cls)).and(hasPath(expectedPath))));
    }

}