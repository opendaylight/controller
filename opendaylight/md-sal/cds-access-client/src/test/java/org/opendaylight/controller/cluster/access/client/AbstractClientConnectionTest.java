/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.opendaylight.controller.cluster.access.client.ConnectionEntryMatcher.entryWithRequest;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import akka.testkit.TestProbe;
import com.google.common.collect.Iterables;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.access.commands.AbortLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionAbortSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionFailure;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.RequestEnvelope;
import org.opendaylight.controller.cluster.access.concepts.RequestSuccess;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.access.concepts.ResponseEnvelope;
import org.opendaylight.controller.cluster.access.concepts.RuntimeRequestException;
import org.opendaylight.controller.cluster.access.concepts.SuccessEnvelope;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

public abstract class AbstractClientConnectionTest<T extends AbstractClientConnection<U>, U extends BackendInfo> {

    protected static final MemberName MEMBER_NAME = MemberName.forName("member-1");
    protected static final FrontendType FRONTEND_TYPE =
            FrontendType.forName(ClientActorContextTest.class.getSimpleName());
    protected static final FrontendIdentifier FRONTEND_ID = FrontendIdentifier.create(MEMBER_NAME, FRONTEND_TYPE);
    protected static final ClientIdentifier CLIENT_ID = ClientIdentifier.create(FRONTEND_ID, 0);
    protected static final String PERSISTENCE_ID = "per-1";

    protected T connection;
    protected ClientActorContext context;
    protected ActorSystem system;
    protected TestProbe backendProbe;
    protected TestProbe contextProbe;
    protected TestProbe replyToProbe;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        system = ActorSystem.apply();
        backendProbe = new TestProbe(system);
        contextProbe = new TestProbe(system);
        context = new ClientActorContext(contextProbe.ref(), system.scheduler(), system.dispatcher(),
                PERSISTENCE_ID, CLIENT_ID);
        replyToProbe = new TestProbe(system);
        connection = createConnection();
    }

    protected abstract T createConnection();

    @Test
    public void testLocalActor() throws Exception {
        Assert.assertEquals(contextProbe.ref(), connection.localActor());
    }

    @Test
    public abstract void testReconnectConnection() throws Exception;

    @Test
    public void testPoison() throws Exception {
        final Consumer<Response<?, ?>> callback = mock(Consumer.class);
        final Request<?, ?> request = createRequest(replyToProbe.ref());
        final ConnectionEntry entry = new ConnectionEntry(request, callback, 0L);
        connection.enqueueEntry(entry, 0L);
        connection.poison(new RuntimeRequestException("fail", new RuntimeException("fail")));
        verify(callback, timeout(1000)).accept(isA(TransactionFailure.class));
    }

    @Test
    public void testSendRequestReceiveResponse() throws Exception {
        final Consumer<Response<?, ?>> callback = mock(Consumer.class);
        final Request<?, ?> request = createRequest(replyToProbe.ref());
        connection.sendRequest(request, callback);
        final RequestEnvelope requestEnvelope = backendProbe.expectMsgClass(RequestEnvelope.class);
        Assert.assertEquals(request, requestEnvelope.getMessage());
        final LocalHistoryIdentifier historyId = new LocalHistoryIdentifier(CLIENT_ID, 0L);
        final RequestSuccess<?, ?> message = new TransactionAbortSuccess(new TransactionIdentifier(historyId, 0L), 0L);
        final ResponseEnvelope<?> envelope = new SuccessEnvelope(message, 0L, 0L, 0L);
        connection.receiveResponse(envelope);
        verify(callback, timeout(1000)).accept(isA(TransactionAbortSuccess.class));
    }

    @Test
    public void testRun() throws Exception {
        final ClientActorBehavior<U> behavior = mock(ClientActorBehavior.class);
        Assert.assertSame(behavior, connection.runTimer(behavior));
    }

    @Test
    public void testCheckTimeoutEmptyQueue() throws Exception {
        final Optional<Long> timeout = connection.checkTimeout(context.ticker().read());
        Assert.assertFalse(timeout.isPresent());
    }

    @Test
    public void testCheckTimeout() throws Exception {
        final Consumer<Response<?, ?>> callback = mock(Consumer.class);
        connection.sendRequest(createRequest(replyToProbe.ref()), callback);
        final long now = context.ticker().read();
        final Optional<Long> timeout = connection.checkTimeout(now);
        Assert.assertTrue(timeout.isPresent());
    }

    @Test
    public void testReplay() throws Exception {
        final Consumer<Response<?, ?>> callback = mock(Consumer.class);
        final Request<?, ?> request1 = createRequest(replyToProbe.ref());
        final Request<?, ?> request2 = createRequest(replyToProbe.ref());
        connection.sendRequest(request1, callback);
        connection.sendRequest(request2, callback);
        final Iterable<ConnectionEntry> entries = connection.startReplay();
        Assert.assertThat(entries, hasItems(entryWithRequest(request1), entryWithRequest(request2)));
        Assert.assertEquals(2, Iterables.size(entries));
        Iterables.removeIf(entries, e -> true);
        final ReconnectForwarder forwarder = mock(ReconnectForwarder.class);
        connection.finishReplay(forwarder);
    }

    @After
    public void tearDown() throws Exception {
        JavaTestKit.shutdownActorSystem(system);
    }

    protected Request<?, ?> createRequest(final ActorRef replyTo) {
        final TransactionIdentifier identifier =
                new TransactionIdentifier(new LocalHistoryIdentifier(CLIENT_ID, 0L), 0L);
        return new AbortLocalTransactionRequest(identifier, replyTo);
    }

}