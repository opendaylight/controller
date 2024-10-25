/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.testkit.TestActorRef;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.client.AccessClientUtil;
import org.opendaylight.controller.cluster.access.client.ConnectedClientConnection;
import org.opendaylight.controller.cluster.access.commands.ConnectClientSuccess;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.access.concepts.ResponseEnvelope;

final class ShardTestConnection {
    private static final Duration DEFAULT_MAX = Duration.ofSeconds(5);

    private final ConnectedClientConnection<@NonNull ShardTestBackendInfo> connection;
    private final ShardTestKit testKit;

    ShardTestConnection(final ShardTestKit testKit, final TestActorRef<Shard> shard, final ClientIdentifier clientId,
            final ConnectClientSuccess connect) {
        this.testKit = requireNonNull(testKit);
        connection = AccessClientUtil.createConnectedConnection(
            AccessClientUtil.createClientActorContext(testKit.getSystem(), self(), connect.getTarget(),
                clientId.toString()),
            0L, new ShardTestBackendInfo(shard));
    }

    <T extends Response<?, ?>> @NonNull T request(final Class<T> clazz, final Function<ActorRef, Request<?, ?>> req) {
        return request(clazz, req, DEFAULT_MAX);
    }

    <T extends Response<?, ?>> @NonNull T request(final Class<T> clazz, final Function<ActorRef, Request<?, ?>> req,
            final Duration max) {
        sendRequest(req.apply(self()), ignored -> {
            // No-op
        });
        return assertResponse(clazz, max);
    }

    void asyncRequest(final Function<ActorRef, Request<?, ?>> req) {
        sendRequest(req.apply(self()), ignored -> {
            // No-op, pick up from testkit
        });
    }

    <T extends Response<?, ?>> @NonNull T assertResponse(final Class<T> clazz) {
        return assertResponse(clazz, DEFAULT_MAX);
    }

    <T extends Response<?, ?>> @NonNull T assertResponse(final Class<T> clazz, final Duration max) {
        return assertInstanceOf(clazz, testKit.expectMsgClass(max, ResponseEnvelope.class).getMessage());
    }

    private void sendRequest(final Request<?, ?> reqest, final Consumer<Response<?, ?>> callback) {
        connection.sendRequest(reqest, callback);
    }

    private ActorRef self() {
        return testKit.getRef();
    }
}
