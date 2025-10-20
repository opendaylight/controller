/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verify;
import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.UnsignedLong;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import org.apache.pekko.actor.ActorRef;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.Holding;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.client.AbstractClientConnection;
import org.opendaylight.controller.cluster.access.client.ClientActorContext;
import org.opendaylight.controller.cluster.access.client.ConnectedClientConnection;
import org.opendaylight.controller.cluster.access.client.ConnectionEntry;
import org.opendaylight.controller.cluster.access.commands.CreateLocalHistoryRequest;
import org.opendaylight.controller.cluster.access.commands.DestroyLocalHistoryRequest;
import org.opendaylight.controller.cluster.access.commands.LocalHistoryRequest;
import org.opendaylight.controller.cluster.access.commands.PurgeLocalHistoryRequest;
import org.opendaylight.controller.cluster.access.commands.SkipTransactionsRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeSnapshot;
import org.opendaylight.yangtools.yang.data.tree.api.ReadOnlyDataTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Per-connection representation of a local history. This class handles state replication across a single connection.
 *
 * @author Robert Varga
 */
abstract class ProxyHistory implements Identifiable<LocalHistoryIdentifier> {
    private abstract static class AbstractLocal extends ProxyHistory {
        private final ReadOnlyDataTree dataTree;

        AbstractLocal(final AbstractClientHistory parent, final AbstractClientConnection<ShardBackendInfo> connection,
            final LocalHistoryIdentifier identifier, final ReadOnlyDataTree dataTree) {
            super(parent, connection, identifier);
            this.dataTree = requireNonNull(dataTree);
        }

        final DataTreeSnapshot takeSnapshot() {
            return dataTree.takeSnapshot();
        }
    }

    private abstract static class AbstractRemote extends ProxyHistory {
        AbstractRemote(final AbstractClientHistory parent, final AbstractClientConnection<ShardBackendInfo> connection,
            final LocalHistoryIdentifier identifier) {
            super(parent, connection, identifier);
        }
    }

    private static final class Local extends AbstractLocal {
        private static final AtomicReferenceFieldUpdater<Local, LocalReadWriteProxyTransaction> LAST_SEALED_UPDATER =
                AtomicReferenceFieldUpdater.newUpdater(Local.class, LocalReadWriteProxyTransaction.class, "lastSealed");

        // Tracks the last open and last sealed transaction. We need to track both in case the user ends up aborting
        // the open one and attempts to create a new transaction again.
        private LocalReadWriteProxyTransaction lastOpen;

        private volatile LocalReadWriteProxyTransaction lastSealed;

        Local(final AbstractClientHistory parent, final AbstractClientConnection<ShardBackendInfo> connection,
            final LocalHistoryIdentifier identifier, final ReadOnlyDataTree dataTree) {
            super(parent, connection, identifier, dataTree);
        }

        @Override
        AbstractProxyTransaction doCreateTransactionProxy(final AbstractClientConnection<ShardBackendInfo> connection,
                final TransactionIdentifier txId, final boolean snapshotOnly, final boolean isDone) {
            checkState(lastOpen == null, "Proxy %s has %s currently open", this, lastOpen);

            if (isDone) {
                // Done transactions do not register on our radar on should not have any state associated.
                return snapshotOnly ? new LocalReadOnlyProxyTransaction(this, txId)
                        : new LocalReadWriteProxyTransaction(this, txId);
            }

            // onTransactionCompleted() runs concurrently
            final LocalReadWriteProxyTransaction localSealed = lastSealed;
            final DataTreeSnapshot baseSnapshot;
            if (localSealed != null) {
                baseSnapshot = localSealed.getSnapshot();
            } else {
                baseSnapshot = takeSnapshot();
            }

            if (snapshotOnly) {
                return new LocalReadOnlyProxyTransaction(this, txId, baseSnapshot);
            }

            lastOpen = new LocalReadWriteProxyTransaction(this, txId, baseSnapshot);
            LOG.debug("Proxy {} open transaction {}", this, lastOpen);
            return lastOpen;
        }

        @Override
        ProxyHistory createSuccessor(final AbstractClientConnection<ShardBackendInfo> connection) {
            return createClient(parent(), connection, getIdentifier());
        }

        @Override
        void onTransactionAborted(final AbstractProxyTransaction tx) {
            if (tx.equals(lastOpen)) {
                lastOpen = null;
            }
        }

        @Override
        void onTransactionCompleted(final AbstractProxyTransaction tx) {
            verify(tx instanceof LocalProxyTransaction, "Unexpected transaction %s", tx);
            if (tx instanceof LocalReadWriteProxyTransaction
                    && LAST_SEALED_UPDATER.compareAndSet(this, (LocalReadWriteProxyTransaction) tx, null)) {
                LOG.debug("Completed last sealed transaction {}", tx);
            }
        }

        @Override
        void onTransactionSealed(final AbstractProxyTransaction tx) {
            checkState(tx.equals(lastOpen));
            lastSealed = lastOpen;
            lastOpen = null;
        }
    }

    private static final class LocalSingle extends AbstractLocal {
        LocalSingle(final AbstractClientHistory parent, final AbstractClientConnection<ShardBackendInfo> connection,
            final LocalHistoryIdentifier identifier, final ReadOnlyDataTree dataTree) {
            super(parent, connection, identifier, dataTree);
        }

        @Override
        AbstractProxyTransaction doCreateTransactionProxy(final AbstractClientConnection<ShardBackendInfo> connection,
                final TransactionIdentifier txId, final boolean snapshotOnly, final boolean isDone) {
            final DataTreeSnapshot snapshot = takeSnapshot();
            return snapshotOnly ? new LocalReadOnlyProxyTransaction(this, txId, snapshot) :
                new LocalReadWriteProxyTransaction(this, txId, snapshot);
        }

        @Override
        ProxyHistory createSuccessor(final AbstractClientConnection<ShardBackendInfo> connection) {
            return createSingle(parent(), connection, getIdentifier());
        }
    }

    private static final class Remote extends AbstractRemote {
        Remote(final AbstractClientHistory parent, final AbstractClientConnection<ShardBackendInfo> connection,
            final LocalHistoryIdentifier identifier) {
            super(parent, connection, identifier);
        }

        @Override
        AbstractProxyTransaction doCreateTransactionProxy(final AbstractClientConnection<ShardBackendInfo> connection,
                final TransactionIdentifier txId, final boolean snapshotOnly, final boolean isDone) {
            return new RemoteProxyTransaction(this, txId, snapshotOnly, true, isDone);
        }

        @Override
        ProxyHistory createSuccessor(final AbstractClientConnection<ShardBackendInfo> connection) {
            return createClient(parent(), connection, getIdentifier());
        }
    }

    private static final class RemoteSingle extends AbstractRemote {
        RemoteSingle(final AbstractClientHistory parent, final AbstractClientConnection<ShardBackendInfo> connection,
            final LocalHistoryIdentifier identifier) {
            super(parent, connection, identifier);
        }

        @Override
        AbstractProxyTransaction doCreateTransactionProxy(final AbstractClientConnection<ShardBackendInfo> connection,
                final TransactionIdentifier txId, final boolean snapshotOnly, final boolean isDone) {
            return new RemoteProxyTransaction(this, txId, snapshotOnly, false, isDone);
        }

        @Override
        ProxyHistory createSuccessor(final AbstractClientConnection<ShardBackendInfo> connection) {
            return createSingle(parent(), connection, getIdentifier());
        }
    }

    private static final class RequestReplayException extends RequestException {
        private static final long serialVersionUID = 1L;

        RequestReplayException(final String format, final Object... args) {
            super(String.format(format, args));
        }

        @Override
        public boolean isRetriable() {
            return false;
        }
    }

    private final class ReconnectCohort extends ProxyReconnectCohort {
        @Override
        public LocalHistoryIdentifier getIdentifier() {
            return identifier;
        }

        @Holding("lock")
        @Override
        void replayRequests(final Collection<ConnectionEntry> previousEntries) {
            // First look for our Create message
            Iterator<ConnectionEntry> it = previousEntries.iterator();
            while (it.hasNext()) {
                final ConnectionEntry e = it.next();
                final Request<?, ?> req = e.getRequest();
                if (identifier.equals(req.getTarget())) {
                    verify(req instanceof LocalHistoryRequest, "Unexpected request %s", req);
                    if (req instanceof CreateLocalHistoryRequest) {
                        successor.connection.enqueueRequest(req, e.getCallback(), e.getEnqueuedTicks());
                        it.remove();
                        break;
                    }
                }
            }

            for (AbstractProxyTransaction t : proxies.values()) {
                LOG.debug("{} replaying messages to old proxy {} towards successor {}", identifier, t, successor);
                t.replayMessages(successor, previousEntries);
            }

            // Forward any skipped transactions
            final var local = skippedTransactions;
            if (local != null) {
                LOG.debug("{} forwarding skipped transactions towards successor {}", identifier, successor);
                successor.skipTransactions(local);
                skippedTransactions = null;
            }

            // Now look for any finalizing messages
            it = previousEntries.iterator();
            while (it.hasNext()) {
                final ConnectionEntry e  = it.next();
                final Request<?, ?> req = e.getRequest();
                if (identifier.equals(req.getTarget())) {
                    verify(req instanceof LocalHistoryRequest, "Unexpected request %s", req);
                    if (req instanceof DestroyLocalHistoryRequest) {
                        successor.connection.enqueueRequest(req, e.getCallback(), e.getEnqueuedTicks());
                        it.remove();
                        break;
                    }
                }
            }
        }

        @Holding("lock")
        @Override
        ProxyHistory finishReconnect() {
            final ProxyHistory ret = verifyNotNull(successor);

            for (AbstractProxyTransaction t : proxies.values()) {
                t.finishReconnect();
            }

            LOG.debug("Finished reconnecting proxy history {}", this);
            lock.unlock();
            return ret;
        }

        @Override
        void replayEntry(final ConnectionEntry entry, final Consumer<ConnectionEntry> replayTo)
                throws RequestException {
            final Request<?, ?> request = entry.getRequest();
            if (request instanceof TransactionRequest) {
                lookupProxy(request).replayRequest((TransactionRequest<?>) request, entry.getCallback(),
                    entry.getEnqueuedTicks());
            } else if (request instanceof LocalHistoryRequest) {
                replayTo.accept(entry);
            } else {
                throw new IllegalArgumentException("Unhandled request " + request);
            }
        }

        @Override
        void forwardEntry(final ConnectionEntry entry, final Consumer<ConnectionEntry> forwardTo)
                throws RequestException {
            final Request<?, ?> request = entry.getRequest();
            if (request instanceof TransactionRequest) {
                lookupProxy(request).forwardRequest((TransactionRequest<?>) request, entry.getCallback());
            } else if (request instanceof LocalHistoryRequest) {
                forwardTo.accept(entry);
            } else {
                throw new IllegalArgumentException("Unhandled request " + request);
            }
        }

        private AbstractProxyTransaction lookupProxy(final Request<?, ?> request)
                throws RequestReplayException {
            final AbstractProxyTransaction proxy;
            lock.lock();
            try {
                proxy = proxies.get(request.getTarget());
            } finally {
                lock.unlock();
            }
            if (proxy != null) {
                return proxy;
            }

            throw new RequestReplayException("Failed to find proxy for %s", request);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(ProxyHistory.class);

    private final Lock lock = new ReentrantLock();
    private final @NonNull LocalHistoryIdentifier identifier;
    private final @NonNull AbstractClientConnection<ShardBackendInfo> connection;
    private final @NonNull AbstractClientHistory parent;

    private final @GuardedBy("lock") Map<TransactionIdentifier, AbstractProxyTransaction> proxies =
        new LinkedHashMap<>();
    private @GuardedBy("lock") ProxyHistory successor;

    // List of transaction identifiers which were allocated by our parent history, but did not touch our shard. Each of
    // these represents a hole in otherwise-contiguous allocation of transactionIds. These holes are problematic, as
    // each of them prevents LeaderFrontendState.purgedHistories from coalescing, leading to a gradual heap exhaustion.
    //
    // <p>
    // We keep these in an ArrayList for fast insertion, as that happens when we are otherwise idle. We translate these
    // into purge requests when:
    // - we are about to allocate a new transaction
    // - we get a successor proxy
    // - the list grows unreasonably long
    //
    // TODO: we are tracking entire TransactionIdentifiers, but really only need to track the longs. Do that once we
    //       have a {@code List<long>}.
    // FIXME: this is not tuneable, but perhaps should be
    // FIXME: default value deserves some explanation -- this affects depth of an RB Tree on the receiving end.
    private static final int PURGE_SKIPPED_TXID_THRESHOLD = 256;

    private volatile @GuardedBy("lock") List<TransactionIdentifier> skippedTransactions;

    private ProxyHistory(final AbstractClientHistory parent,
            final AbstractClientConnection<ShardBackendInfo> connection, final LocalHistoryIdentifier identifier) {
        this.parent = requireNonNull(parent);
        this.connection = requireNonNull(connection);
        this.identifier = requireNonNull(identifier);
    }

    static ProxyHistory createClient(final AbstractClientHistory parent,
            final AbstractClientConnection<ShardBackendInfo> connection, final LocalHistoryIdentifier identifier) {
        final Optional<ReadOnlyDataTree> dataTree = connection.getBackendInfo().flatMap(ShardBackendInfo::getDataTree);
        return dataTree.isPresent() ? new Local(parent, connection, identifier, dataTree.orElseThrow())
             : new Remote(parent, connection, identifier);
    }

    static ProxyHistory createSingle(final AbstractClientHistory parent,
            final AbstractClientConnection<ShardBackendInfo> connection,
            final LocalHistoryIdentifier identifier) {
        final Optional<ReadOnlyDataTree> dataTree = connection.getBackendInfo().flatMap(ShardBackendInfo::getDataTree);
        return dataTree.isPresent() ? new LocalSingle(parent, connection, identifier, dataTree.orElseThrow())
             : new RemoteSingle(parent, connection, identifier);
    }

    @Override
    // Non-final for mocking
    public LocalHistoryIdentifier getIdentifier() {
        return identifier;
    }

    final ClientActorContext context() {
        return connection.context();
    }

    final long currentTime() {
        return connection.currentTime();
    }

    final ActorRef localActor() {
        return connection.localActor();
    }

    final AbstractClientHistory parent() {
        return parent;
    }

    final AbstractProxyTransaction createTransactionProxy(final TransactionIdentifier txId,
            final boolean snapshotOnly) {
        return createTransactionProxy(txId, snapshotOnly, false);
    }

    // Non-final for mocking
    AbstractProxyTransaction createTransactionProxy(final TransactionIdentifier txId, final boolean snapshotOnly,
            final boolean isDone) {
        lock.lock();
        try {
            if (successor != null) {
                return successor.createTransactionProxy(txId, snapshotOnly, isDone);
            }

            final TransactionIdentifier proxyId = new TransactionIdentifier(identifier, txId.getTransactionId());
            final AbstractProxyTransaction ret = doCreateTransactionProxy(connection, proxyId, snapshotOnly, isDone);
            proxies.put(proxyId, ret);
            LOG.debug("Allocated proxy {} for transaction {}", proxyId, txId);
            return ret;
        } finally {
            lock.unlock();
        }
    }

    final void skipTransaction(final TransactionIdentifier txId) {
        lock.lock();
        try {
            if (successor != null) {
                successor.skipTransaction(txId);
                return;
            }

            var local = skippedTransactions;
            if (local == null) {
                skippedTransactions = local = new ArrayList<>();
            }
            local.add(txId);
            LOG.debug("Recorded skipped transaction {}", txId);
            skipIfNeeded(local);
        } finally {
            lock.unlock();
        }
    }

    @Holding("lock")
    private void skipIfNeeded(final List<TransactionIdentifier> current) {
        if (current.size() >= PURGE_SKIPPED_TXID_THRESHOLD) {
            skippedTransactions = null;
            doSkipTransactions(current);
        }
    }

    private void skipTransactions(final List<TransactionIdentifier> toSkip) {
        lock.lock();
        try {
            if (successor != null) {
                successor.skipTransactions(toSkip);
                return;
            }

            var local = skippedTransactions;
            if (local != null) {
                local.addAll(toSkip);
            } else {
                skippedTransactions = local = toSkip;
            }
            skipIfNeeded(local);
        } finally {
            lock.unlock();
        }
    }

    private void skipTransactions() {
        var local = skippedTransactions;
        if (local != null) {
            lock.lock();
            try {
                local = skippedTransactions;
                if (local != null && successor == null) {
                    skippedTransactions = null;
                    doSkipTransactions(local);
                }
            } finally {
                lock.unlock();
            }
        }
    }

    @Holding("lock")
    private void doSkipTransactions(final List<TransactionIdentifier> toSkip) {
        final var txIds = toSkip.stream()
            .mapToLong(TransactionIdentifier::getTransactionId)
            .distinct()
            .sorted()
            .mapToObj(UnsignedLong::fromLongBits)
            .collect(ImmutableList.toImmutableList());

        LOG.debug("Proxy {} skipping transactions {}", this, txIds);
        connection.enqueueRequest(new SkipTransactionsRequest(new TransactionIdentifier(identifier,
            txIds.get(0).longValue()), 0, localActor(),txIds.subList(1, txIds.size())), resp -> {
                LOG.debug("Proxy {} confirmed transaction skip", this);
            }, connection.currentTime());
    }

    final void abortTransaction(final AbstractProxyTransaction tx) {
        lock.lock();
        try {
            // Removal will be completed once purge completes
            LOG.debug("Proxy {} aborted transaction {}", this, tx);
            onTransactionAborted(tx);
        } finally {
            lock.unlock();
        }
    }

    final void completeTransaction(final AbstractProxyTransaction tx) {
        lock.lock();
        try {
            // Removal will be completed once purge completes
            LOG.debug("Proxy {} completing transaction {}", this, tx);
            onTransactionCompleted(tx);
        } finally {
            lock.unlock();
        }
    }

    final void purgeTransaction(final AbstractProxyTransaction tx) {
        lock.lock();
        try {
            proxies.remove(tx.getIdentifier());
            LOG.debug("Proxy {} purged transaction {}", this, tx);
        } finally {
            lock.unlock();
        }
    }

    final void close() {
        lock.lock();
        try {
            if (successor != null) {
                successor.close();
                return;
            }

            LOG.debug("Proxy {} invoking destroy", this);
            connection.sendRequest(new DestroyLocalHistoryRequest(getIdentifier(), 1, localActor()),
                this::onDestroyComplete);
        } finally {
            lock.unlock();
        }
    }

    final void enqueueRequest(final TransactionRequest<?> request, final Consumer<Response<?, ?>> callback,
            final long enqueuedTicks) {
        skipTransactions();
        connection.enqueueRequest(request, callback, enqueuedTicks);
    }

    final void sendRequest(final TransactionRequest<?> request, final Consumer<Response<?, ?>> callback) {
        skipTransactions();
        connection.sendRequest(request, callback);
    }

    @Holding("lock")
    @SuppressWarnings("checkstyle:hiddenField")
    abstract AbstractProxyTransaction doCreateTransactionProxy(AbstractClientConnection<ShardBackendInfo> connection,
            TransactionIdentifier txId, boolean snapshotOnly, boolean isDone);

    @Holding("lock")
    @SuppressWarnings("checkstyle:hiddenField")
    abstract ProxyHistory createSuccessor(AbstractClientConnection<ShardBackendInfo> connection);

    @SuppressFBWarnings(value = "UL_UNRELEASED_LOCK", justification = "Lock is released asynchronously via the cohort")
    final ProxyReconnectCohort startReconnect(final ConnectedClientConnection<ShardBackendInfo> newConnection) {
        lock.lock();
        if (successor != null) {
            lock.unlock();
            throw new IllegalStateException("Proxy history " + this + " already has a successor");
        }

        successor = createSuccessor(newConnection);
        LOG.debug("History {} instantiated successor {}", this, successor);

        for (AbstractProxyTransaction t : proxies.values()) {
            t.startReconnect();
        }

        return new ReconnectCohort();
    }

    private void onDestroyComplete(final Response<?, ?> response) {
        LOG.debug("Proxy {} destroy completed with {}", this, response);

        lock.lock();
        try {
            parent.onProxyDestroyed(this);
            connection.sendRequest(new PurgeLocalHistoryRequest(getIdentifier(), 2, localActor()),
                this::onPurgeComplete);
        } finally {
            lock.unlock();
        }
    }

    private void onPurgeComplete(final Response<?, ?> response) {
        LOG.debug("Proxy {} purge completed with {}", this, response);
    }

    @Holding("lock")
    void onTransactionAborted(final AbstractProxyTransaction tx) {
        // No-op for most implementations
    }

    @Holding("lock")
    void onTransactionCompleted(final AbstractProxyTransaction tx) {
        // No-op for most implementations
    }

    @Holding("lock")
    void onTransactionSealed(final AbstractProxyTransaction tx) {
        // No-op on most implementations
    }
}
