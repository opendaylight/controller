/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FailureEnvelope;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.RequestFailure;
import org.opendaylight.controller.cluster.access.concepts.ResponseEnvelope;
import org.opendaylight.controller.cluster.access.concepts.RetiredGenerationException;
import org.opendaylight.controller.cluster.access.concepts.SuccessEnvelope;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.concepts.WritableIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A behavior, which handles messages sent to a {@link AbstractClientActor}.
 *
 * @author Robert Varga
 */
@Beta
public abstract class ClientActorBehavior<T extends BackendInfo> extends
        RecoveredClientActorBehavior<ClientActorContext> implements Identifiable<ClientIdentifier> {
    private static final Logger LOG = LoggerFactory.getLogger(ClientActorBehavior.class);

    /**
     * Map of connections to the backend. This map is concurrent to allow lookups, but given complex operations
     * involved in connection transitions it is protected by a {@link InversibleLock}. Write-side of the lock is taken
     * during connection transitions. Optimistic read-side of the lock is taken when new connections are introduced
     * into the map.
     *
     * <p>
     * The lock detects potential AB/BA deadlock scenarios and will force the reader side out by throwing
     * a {@link InversibleLockException} -- which must be propagated up, releasing locks as it propagates. The initial
     * entry point causing the the conflicting lookup must then call {@link InversibleLockException#awaitResolution()}
     * before retrying the operation.
     */
    // TODO: it should be possible to move these two into ClientActorContext
    private final Map<Long, AbstractClientConnection<T>> connections = new ConcurrentHashMap<>();
    private final InversibleLock connectionsLock = new InversibleLock();
    private final BackendInfoResolver<T> resolver;

    protected ClientActorBehavior(@Nonnull final ClientActorContext context,
            @Nonnull final BackendInfoResolver<T> resolver) {
        super(context);
        this.resolver = Preconditions.checkNotNull(resolver);
    }

    @Override
    @Nonnull
    public final ClientIdentifier getIdentifier() {
        return context().getIdentifier();
    }

    /**
     * Get a connection to a shard.
     *
     * @param shard Shard cookie
     * @return Connection to a shard
     * @throws InversibleLockException if the shard is being reconnected
     */
    public final AbstractClientConnection<T> getConnection(final Long shard) {
        while (true) {
            final long stamp = connectionsLock.optimisticRead();
            final AbstractClientConnection<T> conn = connections.computeIfAbsent(shard, this::createConnection);
            if (connectionsLock.validate(stamp)) {
                // No write-lock in-between, return success
                return conn;
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    final ClientActorBehavior<T> onReceiveCommand(final Object command) {
        if (command instanceof InternalCommand) {
            return ((InternalCommand<T>) command).execute(this);
        }
        if (command instanceof SuccessEnvelope) {
            return onRequestSuccess((SuccessEnvelope) command);
        }
        if (command instanceof FailureEnvelope) {
            return internalOnRequestFailure((FailureEnvelope) command);
        }

        return onCommand(command);
    }

    private static long extractCookie(final WritableIdentifier id) {
        if (id instanceof TransactionIdentifier) {
            return ((TransactionIdentifier) id).getHistoryId().getCookie();
        } else if (id instanceof LocalHistoryIdentifier) {
            return ((LocalHistoryIdentifier) id).getCookie();
        } else {
            throw new IllegalArgumentException("Unhandled identifier " + id);
        }
    }

    private void onResponse(final ResponseEnvelope<?> response) {
        final long cookie = extractCookie(response.getMessage().getTarget());
        final AbstractClientConnection<T> connection = connections.get(cookie);
        if (connection != null) {
            connection.receiveResponse(response);
        } else {
            LOG.info("{}: Ignoring unknown response {}", persistenceId(), response);
        }
    }

    private ClientActorBehavior<T> onRequestSuccess(final SuccessEnvelope success) {
        onResponse(success);
        return this;
    }

    private ClientActorBehavior<T> onRequestFailure(final FailureEnvelope failure) {
        onResponse(failure);
        return this;
    }

    private ClientActorBehavior<T> internalOnRequestFailure(final FailureEnvelope command) {
        final RequestFailure<?, ?> failure = command.getMessage();
        final RequestException cause = failure.getCause();
        if (cause instanceof RetiredGenerationException) {
            LOG.error("{}: current generation {} has been superseded", persistenceId(), getIdentifier(), cause);
            haltClient(cause);
            poison(cause);
            return null;
        }

        return onRequestFailure(command);
    }

    private void poison(final RequestException cause) {
        final long stamp = connectionsLock.writeLock();
        try {
            for (AbstractClientConnection<T> q : connections.values()) {
                q.poison(cause);
            }

            connections.clear();
        } finally {
            connectionsLock.unlockWrite(stamp);
        }
    }

    /**
     * Halt And Catch Fire. Halt processing on this client. Implementations need to ensure they initiate state flush
     * procedures. No attempt to use this instance should be made after this method returns. Any such use may result
     * in undefined behavior.
     *
     * @param cause Failure cause
     */
    protected abstract void haltClient(@Nonnull Throwable cause);

    /**
     * Override this method to handle any command which is not handled by the base behavior.
     *
     * @param command the command to process
     * @return Next behavior to use, null if this actor should shut down.
     */
    @Nullable
    protected abstract ClientActorBehavior<T> onCommand(@Nonnull Object command);

    /**
     * Override this method to provide a backend resolver instance.
     *
     * @return a backend resolver instance
     */
    protected final @Nonnull BackendInfoResolver<T> resolver() {
        return resolver;
    }

    /**
     * Callback invoked when a new connection has been established.
     *
     * @param conn Old connection
     * @param backend New backend
     * @return Newly-connected connection.
     */
    @GuardedBy("connectionsLock")
    protected abstract @Nonnull ConnectedClientConnection<T> connectionUp(
            final @Nonnull AbstractClientConnection<T> conn, final @Nonnull T backend);

    private void backendConnectFinished(final Long shard, final AbstractClientConnection<T> conn,
            final T backend, final Throwable failure) {
        if (failure != null) {
            LOG.error("{}: failed to resolve shard {}", persistenceId(), shard, failure);
            return;
        }

        LOG.debug("{}: resolved shard {} to {}", persistenceId(), shard, backend);
        final long stamp = connectionsLock.writeLock();
        try {
            // Bring the connection up
            final ConnectedClientConnection<T> newConn = connectionUp(conn, backend);

            // Make sure new lookups pick up the new connection
            connections.replace(shard, conn, newConn);
            LOG.debug("{}: replaced connection {} with {}", persistenceId(), conn, newConn);
        } finally {
            connectionsLock.unlockWrite(stamp);
        }
    }

    void removeConnection(final AbstractClientConnection<?> conn) {
        connections.remove(conn.cookie(), conn);
        LOG.debug("{}: removed connection {}", persistenceId(), conn);
    }

    @SuppressWarnings("unchecked")
    void reconnectConnection(final ConnectedClientConnection<?> oldConn,
            final ReconnectingClientConnection<?> newConn) {
        final ReconnectingClientConnection<T> conn = (ReconnectingClientConnection<T>)newConn;
        connections.replace(oldConn.cookie(), (AbstractClientConnection<T>)oldConn, conn);
        LOG.debug("{}: connection {} reconnecting as {}", persistenceId(), oldConn, newConn);

        final Long shard = oldConn.cookie();
        resolver().refreshBackendInfo(shard, conn.getBackendInfo().get()).whenComplete(
            (backend, failure) -> context().executeInActor(behavior -> {
                backendConnectFinished(shard, conn, backend, failure);
                return behavior;
            }));
    }

    private ConnectingClientConnection<T> createConnection(final Long shard) {
        final ConnectingClientConnection<T> conn = new ConnectingClientConnection<>(context(), shard);

        resolver().getBackendInfo(shard).whenComplete((backend, failure) -> context().executeInActor(behavior -> {
            backendConnectFinished(shard, conn, backend, failure);
            return behavior;
        }));

        return conn;
    }
}
