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
import java.util.concurrent.locks.StampedLock;
import org.opendaylight.controller.cluster.access.client.BackendInfoResolver;
import org.opendaylight.controller.cluster.access.client.ClientActorBehavior;
import org.opendaylight.controller.cluster.access.client.ClientActorContext;
import org.opendaylight.controller.cluster.access.client.ConnectedClientConnection;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ClientActorBehavior} acting as an intermediary between the backend actors and the DistributedDataStore
 * frontend.
 *
 * <p>
 * This class is not visible outside of this package because it breaks the actor containment. Services provided to
 * Java world outside of actor containment are captured in {@link DataStoreClient}.
 *
 * <p>
 * IMPORTANT: this class breaks actor containment via methods implementing {@link DataStoreClient} contract.
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
abstract class AbstractDataStoreClientBehavior extends ClientActorBehavior<ShardBackendInfo>
        implements DataStoreClient {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDataStoreClientBehavior.class);

    private final Map<LocalHistoryIdentifier, ClientLocalHistory> histories = new ConcurrentHashMap<>();
    private final AtomicLong nextHistoryId = new AtomicLong(1);
    private final StampedLock lock = new StampedLock();
    private final SingleClientHistory singleHistory;

    private volatile Throwable aborted;

    AbstractDataStoreClientBehavior(final ClientActorContext context,
            final BackendInfoResolver<ShardBackendInfo> resolver) {
        super(context, resolver);
        singleHistory = new SingleClientHistory(this, new LocalHistoryIdentifier(getIdentifier(), 0));
    }

    //
    //
    // Methods below are invoked from the client actor thread
    //
    //

    @Override
    protected final void haltClient(final Throwable cause) {
        // If we have encountered a previous problem there is no cleanup necessary, as we have already cleaned up
        // Thread safely is not an issue, as both this method and any failures are executed from the same (client actor)
        // thread.
        if (aborted != null) {
            abortOperations(cause);
        }
    }

    private void abortOperations(final Throwable cause) {
        final long stamp = lock.writeLock();
        try {
            // This acts as a barrier, application threads check this after they have added an entry in the maps,
            // and if they observe aborted being non-null, they will perform their cleanup and not return the handle.
            aborted = cause;

            for (ClientLocalHistory h : histories.values()) {
                h.localAbort(cause);
            }
            histories.clear();
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    private AbstractDataStoreClientBehavior shutdown(final ClientActorBehavior<ShardBackendInfo> currentBehavior) {
        abortOperations(new IllegalStateException("Client " + getIdentifier() + " has been shut down"));
        return null;
    }

    @Override
    protected final AbstractDataStoreClientBehavior onCommand(final Object command) {
        if (command instanceof GetClientRequest) {
            ((GetClientRequest) command).getReplyTo().tell(new Status.Success(this), ActorRef.noSender());
        } else {
            LOG.warn("{}: ignoring unhandled command {}", persistenceId(), command);
        }

        return this;
    }

    /*
     * The connection has resolved, which means we have to potentially perform message adaptation. This is a bit more
     * involved, as the messages need to be replayed to the individual proxies.
     */
    @Override
    protected final ConnectionConnectCohort connectionUp(final ConnectedClientConnection<ShardBackendInfo> newConn) {
        final long stamp = lock.writeLock();

        // Step 1: Freeze all AbstractProxyHistory instances pointing to that shard. This indirectly means that no
        //         further TransactionProxies can be created and we can safely traverse maps without risking
        //         missing an entry
        final Collection<HistoryReconnectCohort> cohorts = new ArrayList<>();
        startReconnect(singleHistory, newConn, cohorts);
        for (ClientLocalHistory h : histories.values()) {
            startReconnect(h, newConn, cohorts);
        }

        return previousEntries -> {
            try {
                // Step 2: Collect previous successful requests from the cohorts. We do not want to expose
                //         the non-throttling interface to the connection, hence we use a wrapper consumer
                for (HistoryReconnectCohort c : cohorts) {
                    c.replayRequests(previousEntries);
                }

                // Step 3: Install a forwarder, which will forward requests back to affected cohorts. Any outstanding
                //         requests will be immediately sent to it and requests being sent concurrently will get
                //         forwarded once they hit the new connection.
                return BouncingReconnectForwarder.forCohorts(newConn, cohorts);
            } finally {
                try {
                    // Step 4: Complete switchover of the connection. The cohorts can resume normal operations.
                    for (HistoryReconnectCohort c : cohorts) {
                        c.close();
                    }
                } finally {
                    lock.unlockWrite(stamp);
                }
            }
        };
    }

    private static void startReconnect(final AbstractClientHistory history,
            final ConnectedClientConnection<ShardBackendInfo> newConn,
            final Collection<HistoryReconnectCohort> cohorts) {
        final HistoryReconnectCohort cohort = history.startReconnect(newConn);
        if (cohort != null) {
            cohorts.add(cohort);
        }
    }

    //
    //
    // Methods below are invoked from application threads
    //
    //

    @Override
    public final ClientLocalHistory createLocalHistory() {
        final LocalHistoryIdentifier historyId = new LocalHistoryIdentifier(getIdentifier(),
            nextHistoryId.getAndIncrement());

        final long stamp = lock.readLock();
        try {
            if (aborted != null) {
                throw Throwables.propagate(aborted);
            }

            final ClientLocalHistory history = new ClientLocalHistory(this, historyId);
            LOG.debug("{}: creating a new local history {}", persistenceId(), history);

            Verify.verify(histories.put(historyId, history) == null);
            return history;
        } finally {
            lock.unlockRead(stamp);
        }
    }

    @Override
    public final ClientTransaction createTransaction() {
        return singleHistory.createTransaction();
    }

    @Override
    public final ClientSnapshot createSnapshot() {
        return singleHistory.takeSnapshot();
    }

    @Override
    public final void close() {
        context().executeInActor(this::shutdown);
    }

    abstract Long resolveShardForPath(YangInstanceIdentifier path);
}
