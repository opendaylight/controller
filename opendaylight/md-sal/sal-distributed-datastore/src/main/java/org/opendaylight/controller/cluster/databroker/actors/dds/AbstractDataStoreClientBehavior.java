/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.base.VerifyException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.StampedLock;
import java.util.stream.Stream;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.access.client.ClientActorBehavior;
import org.opendaylight.controller.cluster.access.client.ClientActorContext;
import org.opendaylight.controller.cluster.access.client.ConnectedClientConnection;
import org.opendaylight.controller.cluster.access.client.ConnectionEntry;
import org.opendaylight.controller.cluster.access.client.ReconnectForwarder;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.raft.spi.RestrictedObjectStreams;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ClientActorBehavior} acting as an intermediary between the backend actors and the DistributedDataStore
 * frontend.
 *
 * <p>This class is not visible outside of this package because it breaks the actor containment. Services provided to
 * Java world outside of actor containment are captured in {@link DataStoreClient}.
 *
 * <p>IMPORTANT: this class breaks actor containment via methods implementing {@link DataStoreClient} contract.
 *               When touching internal state, be mindful of the execution context from which execution context, Actor
 *               or POJO, is the state being accessed or modified.
 *
 * <p>THREAD SAFETY: this class must always be kept thread-safe, so that both the Actor System thread and the
 *                   application threads can run concurrently. All state transitions must be made in a thread-safe
 *                   manner. When in doubt, feel free to synchronize on this object.
 *
 * <p>PERFORMANCE: this class lies in a performance-critical fast path. All code needs to be concise and efficient, but
 *                 performance must not come at the price of correctness. Any optimizations need to be carefully
 *                 analyzed for correctness and performance impact.
 *
 * <p>TRADE-OFFS: part of the functionality runs in application threads without switching contexts, which makes it ideal
 *                for performing work and charging applications for it. That has two positive effects:
 *                - CPU usage is distributed across applications, minimizing work done in the actor thread
 *                - CPU usage provides back-pressure towards the application.
 */
abstract class AbstractDataStoreClientBehavior extends ClientActorBehavior<ShardBackendInfo>
        implements DataStoreClient {
    private static final class Closed {
        final Instant when;
        final String who;

        Closed() {
            when = Instant.now();
            who = Thread.currentThread().getName();
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).omitNullValues().add("who", who).add("when", when).toString();
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(AbstractDataStoreClientBehavior.class);
    private static final @NonNull RestrictedObjectStreams OBJECT_STREAMS = RestrictedObjectStreams.ofClassLoaders(
        LocalHistoryIdentifier.class, ClientActorBehavior.class, AbstractDataStoreClientBehavior.class);
    private static final VarHandle ABORTED_VH;
    private static final VarHandle CLOSED_VH;
    private static final VarHandle HISTORY_ID_VH;

    static {
        final var lookup = MethodHandles.lookup();
        try {
            ABORTED_VH = lookup.findVarHandle(AbstractDataStoreClientBehavior.class, "aborted", Throwable.class);
            CLOSED_VH = lookup.findVarHandle(AbstractDataStoreClientBehavior.class, "closed", Closed.class);
            HISTORY_ID_VH = lookup.findVarHandle(AbstractDataStoreClientBehavior.class, "nextHistoryId", long.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final ConcurrentHashMap<LocalHistoryIdentifier, ClientLocalHistory> histories = new ConcurrentHashMap<>();
    private final StampedLock lock = new StampedLock();
    private final SingleClientHistory singleHistory;

    @SuppressFBWarnings(value = "URF_UNREAD_FIELD", justification = "https://github.com/spotbugs/spotbugs/issues/2749")
    private volatile long nextHistoryId = 1;
    @SuppressFBWarnings(value = "UUF_UNUSED_FIELD", justification = "https://github.com/spotbugs/spotbugs/issues/2749")
    private volatile @Nullable Throwable aborted;
    @SuppressFBWarnings(value = "UUF_UNUSED_FIELD", justification = "https://github.com/spotbugs/spotbugs/issues/2749")
    private volatile @Nullable Closed closed;

    @NonNullByDefault
    AbstractDataStoreClientBehavior(final ClientActorContext context, final AbstractShardBackendResolver resolver) {
        super(context, resolver, OBJECT_STREAMS);
        singleHistory = new SingleClientHistory(this, new LocalHistoryIdentifier(getIdentifier(), 0));
    }

    @VisibleForTesting
    final @Nullable Throwable aborted() {
        return (Throwable) ABORTED_VH.getAcquire(this);
    }

    @VisibleForTesting
    final long nextHistoryId() {
        return (long) HISTORY_ID_VH.getAndAdd(this, 1);
    }

    //
    //
    // Methods below are invoked from the client actor thread
    //
    //

    @Override
    protected final void haltClient(final Throwable cause) {
        requireNonNull(cause);

        // If we have encountered a previous problem there is no cleanup necessary, as we have already cleaned up
        // Thread safely is not an issue, as both this method and any failures are executed from the same (client actor)
        // thread.
        if (aborted() == null) {
            abortOperations(cause);
        }
    }

    @NonNullByDefault
    private void abortOperations(final Throwable cause) {
        final long stamp = lock.writeLock();
        try {
            lockedAbortOperations(cause);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @NonNullByDefault
    private void lockedAbortOperations(final Throwable cause) {
        // This acts as a barrier, application threads check this after they have added an entry in the maps,
        // and if they observe aborted being non-null, they will perform their cleanup and not return the handle.
        final var witness = (Throwable) ABORTED_VH.compareAndExchange(this, null, cause);
        if (witness != null) {
            LOG.debug("{}: already aborted", persistenceId(), LOG.isTraceEnabled() ? witness : null);
            return;
        }

        for (var history : histories.values()) {
            history.localAbort(cause);
        }
        histories.clear();
    }

    @Override
    protected final AbstractDataStoreClientBehavior onCommand(final Object command) {
        if (command instanceof GetClientRequest request) {
            request.getReplyTo().tell(new Status.Success(this), ActorRef.noSender());
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
        final var cohorts = new ArrayList<HistoryReconnectCohort>();
        startReconnect(singleHistory, newConn, cohorts);
        for (var history : histories.values()) {
            startReconnect(history, newConn, cohorts);
        }

        return previousEntries -> finishReconnect(newConn, stamp, cohorts, previousEntries);
    }

    private @NonNull ReconnectForwarder finishReconnect(final ConnectedClientConnection<ShardBackendInfo> newConn,
            final long stamp, final Collection<HistoryReconnectCohort> cohorts,
            final Collection<ConnectionEntry> previousEntries) {
        try {
            // Step 2: Collect previous successful requests from the cohorts. We do not want to expose
            //         the non-throttling interface to the connection, hence we use a wrapper consumer
            for (var cohort : cohorts) {
                cohort.replayRequests(previousEntries);
            }

            // Step 3: Install a forwarder, which will forward requests back to affected cohorts. Any outstanding
            //         requests will be immediately sent to it and requests being sent concurrently will get
            //         forwarded once they hit the new connection.
            return BouncingReconnectForwarder.forCohorts(newConn, cohorts);
        } finally {
            try {
                // Step 4: Complete switchover of the connection. The cohorts can resume normal operations.
                for (var cohort : cohorts) {
                    cohort.close();
                }
            } finally {
                lock.unlockWrite(stamp);
            }
        }
    }

    private static void startReconnect(final AbstractClientHistory history,
            final ConnectedClientConnection<ShardBackendInfo> newConn,
            final Collection<HistoryReconnectCohort> cohorts) {
        final var cohort = history.startReconnect(newConn);
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
        final var historyId = new LocalHistoryIdentifier(getIdentifier(), nextHistoryId());
        final long stamp = lock.readLock();
        try {
            return lockedCreateLocalHistory(historyId);
        } finally {
            lock.unlockRead(stamp);
        }
    }

    @NonNullByDefault
    private ClientLocalHistory lockedCreateLocalHistory(final LocalHistoryIdentifier historyId) {
        final var ex = aborted();
        if (ex != null) {
            Throwables.throwIfUnchecked(ex);
            throw new IllegalStateException(ex);
        }

        final var history = new ClientLocalHistory(this, historyId);
        LOG.debug("{}: creating a new local history {}", persistenceId(), history);

        final var prev = histories.putIfAbsent(historyId, history);
        if (prev != null) {
            throw new VerifyException("Attempted to replace " + prev + " with " + history);
        }
        return history;
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
    public void close() {
        final var witness = (Closed) CLOSED_VH.compareAndExchange(this, null, new Closed());
        if (witness != null) {
            LOG.debug("{}: already closed by {} at {}", persistenceId(), witness.who, witness.when);
            return;
        }

        LOG.debug("{}: closing", persistenceId());
        final var sw = Stopwatch.createStarted();
        context().executeInActor(currentBehavior -> {
            LOG.debug("{}: resumed close after {}", persistenceId(), sw);
            terminate();
            return null;
        });
    }

    abstract Long resolveShardForPath(YangInstanceIdentifier path);

    abstract Stream<Long> resolveAllShards();

    final ActorUtils actorUtils() {
        return ((AbstractShardBackendResolver) resolver()).actorUtils();
    }
}
