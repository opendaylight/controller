/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.testkit.TestProbe;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.client.AbstractClientConnection;
import org.opendaylight.controller.cluster.access.client.AccessClientUtil;
import org.opendaylight.controller.cluster.access.commands.TransactionFailure;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.concepts.FailureEnvelope;
import org.opendaylight.controller.cluster.access.concepts.RequestEnvelope;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.RequestFailure;
import org.opendaylight.controller.cluster.access.concepts.RequestSuccess;
import org.opendaylight.controller.cluster.access.concepts.SuccessEnvelope;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

/**
 * Helper class. Allows checking messages received by backend and respond to them.
 */
final class TransactionTester<T extends AbstractProxyTransaction> {
    private final T transaction;
    private final AbstractClientConnection<ShardBackendInfo> connection;
    private final TestProbe backendProbe;

    private RequestEnvelope envelope;

    TransactionTester(final T transaction, final AbstractClientConnection<ShardBackendInfo> connection,
            final TestProbe backendProbe) {
        this.transaction = transaction;
        this.connection = connection;
        this.backendProbe = backendProbe;
    }

    ActorRef localActor() {
        return connection.localActor();
    }

    T getTransaction() {
        return transaction;
    }

    TransactionRequest<?> getLastReceivedMessage() {
        return assertInstanceOf(TransactionRequest.class, envelope.getMessage());
    }

    <R extends TransactionRequest<R>> R expectTransactionRequest(final Class<R> expected) {
        envelope = backendProbe.expectMsgClass(RequestEnvelope.class);
        return assertInstanceOf(expected, envelope.getMessage());
    }

    void replySuccess(final RequestSuccess<?, ?> success) {
        AccessClientUtil.completeRequest(connection,
            new SuccessEnvelope(success, envelope.getSessionId(), envelope.getTxSequence(), 0));
    }

    void replyFailure(final RequestException cause) {
        AccessClientUtil.completeRequest(connection, new FailureEnvelope(
            new MockFailure(transaction.getIdentifier(), envelope.getMessage().getSequence(), cause),
            envelope.getSessionId(), envelope.getTxSequence(), 0));
    }

    private static class MockFailure extends RequestFailure<TransactionIdentifier, TransactionFailure> {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        @NonNullByDefault
        MockFailure(final TransactionIdentifier target, final long sequence, final RequestException cause) {
            super(target, sequence, cause);
        }

        @Override
        protected TransactionFailure cloneAsVersion(final ABIVersion targetVersion) {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        protected SerialForm<TransactionIdentifier, TransactionFailure> externalizableProxy(final ABIVersion version) {
            throw new UnsupportedOperationException("Not implemented");
        }
    }
}
