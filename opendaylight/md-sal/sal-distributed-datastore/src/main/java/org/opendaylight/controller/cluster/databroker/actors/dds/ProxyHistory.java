/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import akka.actor.ActorRef;
import com.google.common.base.Preconditions;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.StampedLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.cluster.access.commands.LocalHistoryRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Per-connection representation of a local history. This class handles state replication across a single connection.
 *
 * @author Robert Varga
 */
abstract class ProxyHistory implements Identifiable<LocalHistoryIdentifier> {
    private static abstract class AbstractLocal extends ProxyHistory {
        private final DataTree dataTree;

        AbstractLocal(final AbstractClientConnection connection, final LocalHistoryIdentifier identifier,
            final DataTree dataTree) {
            super(connection, identifier);
            this.dataTree = Preconditions.checkNotNull(dataTree);
        }

        final DataTreeSnapshot takeSnapshot() {
            return dataTree.takeSnapshot();
        }
    }

    private static final class Local extends AbstractLocal {
        Local(final AbstractClientConnection connection, final LocalHistoryIdentifier identifier,
            final DataTree dataTree) {
            super(connection, identifier, dataTree);
        }

        @Override
        AbstractProxyTransaction doCreateTransactionProxy(final AbstractClientConnection connection,
                final TransactionIdentifier txId) {
            // FIXME: this violates history contract: we should use the last submitted transaction instead to ensure
            //        causality
            return new LocalProxyTransaction(this, txId, takeSnapshot());
        }
    }

    private static final class LocalSingle extends AbstractLocal {
        LocalSingle(final AbstractClientConnection connection, final LocalHistoryIdentifier identifier,
            final DataTree dataTree) {
            super(connection, identifier, dataTree);
        }

        @Override
        AbstractProxyTransaction doCreateTransactionProxy(final AbstractClientConnection connection,
                final TransactionIdentifier txId) {
            return new LocalProxyTransaction(this, txId, takeSnapshot());
        }
    }

    private static final class Remote extends ProxyHistory {
        Remote(final AbstractClientConnection connection, final LocalHistoryIdentifier identifier) {
            super(connection, identifier);
        }

        @Override
        AbstractProxyTransaction doCreateTransactionProxy(final AbstractClientConnection connection,
                final TransactionIdentifier txId) {
            return new RemoteProxyTransaction(this, txId);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(ProxyHistory.class);

    private static final RequestException FAILED_TO_REPLAY_EXCEPTION =
            new RequestException("Failed to replay request") {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isRetriable() {
            return false;
        }
    };

    @GuardedBy("lock")
    private final Map<TransactionIdentifier, AbstractProxyTransaction> proxies = new LinkedHashMap<>();
    private final StampedLock lock = new StampedLock();
    private final LocalHistoryIdentifier identifier;

    private volatile AbstractClientConnection connection;

    ProxyHistory(final AbstractClientConnection connection, final LocalHistoryIdentifier identifier) {
        this.connection = Preconditions.checkNotNull(connection);
        this.identifier = Preconditions.checkNotNull(identifier);
    }

    static ProxyHistory createClient(final AbstractClientConnection connection,
            final LocalHistoryIdentifier identifier) {
        final Optional<DataTree> dataTree = connection.getBackendInfo().flatMap(ShardBackendInfo::getDataTree);
        return dataTree.isPresent() ? new Local(connection, identifier, dataTree.get())
             : new Remote(connection, identifier);
    }

    static ProxyHistory createSingle(final AbstractClientConnection connection,
            final LocalHistoryIdentifier identifier) {
        final Optional<DataTree> dataTree = connection.getBackendInfo().flatMap(ShardBackendInfo::getDataTree);
        return dataTree.isPresent() ? new LocalSingle(connection, identifier, dataTree.get())
             : new Remote(connection, identifier);
    }

    @Override
    public LocalHistoryIdentifier getIdentifier() {
        return identifier;
    }

    final ActorRef localActor() {
        return connection.localActor();
    }

    final AbstractProxyTransaction createTransactionProxy(final TransactionIdentifier txId) {
        final TransactionIdentifier proxyId = new TransactionIdentifier(identifier, txId.getTransactionId());

        final long stamp = lock.readLock();
        try {
            final AbstractProxyTransaction ret = doCreateTransactionProxy(connection, proxyId);
            proxies.put(txId, ret);
            return ret;
        } finally {
            lock.unlockRead(stamp);
        }
    }

    final void completeTransaction(final AbstractProxyTransaction tx) {
        final long stamp = lock.readLock();
        try {
            proxies.remove(tx.getIdentifier());
        } finally {
            lock.unlockRead(stamp);
        }
    }

    final void sendRequest(final TransactionRequest<?> request, final Consumer<Response<?, ?>> callback) {
        connection.sendRequest(request, callback);
    }

    abstract AbstractProxyTransaction doCreateTransactionProxy(AbstractClientConnection connection,
            TransactionIdentifier txId);

    @GuardedBy("lock")
    private void replaySuccessfulRequests(final BiConsumer<Request<?, ?>, Consumer<Response<?, ?>>> replayTo) {
        for (AbstractProxyTransaction t : proxies.values()) {
            t.replaySuccessfulRequests(replayTo);
        }
    }

    private void replayRequest(final Request<?, ?> request, final Consumer<Response<?, ?>> callback,
            final BiConsumer<Request<?, ?>, Consumer<Response<?, ?>>> replayTo) throws RequestException {
        if (request instanceof TransactionRequest) {
            replayTransactionRequest((TransactionRequest<?>) request, callback, replayTo);
        } else if (request instanceof LocalHistoryRequest) {
            replayLocalHistoryRequest((LocalHistoryRequest<?>) request, callback, replayTo);
        } else {
            throw new IllegalArgumentException("Unhandled request " + request);
        }
    }

    private static void replayLocalHistoryRequest(final LocalHistoryRequest<?> request,
            final Consumer<Response<?, ?>> callback,
            final BiConsumer<Request<?, ?>, Consumer<Response<?, ?>>> replayTo) {
        replayTo.accept(request, callback);
    }

    private void replayTransactionRequest(final TransactionRequest<?> request,
            final Consumer<Response<?, ?>> callback,
            final BiConsumer<Request<?, ?>, Consumer<Response<?, ?>>> replayTo) throws RequestException {
        // XXX: this should be safe for the most part. We can get called by delayed requests, but those should not
        //      proxies to be touched, as that would indicate multi-threaded -- which we do not allow. Nevertheless
        //      it would be nice if we could detect concurrent access...
        final AbstractProxyTransaction proxy = proxies.get(request);
        if (proxy == null) {
            LOG.warn("Failed to find proxy for {}", request);
            throw FAILED_TO_REPLAY_EXCEPTION;
        }

        proxy.replayRequest(request, callback, replayTo);
    }

    @GuardedBy("lock")
    private void replaceConnection(final AbstractClientConnection newConnection, final long stamp) {
        LOG.debug("Replacing connection {} in {} with {}", connection, this, newConnection);
        connection = Preconditions.checkNotNull(newConnection);
        lock.unlockWrite(stamp);
    }

    ReconnectCohort startReconnect(final ShardBackendInfo backend) {
        final long stamp = lock.writeLock();

        // FIXME: use backend

        return new ReconnectCohort() {
            @Override
            public LocalHistoryIdentifier getIdentifier() {
                return identifier;
            }

            @Override
            void replaySuccessfulRequests(final BiConsumer<Request<?, ?>, Consumer<Response<?, ?>>> replayTo) {
                ProxyHistory.this.replaySuccessfulRequests(replayTo);
            }

            @Override
            void finishReconnect(final AbstractClientConnection connection) {
                ProxyHistory.this.replaceConnection(connection, stamp);
            }

            @Override
            void replayRequest(final Request<?, ?> request, final Consumer<Response<?, ?>> callback,
                    final BiConsumer<Request<?, ?>, Consumer<Response<?, ?>>> replayTo) throws RequestException {
                ProxyHistory.this.replayRequest(request, callback, replayTo);
            }
        };
    }
}
