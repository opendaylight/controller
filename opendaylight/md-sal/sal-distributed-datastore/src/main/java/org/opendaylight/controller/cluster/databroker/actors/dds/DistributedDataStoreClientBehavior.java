/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import akka.actor.ActorRef;
import akka.actor.Status;
import com.google.common.base.Throwables;
import com.google.common.base.Verify;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.opendaylight.controller.cluster.access.client.ClientActorBehavior;
import org.opendaylight.controller.cluster.access.client.ClientActorContext;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ClientActorBehavior} acting as an intermediary between the backend actors and the DistributedDataStore
 * frontend.
 *
 * This class is not visible outside of this package because it breaks the actor containment. Services provided to
 * Java world outside of actor containment are captured in {@link DistributedDataStoreClient}.
 *
 * IMPORTANT: this class breaks actor containment via methods implementing {@link DistributedDataStoreClient} contract.
 *            When touching internal state, be mindful of the execution context from which execution context, Actor
 *            or POJO, is the state being accessed or modified.
 *
 * THREAD SAFETY: this class must always be kept thread-safe, so that both the Actor System thread and the application
 *                threads can run concurrently. All state transitions must be made in a thread-safe manner. When in
 *                doubt, feel free to synchronize on this object.
 *
 * PERFORMANCE: this class lies in a performance-critical fast path. All code needs to be concise and efficient, but
 *              performance must not come at the price of correctness. Any optimizations need to be carefully analyzed
 *              for correctness and performance impact.
 *
 * TRADE-OFFS: part of the functionality runs in application threads without switching contexts, which makes it ideal
 *             for performing work and charging applications for it. That has two positive effects:
 *             - CPU usage is distributed across applications, minimizing work done in the actor thread
 *             - CPU usage provides back-pressure towards the application.
 *
 * @author Robert Varga
 */
final class DistributedDataStoreClientBehavior extends ClientActorBehavior implements DistributedDataStoreClient {
    private static final Logger LOG = LoggerFactory.getLogger(DistributedDataStoreClientBehavior.class);

    private final Map<LocalHistoryIdentifier, ClientLocalHistory> histories = new ConcurrentHashMap<>();

    /**
     * Map of connections to the backend. This map is concurrent to allow lookups, but given complex operations
     * involved in connection transitions it is protected by a {@link InversibleLock}. Write-side of the lock is taken
     * during connection transitions. Optimistic read-side of the lock is taken when new connections are introduced
     * into the map.
     *
     * The lock detects potential AB/BA deadlock scenarios and will force the reader side out by throwing
     * a {@link InversibleLockException} -- which must be propagated up, releasing locks as it propagates. The initial
     * entry point causing the the conflicting lookup must then call {@link InversibleLockException#awaitResolution()} before
     * retrying the operation.
     */
    private final Map<Long, AbstractClientConnection> connections = new ConcurrentHashMap<>();
    private final InversibleLock connectionsLock = new InversibleLock();

    private final AtomicLong nextHistoryId = new AtomicLong(1);
    private final ModuleShardBackendResolver resolver;
    private final SingleClientHistory singleHistory;

    private volatile Throwable aborted;

    DistributedDataStoreClientBehavior(final ClientActorContext context, final ActorContext actorContext) {
        super(context);
        resolver = new ModuleShardBackendResolver(context.getIdentifier(), actorContext);
        singleHistory = new SingleClientHistory(this, new LocalHistoryIdentifier(getIdentifier(), 0));
    }

    //
    //
    // Methods below are invoked from the client actor thread
    //
    //

    @Override
    protected void haltClient(final Throwable cause) {
        // If we have encountered a previous problem there is not cleanup necessary, as we have already cleaned up
        // Thread safely is not an issue, as both this method and any failures are executed from the same (client actor)
        // thread.
        if (aborted != null) {
            abortOperations(cause);
        }
    }

    private void abortOperations(final Throwable cause) {
        // This acts as a barrier, application threads check this after they have added an entry in the maps,
        // and if they observe aborted being non-null, they will perform their cleanup and not return the handle.
        aborted = cause;

        for (ClientLocalHistory h : histories.values()) {
            h.localAbort(cause);
        }
        histories.clear();
    }

    private DistributedDataStoreClientBehavior shutdown(final ClientActorBehavior currentBehavior) {
        abortOperations(new IllegalStateException("Client " + getIdentifier() + " has been shut down"));
        return null;
    }

    @Override
    protected DistributedDataStoreClientBehavior onCommand(final Object command) {
        if (command instanceof GetClientRequest) {
            ((GetClientRequest) command).getReplyTo().tell(new Status.Success(this), ActorRef.noSender());
        } else {
            LOG.warn("{}: ignoring unhandled command {}", persistenceId(), command);
        }

        return this;
    }

    private void backendConnectFinished(final Long shard, final ConnectingClientConnection conn,
            final ShardBackendInfo backend, final Throwable t) {
        if (t != null) {
            LOG.error("{}: failed to resolve shard {}", persistenceId(), shard, t);
            return;
        }

        final long stamp = connectionsLock.writeLock();
        try {
            /*
             * We are transitioning from a placeholder connection to a connected one. The placeholder operates just
             * like a remote connection, hence we discern the two connection ups.
             */
            if (!backend.getDataTree().isPresent()) {
                remoteConnectionUp(shard, conn, backend);
            } else {
                localConnectionUp(shard, conn, backend);
            }
        } finally {
            connectionsLock.unlock(stamp);
        }
    }

    /*
     * The connection has resolved to a local node, which means we have to perform the remote-to-local transition.
     * This is a bit more involved, as the messages need to be replayed to the individual proxies.
     *
     *
     *
     */
    private void localConnectionUp(final Long shard, final ConnectingClientConnection conn,
            final ShardBackendInfo backend) {

        //final ConnectedClientConnection newConn = conn.toLocalConnected(backend, input -> replayMe);



    }

    /*
     * The connection has resolved to a remote node, which is essentially a no-op, except we need to replace connection
     * and splice the queued messages onto it.
     */
    private void remoteConnectionUp(final Long shard, final ConnectingClientConnection conn,
            final ShardBackendInfo backend) {
        final ConnectedClientConnection newConn = conn.toRemoteConnected(backend);

        // Make sure new objects pick up the new connection
        connections.replace(shard, conn, newConn);

        // Propagate the connection through all history proxies
        singleHistory.updateConnection(shard, newConn);
        for (ClientLocalHistory h : histories.values()) {
            h.updateConnection(shard, newConn);
        }
    }

    //
    //
    // Methods below are invoked from application threads
    //
    //

    private static <K, V extends LocalAbortable> V returnIfOperational(final Map<K , V> map, final K key, final V value,
            final Throwable aborted) {
        Verify.verify(map.put(key, value) == null);

        if (aborted != null) {
            try {
                value.localAbort(aborted);
            } catch (Exception e) {
                LOG.debug("Close of {} failed", value, e);
            }
            map.remove(key, value);
            throw Throwables.propagate(aborted);
        }

        return value;
    }

    @Override
    public ClientLocalHistory createLocalHistory() {
        final LocalHistoryIdentifier historyId = new LocalHistoryIdentifier(getIdentifier(),
            nextHistoryId.getAndIncrement());
        final ClientLocalHistory history = new ClientLocalHistory(this, historyId);
        LOG.debug("{}: creating a new local history {}", persistenceId(), history);

        return returnIfOperational(histories, historyId, history, aborted);
    }

    @Override
    public ClientTransaction createTransaction() {
        return singleHistory.createTransaction();
    }

    @Override
    public void close() {
        context().executeInActor(this::shutdown);
    }

    @Override
    protected ModuleShardBackendResolver resolver() {
        return resolver;
    }

    private ConnectingClientConnection createConnection(final Long shard) {
        final ConnectingClientConnection conn = new ConnectingClientConnection(context());

        resolver.getBackendInfo(shard).whenComplete((t, u) -> context().executeInActor(behavior -> {
            backendConnectFinished(shard, conn, t, u);
            return behavior;
        }));

        return conn;
    }

    /**
     *
     * @param shard
     * @return
     * @throws InversibleLockException
     */
    AbstractClientConnection getConnection(final Long shard) {
        while (true) {
            final long stamp = connectionsLock.optimisticRead();
            final AbstractClientConnection conn = connections.computeIfAbsent(shard, this::createConnection);
            if (connectionsLock.validate(stamp)) {
                // No write-lock in-between, return success
                return conn;
            }
        }
    }
}
