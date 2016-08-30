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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.concurrent.GuardedBy;
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
 * <p>
 * This class is not visible outside of this package because it breaks the actor containment. Services provided to
 * Java world outside of actor containment are captured in {@link DistributedDataStoreClient}.
 *
 * <p>
 * IMPORTANT: this class breaks actor containment via methods implementing {@link DistributedDataStoreClient} contract.
 *            When touching internal state, be mindful of the execution context from which execution context, Actor
 *            or POJO, is the state being accessed or modified.
 *
 * <p>
 * THREAD SAFETY: this class must always be kept thread-safe, so that both the Actor System thread and the application
 *                threads can run concurrently. All state transitions must be made in a thread-safe manner. When in
 *                doubt, feel free to synchronize on this object.
 *
 * <p>
 * PERFORMANCE: this class lies in a performance-critical fast path. All code needs to be concise and efficient, but
 *              performance must not come at the price of correctness. Any optimizations need to be carefully analyzed
 *              for correctness and performance impact.
 *
 * <p>
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
        // If we have encountered a previous problem there is no cleanup necessary, as we have already cleaned up
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
            if (backend.getDataTree().isPresent()) {
                localConnectionUp(shard, conn, backend);
            } else {
                remoteConnectionUp(shard, conn, backend);
            }
        } finally {
            connectionsLock.unlockWrite(stamp);
        }
    }

    /*
     * The connection has resolved to a local node, which means we have to perform the remote-to-local transition.
     * This is a bit more involved, as the messages need to be replayed to the individual proxies.
     */
    @GuardedBy("connectionsLock")
    private void localConnectionUp(final Long shard, final ConnectingClientConnection conn,
            final ShardBackendInfo backend) {

        // Step 0: create a new connected connection
        final ConnectedClientConnection newConn = new ConnectedClientConnection(conn.context(), backend);

        final Collection<HistoryReconnectCohort> cohorts = new ArrayList<>();
        try {
            // Step 1: Freeze all AbstractProxyHistory instances pointing to that shard. This indirectly means that no
            //         further TransactionProxies can be created and we can safely traverse maps without risking
            //         missing an entry
            for (ClientLocalHistory h : histories.values()) {
                final HistoryReconnectCohort cohort = h.startReconnect(shard, newConn);
                if (cohort != null) {
                    cohorts.add(cohort);
                }
            }

            // Step 2: Collect previous successful requests from the cohorts. We do not want to expose
            //         the non-throttling interface to the connection, hence we use a wrapper consumer
            for (HistoryReconnectCohort c : cohorts) {
                c.replaySuccessfulRequests();
            }

            // Step 3: Install a forwarder, which will forward requests back to affected cohorts. Any outstanding
            //         requests will be immediately sent to it and requests being sent concurrently will get forwarded
            //         once they hit the new connection.
            conn.setForwarder(ReconnectForwarder.forCohorts(newConn, cohorts));
        } finally {
            // Step 4: Complete switchover of the connection. The cohorts can resume normal operations.
            for (HistoryReconnectCohort c : cohorts) {
                c.close();
            }
        }
    }

    private static void updateConnection(final AbstractClientHistory history, final Long shard,
            final ShardBackendInfo backend, final ConnectedClientConnection conn) {
        final HistoryReconnectCohort cohort = history.startReconnect(shard, conn);
        if (cohort != null) {
            cohort.replaySuccessfulRequests();
            cohort.close();
        }
    }

    /*
     * The connection has resolved to a remote node, which is essentially a no-op, except we need to replace connection
     * and splice the queued messages onto it.
     */
    @GuardedBy("connectionsLock")
    private void remoteConnectionUp(final Long shard, final ConnectingClientConnection conn,
            final ShardBackendInfo backend) {
        final ConnectedClientConnection newConn = conn.toRemoteConnected(backend);

        // Make sure new objects pick up the new connection
        connections.replace(shard, conn, newConn);

        // Propagate the connection through all history proxies
        updateConnection(singleHistory, shard, backend, newConn);
        for (ClientLocalHistory h : histories.values()) {
            updateConnection(h, shard, backend, newConn);
        }
    }

    //
    //
    // Methods below are invoked from application threads
    //
    //

    @Override
    public ClientLocalHistory createLocalHistory() {
        final LocalHistoryIdentifier historyId = new LocalHistoryIdentifier(getIdentifier(),
            nextHistoryId.getAndIncrement());
        final ClientLocalHistory history = new ClientLocalHistory(this, historyId);
        LOG.debug("{}: creating a new local history {}", persistenceId(), history);

        Verify.verify(histories.put(historyId, history) == null);

        final Throwable a = aborted;
        if (a != null) {
            try {
                history.localAbort(a);
            } catch (Exception e) {
                LOG.debug("Close of {} failed", history, e);
            }
            histories.remove(historyId, history);
            throw Throwables.propagate(a);
        }

        return history;
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
     * @throws InversibleLockException if the shard is being reconnected
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
