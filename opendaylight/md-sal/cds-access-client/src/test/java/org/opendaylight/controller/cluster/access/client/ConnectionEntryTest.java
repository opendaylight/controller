/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.TestProbe;
import com.google.common.testing.FakeTicker;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.AbstractRequestFailureProxy;
import org.opendaylight.controller.cluster.access.concepts.AbstractRequestProxy;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.RequestFailure;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.yangtools.concepts.WritableIdentifier;

/**
 * Test suite covering logic contained in {@link ConnectionEntry}.
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class ConnectionEntryTest {
    private static class MockFailure extends RequestFailure<WritableIdentifier, MockFailure> {
        private static final long serialVersionUID = 1L;

        MockFailure(final WritableIdentifier target, final RequestException cause) {
            super(target, 0, cause);
        }

        @Override
        protected AbstractRequestFailureProxy<WritableIdentifier, MockFailure> externalizableProxy(
                final ABIVersion version) {
            return null;
        }

        @Override
        protected MockFailure cloneAsVersion(final ABIVersion version) {
            return this;
        }
    }

    private static class MockRequest extends Request<WritableIdentifier, MockRequest> {
        private static final long serialVersionUID = 1L;

        MockRequest(final WritableIdentifier target, final ActorRef replyTo) {
            super(target, 0, replyTo);
        }

        @Override
        public RequestFailure<WritableIdentifier, ?> toRequestFailure(final RequestException cause) {
            return new MockFailure(getTarget(), cause);
        }

        @Override
        protected AbstractRequestProxy<WritableIdentifier, MockRequest> externalizableProxy(final ABIVersion version) {
            return null;
        }

        @Override
        protected MockRequest cloneAsVersion(final ABIVersion version) {
            return this;
        }
    }

    @Mock
    private ActorRef mockReplyTo;
    @Mock
    private WritableIdentifier mockIdentifier;
    @Mock
    private RequestException mockCause;
    @Mock
    private Consumer<Response<?, ?>> mockCallback;
    @Mock
    private ClientActorBehavior<?> mockBehavior;

    private FakeTicker ticker;
    private Request<WritableIdentifier, ?> mockRequest;
    private Response<WritableIdentifier, ?> mockResponse;

    private static ActorSystem actorSystem;
    private TestProbe mockActor;

    private ConnectionEntry entry;

    @BeforeClass
    public static void setupClass() {
        actorSystem = ActorSystem.apply();
    }

    @AfterClass
    public static void teardownClass() {
        actorSystem.terminate();
    }

    @Before
    public void setup() {
        doNothing().when(mockCallback).accept(any(MockFailure.class));

        ticker = new FakeTicker();
        ticker.advance(ThreadLocalRandom.current().nextLong());

        mockActor = TestProbe.apply(actorSystem);
        mockRequest = new MockRequest(mockIdentifier, mockReplyTo);
        mockResponse = mockRequest.toRequestFailure(mockCause);

        entry = new ConnectionEntry(mockRequest, mockCallback, ticker.read());
    }

    @After
    public void teardown() {
        actorSystem.stop(mockActor.ref());
    }

    @Test
    public void testComplete() {
        entry.complete(mockResponse);
        verify(mockCallback).accept(mockResponse);
    }
}
