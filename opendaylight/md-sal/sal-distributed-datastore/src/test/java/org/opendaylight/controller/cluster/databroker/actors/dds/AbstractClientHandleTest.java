/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.CLIENT_ID;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.HISTORY_ID;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.TRANSACTION_ID;

import java.util.List;
import java.util.Map;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.dispatch.ExecutionContexts;
import org.apache.pekko.testkit.TestProbe;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
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
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.tree.api.DataTree;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeSnapshot;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import scala.concurrent.Future;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public abstract class AbstractClientHandleTest<T extends AbstractClientHandle<AbstractProxyTransaction>> {
    private static final String PERSISTENCE_ID = "per-1";
    private static final YangInstanceIdentifier PATH = YangInstanceIdentifier.of();

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
        contextProbe.reply(new ConnectClientSuccess(CLIENT_ID, sequence, backendProbe.ref(), List.of(), dataTree, 3));
        final InternalCommand<ShardBackendInfo> command = clientContextProbe.expectMsgClass(InternalCommand.class);
        command.execute(client);
        //data tree mock
        doReturn(dataTreeSnapshot).when(dataTree).takeSnapshot();

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
        assertEquals(TRANSACTION_ID, handle.getIdentifier());
    }

    @Test
    public void testAbort() throws Exception {
        doHandleOperation(handle);
        handle.abort();
        final Envelope<?> envelope = backendProbe.expectMsgClass(Envelope.class);
        final AbortLocalTransactionRequest request = (AbortLocalTransactionRequest) envelope.getMessage();
        assertEquals(TRANSACTION_ID, request.getTarget());
        checkClosed();
    }

    @Test
    public void testLocalAbort() throws Exception {
        doHandleOperation(handle);
        handle.localAbort(new RuntimeException("fail"));
        final Envelope<?> envelope = backendProbe.expectMsgClass(Envelope.class);
        final AbortLocalTransactionRequest request = (AbortLocalTransactionRequest) envelope.getMessage();
        assertEquals(TRANSACTION_ID, request.getTarget());
        checkClosed();
    }

    @Test
    public void testEnsureClosed() {
        doHandleOperation(handle);
        final Map<Long, AbstractProxyTransaction> transactions = handle.ensureClosed();
        assertNotNull(transactions);
        assertEquals(1, transactions.size());
    }

    @Test
    public void testEnsureProxy() {
        final var proxy = handle.ensureProxy(PATH);
        assertEquals(0, proxy.getIdentifier().getTransactionId());
    }

    @Test
    public void testParent() {
        assertEquals(parent, handle.parent());
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
        assertEquals(expectedRequestClass, envelope.getMessage().getClass());
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
        final var actorUtils = mock(ActorUtils.class);
        doReturn(Future.successful(new PrimaryShardInfo(system.actorSelection(actor.path()), (short) 0)))
            .when(actorUtils).findPrimaryShardAsync(any());
        doReturn(ExecutionContexts.global()).when(actorUtils).getClientDispatcher();

        final var context = mock(EffectiveModelContext.class);
        lenient().doCallRealMethod().when(context).getQName();
        lenient().doReturn(context).when(actorUtils).getSchemaContext();
        lenient().doReturn(DatastoreContext.newBuilder().build()).when(actorUtils).getDatastoreContext();

        return actorUtils;
    }
}
