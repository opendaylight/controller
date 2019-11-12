/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.CLIENT_ID;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.HISTORY_ID;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.TRANSACTION_ID;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.testkit.TestProbe;
import akka.testkit.javadsl.TestKit;
import java.util.Collection;
import java.util.Collections;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.access.client.AbstractClientConnection;
import org.opendaylight.controller.cluster.access.client.AccessClientUtil;
import org.opendaylight.controller.cluster.access.client.ClientActorContext;
import org.opendaylight.controller.cluster.access.client.InternalCommand;
import org.opendaylight.controller.cluster.access.commands.AbortLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ConnectClientRequest;
import org.opendaylight.controller.cluster.access.commands.ConnectClientSuccess;
import org.opendaylight.controller.cluster.access.concepts.Envelope;
import org.opendaylight.controller.cluster.access.concepts.FailureEnvelope;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.RequestEnvelope;
import org.opendaylight.controller.cluster.access.concepts.RequestFailure;
import org.opendaylight.controller.cluster.access.concepts.RequestSuccess;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.access.concepts.SuccessEnvelope;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import scala.concurrent.Promise;

public abstract class AbstractClientHandleTest<T extends AbstractClientHandle<AbstractProxyTransaction>> {

    private static final String PERSISTENCE_ID = "per-1";
    private static final YangInstanceIdentifier PATH = YangInstanceIdentifier.empty();

    @Mock
    private DataTree dataTree;
    @Mock
    private DataTreeSnapshot dataTreeSnapshot;
    private ActorSystem system;
    private TestProbe backendProbe;
    private AbstractClientHistory parent;
    private AbstractDataStoreClientBehavior client;
    private T handle;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        system = ActorSystem.apply();
        final TestProbe contextProbe = new TestProbe(system, "context");
        final TestProbe clientContextProbe = new TestProbe(system, "client-context");
        backendProbe = new TestProbe(system, "backend");
        //create handle dependencies
        final ActorUtils actorUtils = createActorContextMock(system, contextProbe.ref());
        final ClientActorContext clientContext =
                AccessClientUtil.createClientActorContext(system, clientContextProbe.ref(), CLIENT_ID, PERSISTENCE_ID);
        client = new SimpleDataStoreClientBehavior(clientContext, actorUtils, "shard");
        client.createLocalHistory();
        parent = new SingleClientHistory(client, HISTORY_ID);
        //connect client
        client.getConnection(0L);
        contextProbe.expectMsgClass(ConnectClientRequest.class);
        final long sequence = 0L;
        contextProbe.reply(new ConnectClientSuccess(CLIENT_ID, sequence, backendProbe.ref(),
                Collections.emptyList(), dataTree, 3));
        final InternalCommand<ShardBackendInfo> command = clientContextProbe.expectMsgClass(InternalCommand.class);
        command.execute(client);
        //data tree mock
        when(dataTree.takeSnapshot()).thenReturn(dataTreeSnapshot);

        handle = createHandle(parent);
    }

    @SuppressWarnings("checkstyle:hiddenField")
    protected abstract T createHandle(AbstractClientHistory parent);

    /**
     * Do a operation with handle.
     * Used for testing, whether closed handle throws exception when the operation is performed.
     *
     * @param handle handle
     */
    @SuppressWarnings("checkstyle:hiddenField")
    protected abstract void doHandleOperation(T handle);

    @After
    public void tearDown() {
        TestKit.shutdownActorSystem(system);
    }

    @Test
    public void testGetIdentifier() {
        Assert.assertEquals(TRANSACTION_ID, handle.getIdentifier());
    }

    @Test
    public void testAbort() throws Exception {
        doHandleOperation(handle);
        handle.abort();
        final Envelope<?> envelope = backendProbe.expectMsgClass(Envelope.class);
        final AbortLocalTransactionRequest request = (AbortLocalTransactionRequest) envelope.getMessage();
        Assert.assertEquals(TRANSACTION_ID, request.getTarget());
        checkClosed();
    }

    @Test
    public void testLocalAbort() throws Exception {
        doHandleOperation(handle);
        handle.localAbort(new RuntimeException("fail"));
        final Envelope<?> envelope = backendProbe.expectMsgClass(Envelope.class);
        final AbortLocalTransactionRequest request = (AbortLocalTransactionRequest) envelope.getMessage();
        Assert.assertEquals(TRANSACTION_ID, request.getTarget());
        checkClosed();
    }

    @Test
    public void testEnsureClosed() {
        doHandleOperation(handle);
        final Collection<AbstractProxyTransaction> transactions = handle.ensureClosed();
        Assert.assertNotNull(transactions);
        Assert.assertEquals(1, transactions.size());
    }

    @Test
    public void testEnsureProxy() {
        final AbstractProxyTransaction expected = mock(AbstractProxyTransaction.class);
        final AbstractProxyTransaction proxy = handle.ensureProxy(PATH);
        Assert.assertEquals(0, proxy.getIdentifier().getTransactionId());
    }

    @Test
    public void testParent() {
        Assert.assertEquals(parent, handle.parent());
    }

    protected void checkClosed() throws Exception {
        TestUtils.assertOperationThrowsException(() -> doHandleOperation(handle), IllegalStateException.class);
    }

    /**
     * Checks, whether backend actor has received request of expected class wrapped in RequestEnvelope.
     * Then given response wrapped in ResponseEnvelope is sent.
     *
     * @param expectedRequestClass expected request class
     * @param response             response
     * @param <R>                  expected request type
     * @return request message
     */
    protected <R extends Request<?, R>> R backendRespondToRequest(final Class<R> expectedRequestClass,
                                                            final Response<?, ?> response) {
        final RequestEnvelope envelope = backendProbe.expectMsgClass(RequestEnvelope.class);
        Assert.assertEquals(expectedRequestClass, envelope.getMessage().getClass());
        final AbstractClientConnection<ShardBackendInfo> connection = client.getConnection(0L);
        final long sessionId = envelope.getSessionId();
        final long txSequence = envelope.getTxSequence();
        final long executionTime = 0L;
        if (response instanceof RequestSuccess) {
            final RequestSuccess<?, ?> success = (RequestSuccess<?, ?>) response;
            final SuccessEnvelope responseEnvelope = new SuccessEnvelope(success, sessionId, txSequence, executionTime);
            AccessClientUtil.completeRequest(connection, responseEnvelope);
        } else if (response instanceof RequestFailure) {
            final RequestFailure<?, ?> fail = (RequestFailure<?, ?>) response;
            final FailureEnvelope responseEnvelope = new FailureEnvelope(fail, sessionId, txSequence, executionTime);
            AccessClientUtil.completeRequest(connection, responseEnvelope);
        }
        return expectedRequestClass.cast(envelope.getMessage());
    }

    protected T getHandle() {
        return handle;
    }

    protected DataTreeSnapshot getDataTreeSnapshot() {
        return dataTreeSnapshot;
    }

    private static ActorUtils createActorContextMock(final ActorSystem system, final ActorRef actor) {
        final ActorUtils mock = mock(ActorUtils.class);
        final Promise<PrimaryShardInfo> promise = new scala.concurrent.impl.Promise.DefaultPromise<>();
        final ActorSelection selection = system.actorSelection(actor.path());
        final PrimaryShardInfo shardInfo = new PrimaryShardInfo(selection, (short) 0);
        promise.success(shardInfo);
        when(mock.findPrimaryShardAsync(any())).thenReturn(promise.future());
        return mock;
    }

}
