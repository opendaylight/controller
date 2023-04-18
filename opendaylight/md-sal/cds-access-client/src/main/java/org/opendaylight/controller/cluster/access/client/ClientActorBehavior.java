/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Stopwatch;
import com.google.common.base.Verify;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.checkerframework.checker.lock.qual.Holding;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.access.commands.NotLeaderException;
import org.opendaylight.controller.cluster.access.commands.OutOfSequenceEnvelopeException;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FailureEnvelope;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.RequestFailure;
import org.opendaylight.controller.cluster.access.concepts.ResponseEnvelope;
import org.opendaylight.controller.cluster.access.concepts.RetiredGenerationException;
import org.opendaylight.controller.cluster.access.concepts.RuntimeRequestException;
import org.opendaylight.controller.cluster.access.concepts.SuccessEnvelope;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.common.actor.Dispatchers.DispatcherType;
import org.opendaylight.controller.cluster.io.FileBackedOutputStreamFactory;
import org.opendaylight.controller.cluster.messaging.MessageAssembler;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.concepts.Identifier;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;

/**
 * A behavior, which handles messages sent to a {@link AbstractClientActor}.
 */
public abstract class ClientActorBehavior<T extends BackendInfo> extends
        RecoveredClientActorBehavior<ClientActorContext> implements Identifiable<ClientIdentifier> {
    /**
     * Connection reconnect cohort, driven by this class.
     */
    @FunctionalInterface
    protected interface ConnectionConnectCohort {
        /**
         * Finish the connection by replaying previous messages onto the new connection.
         *
         * @param enqueuedEntries Previously-enqueued entries
         * @return A {@link ReconnectForwarder} to handle any straggler messages which arrive after this method returns.
         */
        @NonNull ReconnectForwarder finishReconnect(@NonNull Collection<ConnectionEntry> enqueuedEntries);
    }

    private static final Logger LOG = LoggerFactory.getLogger(ClientActorBehavior.class);
    private static final FiniteDuration RESOLVE_RETRY_DURATION = FiniteDuration.apply(1, TimeUnit.SECONDS);

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
    private final MessageAssembler responseMessageAssembler;
    private final Registration staleBackendInfoReg;

    protected ClientActorBehavior(final @NonNull ClientActorContext context,
            final @NonNull BackendInfoResolver<T> resolver) {
        super(context);
        this.resolver = requireNonNull(resolver);

        final ClientActorConfig config = context.config();
        responseMessageAssembler = MessageAssembler.builder().logContext(persistenceId())
                .fileBackedStreamFactory(new FileBackedOutputStreamFactory(config.getFileBackedStreamingThreshold(),
                        config.getTempFileDirectory()))
                .assembledMessageCallback((message, sender) -> context.self().tell(message, sender)).build();

        staleBackendInfoReg = resolver.notifyWhenBackendInfoIsStale(shard -> {
            context().executeInActor(behavior -> {
                LOG.debug("BackendInfo for shard {} is now stale", shard);
                final AbstractClientConnection<T> conn = connections.get(shard);
                if (conn instanceof ConnectedClientConnection) {
                    conn.reconnect(this, new BackendStaleException(shard));
                }
                return behavior;
            });
        });
    }

    @Override
    public final ClientIdentifier getIdentifier() {
        return context().getIdentifier();
    }

    @Override
    public void close() {
        super.close();
        responseMessageAssembler.close();
        staleBackendInfoReg.close();
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

    private AbstractClientConnection<T> getConnection(final ResponseEnvelope<?> response) {
        // Always called from actor context: no locking required
        return connections.get(extractCookie(response.getMessage().getTarget()));
    }

    @SuppressWarnings("unchecked")
    @Override
    final ClientActorBehavior<T> onReceiveCommand(final Object command) {
        if (command instanceof InternalCommand) {
            return ((InternalCommand<T>) command).execute(this);
        }

        if (command instanceof SuccessEnvelope successEnvelope) {
            return onRequestSuccess(successEnvelope);
        }
        if (command instanceof FailureEnvelope failureEnvelope) {
            return internalOnRequestFailure(failureEnvelope);
        }

        if (MessageAssembler.isHandledMessage(command)) {
            context().dispatchers().getDispatcher(DispatcherType.Serialization).execute(
                () -> responseMessageAssembler.handleMessage(command, context().self()));
            return this;
        }

        if (context().messageSlicer().handleMessage(command)) {
            return this;
        }

        return onCommand(command);
    }

    private static long extractCookie(final Identifier id) {
        if (id instanceof TransactionIdentifier transactionId) {
            return transactionId.getHistoryId().getCookie();
        } else if (id instanceof LocalHistoryIdentifier historyId) {
            return historyId.getCookie();
        } else {
            throw new IllegalArgumentException("Unhandled identifier " + id);
        }
    }

    private void onResponse(final ResponseEnvelope<?> response) {
        final AbstractClientConnection<T> connection = getConnection(response);
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
        final AbstractClientConnection<T> conn = getConnection(command);
        if (conn != null) {
            /*
             * We are talking to multiple actors, which may be lagging behind our state significantly. This has
             * the effect that we may be receiving responses from a previous connection after we have created a new
             * one to a different actor.
             *
             * Since we are already replaying requests to the new actor, we want to ignore errors reported on the old
             * connection -- for example NotLeaderException, which must not cause a new reconnect. Check the envelope's
             * sessionId and if it does not match our current connection just ignore it.
             */
            final Optional<T> optBackend = conn.getBackendInfo();
            if (optBackend.isPresent() && optBackend.orElseThrow().getSessionId() != command.getSessionId()) {
                LOG.debug("{}: Mismatched current connection {} and envelope {}, ignoring response", persistenceId(),
                    conn, command);
                return this;
            }
        }

        final RequestFailure<?, ?> failure = command.getMessage();
        final RequestException cause = failure.getCause();
        if (cause instanceof RetiredGenerationException) {
            LOG.error("{}: current generation {} has been superseded", persistenceId(), getIdentifier(), cause);
            haltClient(cause);
            poison(cause);
            return null;
        }
        if (cause instanceof NotLeaderException) {
            if (conn instanceof ReconnectingClientConnection) {
                // Already reconnecting, do not churn the logs
                return this;
            } else if (conn != null) {
                LOG.info("{}: connection {} indicated no leadership, reconnecting it", persistenceId(), conn, cause);
                return conn.reconnect(this, cause);
            }
        }
        if (cause instanceof OutOfSequenceEnvelopeException) {
            if (conn instanceof ReconnectingClientConnection) {
                // Already reconnecting, do not churn the logs
                return this;
            } else if (conn != null) {
                LOG.info("{}: connection {} indicated sequencing mismatch on {} sequence {} ({}), reconnecting it",
                    persistenceId(), conn, failure.getTarget(), failure.getSequence(), command.getTxSequence(), cause);
                return conn.reconnect(this, cause);
            }
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

        context().messageSlicer().close();
    }

    /**
     * Halt And Catch Fire. Halt processing on this client. Implementations need to ensure they initiate state flush
     * procedures. No attempt to use this instance should be made after this method returns. Any such use may result
     * in undefined behavior.
     *
     * @param cause Failure cause
     */
    protected abstract void haltClient(@NonNull Throwable cause);

    /**
     * Override this method to handle any command which is not handled by the base behavior.
     *
     * @param command the command to process
     * @return Next behavior to use, null if this actor should shut down.
     */
    protected abstract @Nullable ClientActorBehavior<T> onCommand(@NonNull Object command);

    /**
     * Override this method to provide a backend resolver instance.
     *
     * @return a backend resolver instance
     */
    protected final @NonNull BackendInfoResolver<T> resolver() {
        return resolver;
    }

    /**
     * Callback invoked when a new connection has been established. Implementations are expected perform preparatory
     * tasks before the previous connection is frozen.
     *
     * @param newConn New connection
     * @return ConnectionConnectCohort which will be used to complete the process of bringing the connection up.
     */
    @Holding("connectionsLock")
    protected abstract @NonNull ConnectionConnectCohort connectionUp(@NonNull ConnectedClientConnection<T> newConn);

    private void backendConnectFinished(final Long shard, final AbstractClientConnection<T> oldConn,
            final T backend, final Throwable failure) {
        if (failure != null) {
            if (failure instanceof TimeoutException) {
                if (!oldConn.equals(connections.get(shard))) {
                    // AbstractClientConnection will remove itself when it decides there is no point in continuing,
                    // at which point we want to stop retrying
                    LOG.info("{}: stopping resolution of shard {} on stale connection {}", persistenceId(), shard,
                        oldConn, failure);
                    return;
                }

                LOG.debug("{}: timed out resolving shard {}, scheduling retry in {}", persistenceId(), shard,
                    RESOLVE_RETRY_DURATION, failure);
                context().executeInActor(b -> {
                    resolveConnection(shard, oldConn);
                    return b;
                }, RESOLVE_RETRY_DURATION);
                return;
            }

            LOG.error("{}: failed to resolve shard {}", persistenceId(), shard, failure);
            final RequestException cause;
            if (failure instanceof RequestException requestException) {
                cause = requestException;
            } else {
                cause = new RuntimeRequestException("Failed to resolve shard " + shard, failure);
            }

            oldConn.poison(cause);
            return;
        }

        LOG.info("{}: resolved shard {} to {}", persistenceId(), shard, backend);
        final long stamp = connectionsLock.writeLock();
        try {
            final Stopwatch sw = Stopwatch.createStarted();

            // Create a new connected connection
            final ConnectedClientConnection<T> newConn = new ConnectedClientConnection<>(oldConn, backend);
            LOG.info("{}: resolving connection {} to {}", persistenceId(), oldConn, newConn);

            // Start reconnecting without the old connection lock held
            final ConnectionConnectCohort cohort = Verify.verifyNotNull(connectionUp(newConn));

            // Lock the old connection and get a reference to its entries
            final Collection<ConnectionEntry> replayIterable = oldConn.startReplay();

            // Finish the connection attempt
            final ReconnectForwarder forwarder = Verify.verifyNotNull(cohort.finishReconnect(replayIterable));

            // Cancel sleep debt after entries were replayed, before new connection starts receiving.
            newConn.cancelDebt();

            // Install the forwarder, unlocking the old connection
            oldConn.finishReplay(forwarder);

            // Make sure new lookups pick up the new connection
            if (!connections.replace(shard, oldConn, newConn)) {
                final AbstractClientConnection<T> existing = connections.get(oldConn.cookie());
                LOG.warn("{}: old connection {} does not match existing {}, new connection {} in limbo",
                    persistenceId(), oldConn, existing, newConn);
            } else {
                LOG.info("{}: replaced connection {} with {} in {}", persistenceId(), oldConn, newConn, sw);
            }
        } finally {
            connectionsLock.unlockWrite(stamp);
        }
    }

    void removeConnection(final AbstractClientConnection<?> conn) {
        final long stamp = connectionsLock.writeLock();
        try {
            if (!connections.remove(conn.cookie(), conn)) {
                final AbstractClientConnection<T> existing = connections.get(conn.cookie());
                if (existing != null) {
                    LOG.warn("{}: failed to remove connection {}, as it was superseded by {}", persistenceId(), conn,
                        existing);
                } else {
                    LOG.warn("{}: failed to remove connection {}, as it was not tracked", persistenceId(), conn);
                }
            } else {
                LOG.info("{}: removed connection {}", persistenceId(), conn);
                cancelSlicing(conn.cookie());
            }
        } finally {
            connectionsLock.unlockWrite(stamp);
        }
    }

    @SuppressWarnings("unchecked")
    void reconnectConnection(final ConnectedClientConnection<?> oldConn,
            final ReconnectingClientConnection<?> newConn) {
        final ReconnectingClientConnection<T> conn = (ReconnectingClientConnection<T>)newConn;
        LOG.info("{}: connection {} reconnecting as {}", persistenceId(), oldConn, newConn);

        final long stamp = connectionsLock.writeLock();
        try {
            final boolean replaced = connections.replace(oldConn.cookie(), (AbstractClientConnection<T>)oldConn, conn);
            if (!replaced) {
                final AbstractClientConnection<T> existing = connections.get(oldConn.cookie());
                if (existing != null) {
                    LOG.warn("{}: failed to replace connection {}, as it was superseded by {}", persistenceId(), conn,
                        existing);
                } else {
                    LOG.warn("{}: failed to replace connection {}, as it was not tracked", persistenceId(), conn);
                }
            } else {
                cancelSlicing(oldConn.cookie());
            }
        } finally {
            connectionsLock.unlockWrite(stamp);
        }

        final Long shard = oldConn.cookie();
        LOG.info("{}: refreshing backend for shard {}", persistenceId(), shard);
        resolver().refreshBackendInfo(shard, conn.getBackendInfo().orElseThrow()).whenComplete(
            (backend, failure) -> context().executeInActor(behavior -> {
                backendConnectFinished(shard, conn, backend, failure);
                return behavior;
            }));
    }

    private void cancelSlicing(final Long cookie) {
        context().messageSlicer().cancelSlicing(id -> {
            try {
                return cookie.equals(extractCookie(id));
            } catch (IllegalArgumentException e) {
                LOG.debug("extractCookie failed while cancelling slicing for cookie {}", cookie, e);
                return false;
            }
        });
    }

    private ConnectingClientConnection<T> createConnection(final Long shard) {
        final ConnectingClientConnection<T> conn = new ConnectingClientConnection<>(context(), shard,
                resolver().resolveCookieName(shard));
        resolveConnection(shard, conn);
        return conn;
    }

    private void resolveConnection(final Long shard, final AbstractClientConnection<T> conn) {
        LOG.debug("{}: resolving shard {} connection {}", persistenceId(), shard, conn);
        resolver().getBackendInfo(shard).whenComplete((backend, failure) -> context().executeInActor(behavior -> {
            backendConnectFinished(shard, conn, backend, failure);
            return behavior;
        }));
    }

    private static class BackendStaleException extends RequestException {
        private static final long serialVersionUID = 1L;

        BackendStaleException(final Long shard) {
            super("Backend for shard " + shard + " is stale");
        }

        @Override
        public boolean isRetriable() {
            return false;
        }
    }
}
