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
import com.google.common.base.Verify;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.cluster.access.client.AbstractClientConnection;
import org.opendaylight.controller.cluster.access.client.ConnectedClientConnection;
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
    private abstract static class AbstractLocal extends ProxyHistory {
        private final DataTree dataTree;

        AbstractLocal(final AbstractClientConnection<ShardBackendInfo> connection,
            final LocalHistoryIdentifier identifier, final DataTree dataTree) {
            super(connection, identifier);
            this.dataTree = Preconditions.checkNotNull(dataTree);
        }

        final DataTreeSnapshot takeSnapshot() {
            return dataTree.takeSnapshot();
        }
    }

    private abstract static class AbstractRemote extends ProxyHistory {
        AbstractRemote(final AbstractClientConnection<ShardBackendInfo> connection,
            final LocalHistoryIdentifier identifier) {
            super(connection, identifier);
        }

        @Override
        final AbstractProxyTransaction doCreateTransactionProxy(
                final AbstractClientConnection<ShardBackendInfo> connection, final TransactionIdentifier txId) {
            return new RemoteProxyTransaction(this, txId);
        }
    }

    private static final class Local extends AbstractLocal {
        Local(final AbstractClientConnection<ShardBackendInfo> connection, final LocalHistoryIdentifier identifier,
            final DataTree dataTree) {
            super(connection, identifier, dataTree);
        }

        @Override
        AbstractProxyTransaction doCreateTransactionProxy(final AbstractClientConnection<ShardBackendInfo> connection,
                final TransactionIdentifier txId) {
            // FIXME: this violates history contract: we should use the last submitted transaction instead to ensure
            //        causality
            return new LocalProxyTransaction(this, txId, takeSnapshot());
        }

        @Override
        ProxyHistory createSuccessor(final AbstractClientConnection<ShardBackendInfo> connection) {
            return createClient(connection, getIdentifier());
        }
    }

    private static final class LocalSingle extends AbstractLocal {
        LocalSingle(final AbstractClientConnection<ShardBackendInfo> connection,
            final LocalHistoryIdentifier identifier, final DataTree dataTree) {
            super(connection, identifier, dataTree);
        }

        @Override
        AbstractProxyTransaction doCreateTransactionProxy(final AbstractClientConnection<ShardBackendInfo> connection,
                final TransactionIdentifier txId) {
            return new LocalProxyTransaction(this, txId, takeSnapshot());
        }

        @Override
        ProxyHistory createSuccessor(final AbstractClientConnection<ShardBackendInfo> connection) {
            return createSingle(connection, getIdentifier());
        }
    }

    private static final class Remote extends AbstractRemote {
        Remote(final AbstractClientConnection<ShardBackendInfo> connection, final LocalHistoryIdentifier identifier) {
            super(connection, identifier);
        }

        @Override
        ProxyHistory createSuccessor(final AbstractClientConnection<ShardBackendInfo> connection) {
            return createClient(connection, getIdentifier());
        }
    }

    private static final class RemoteSingle extends AbstractRemote {
        RemoteSingle(final AbstractClientConnection<ShardBackendInfo> connection,
            final LocalHistoryIdentifier identifier) {
            super(connection, identifier);
        }

        @Override
        ProxyHistory createSuccessor(final AbstractClientConnection<ShardBackendInfo> connection) {
            return createSingle(connection, getIdentifier());
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

    private static final Logger LOG = LoggerFactory.getLogger(ProxyHistory.class);

    private final Lock lock = new ReentrantLock();
    private final LocalHistoryIdentifier identifier;
    private final AbstractClientConnection<ShardBackendInfo> connection;

    @GuardedBy("lock")
    private final Map<TransactionIdentifier, AbstractProxyTransaction> proxies = new LinkedHashMap<>();
    @GuardedBy("lock")
    private ProxyHistory successor;

    private ProxyHistory(final AbstractClientConnection<ShardBackendInfo> connection,
            final LocalHistoryIdentifier identifier) {
        this.connection = Preconditions.checkNotNull(connection);
        this.identifier = Preconditions.checkNotNull(identifier);
    }

    static ProxyHistory createClient(final AbstractClientConnection<ShardBackendInfo> connection,
            final LocalHistoryIdentifier identifier) {
        final Optional<DataTree> dataTree = connection.getBackendInfo().flatMap(ShardBackendInfo::getDataTree);
        return dataTree.isPresent() ? new Local(connection, identifier, dataTree.get())
             : new Remote(connection, identifier);
    }

    static ProxyHistory createSingle(final AbstractClientConnection<ShardBackendInfo> connection,
            final LocalHistoryIdentifier identifier) {
        final Optional<DataTree> dataTree = connection.getBackendInfo().flatMap(ShardBackendInfo::getDataTree);
        return dataTree.isPresent() ? new LocalSingle(connection, identifier, dataTree.get())
             : new RemoteSingle(connection, identifier);
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

        lock.lock();
        try {
            final AbstractProxyTransaction ret = doCreateTransactionProxy(connection, proxyId);
            proxies.put(proxyId, ret);
            LOG.debug("Allocated proxy {} for transaction {}", proxyId, txId);
            return ret;
        } finally {
            lock.unlock();
        }
    }

    final void completeTransaction(final AbstractProxyTransaction tx) {
        lock.lock();
        try {
            proxies.remove(tx.getIdentifier());
        } finally {
            lock.unlock();
        }
    }

    final void sendRequest(final TransactionRequest<?> request, final Consumer<Response<?, ?>> callback) {
        connection.sendRequest(request, callback);
    }

    abstract AbstractProxyTransaction doCreateTransactionProxy(AbstractClientConnection<ShardBackendInfo> connection,
            TransactionIdentifier txId);

    abstract ProxyHistory createSuccessor(AbstractClientConnection<ShardBackendInfo> connection);

    ProxyReconnectCohort startReconnect(final ConnectedClientConnection<ShardBackendInfo> newConnection) {
        lock.lock();
        if (successor != null) {
            lock.unlock();
            throw new IllegalStateException("Proxy history " + this + " already has a successor");
        }

        successor = createSuccessor(newConnection);

        return new ProxyReconnectCohort() {
            @Override
            public LocalHistoryIdentifier getIdentifier() {
                return identifier;
            }

            @GuardedBy("lock")
            @Override
            void replaySuccessfulRequests() {
                for (AbstractProxyTransaction t : proxies.values()) {
                    final AbstractProxyTransaction newProxy = successor.createTransactionProxy(t.getIdentifier());
                    LOG.debug("{} created successor transaction proxy {} for {}", identifier, newProxy, t);
                    t.replaySuccessfulRequests(newProxy);
                }
            }

            @GuardedBy("lock")
            @Override
            ProxyHistory finishReconnect() {
                final ProxyHistory ret = Verify.verifyNotNull(successor);
                LOG.debug("Finished reconnecting proxy history {}", this);
                lock.unlock();
                return ret;
            }

            @Override
            void replayRequest(final Request<?, ?> request, final Consumer<Response<?, ?>> callback,
                    final BiConsumer<Request<?, ?>, Consumer<Response<?, ?>>> replayTo) throws RequestException {
                if (request instanceof TransactionRequest) {
                    replayTransactionRequest((TransactionRequest<?>) request, callback);
                } else if (request instanceof LocalHistoryRequest) {
                    replayTo.accept(request, callback);
                } else {
                    throw new IllegalArgumentException("Unhandled request " + request);
                }
            }

            private void replayTransactionRequest(final TransactionRequest<?> request,
                    final Consumer<Response<?, ?>> callback) throws RequestException {

                final AbstractProxyTransaction proxy;
                lock.lock();
                try {
                    proxy = proxies.get(request.getTarget());
                } finally {
                    lock.unlock();
                }
                if (proxy == null) {
                    throw new RequestReplayException("Failed to find proxy for %s", request);
                }

                proxy.replayRequest(request, callback);
            }
        };
    }
}
