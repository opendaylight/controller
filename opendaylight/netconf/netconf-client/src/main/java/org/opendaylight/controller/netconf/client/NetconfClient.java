/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.client;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.protocol.framework.NeverReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.TimedReconnectStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;

/**
 * @deprecated Use {@link NetconfClientDispatcher.createClient()} or {@link NetconfClientDispatcher.createReconnectingClient()} instead.
 */
@Deprecated
public class NetconfClient implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(NetconfClient.class);

    public static final int DEFAULT_CONNECT_TIMEOUT = 5000;
    private final NetconfClientDispatcher dispatch;
    private final String label;
    private final NetconfClientSession clientSession;
    private final NetconfClientSessionListener sessionListener;
    private final long sessionId;
    private final InetSocketAddress address;

    // TODO test reconnecting constructor
    public NetconfClient(String clientLabelForLogging, InetSocketAddress address, int connectionAttempts,
            int attemptMsTimeout, NetconfClientDispatcher netconfClientDispatcher) throws InterruptedException {
        this(clientLabelForLogging, address, getReconnectStrategy(connectionAttempts, attemptMsTimeout),
                netconfClientDispatcher);
    }

    private NetconfClient(String clientLabelForLogging, InetSocketAddress address, ReconnectStrategy strat, NetconfClientDispatcher netconfClientDispatcher) throws InterruptedException {
        this.label = clientLabelForLogging;
        dispatch = netconfClientDispatcher;
        sessionListener = new SimpleNetconfClientSessionListener();
        Future<NetconfClientSession> clientFuture = dispatch.createClient(address, sessionListener, strat);
        this.address = address;
        clientSession = get(clientFuture);
        this.sessionId = clientSession.getSessionId();
    }

    private NetconfClientSession get(Future<NetconfClientSession> clientFuture) throws InterruptedException {
        try {
            return clientFuture.get();
        } catch (CancellationException e) {
            throw new RuntimeException("Cancelling " + this, e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Unable to create " + this, e);
        }
    }

    public static NetconfClient clientFor(String clientLabelForLogging, InetSocketAddress address, ReconnectStrategy strategy, NetconfClientDispatcher netconfClientDispatcher) throws InterruptedException {
        return new NetconfClient(clientLabelForLogging,address,strategy,netconfClientDispatcher);
    }

    public static NetconfClient clientFor(String clientLabelForLogging, InetSocketAddress address,
            ReconnectStrategy strategy, NetconfClientDispatcher netconfClientDispatcher, NetconfClientSessionListener listener) throws InterruptedException {
        return new NetconfClient(clientLabelForLogging,address,strategy,netconfClientDispatcher,listener);
    }

    public NetconfClient(String clientLabelForLogging, InetSocketAddress address, int connectTimeoutMs,
            NetconfClientDispatcher netconfClientDispatcher) throws InterruptedException {
        this(clientLabelForLogging, address,
                new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE, connectTimeoutMs), netconfClientDispatcher);
    }

    public NetconfClient(String clientLabelForLogging, InetSocketAddress address,
            NetconfClientDispatcher netconfClientDispatcher) throws InterruptedException {
        this(clientLabelForLogging, address, new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE,
                DEFAULT_CONNECT_TIMEOUT), netconfClientDispatcher);
    }

    public NetconfClient(String clientLabelForLogging, InetSocketAddress address, ReconnectStrategy strategy,
            NetconfClientDispatcher netconfClientDispatcher, NetconfClientSessionListener listener) throws InterruptedException{
        this.label = clientLabelForLogging;
        dispatch = netconfClientDispatcher;
        sessionListener = listener;
        Future<NetconfClientSession> clientFuture = dispatch.createClient(address, sessionListener, strategy);
        this.address = address;
        clientSession = get(clientFuture);
        this.sessionId = clientSession.getSessionId();
    }

    public Future<NetconfMessage> sendRequest(NetconfMessage message) {
        return ((SimpleNetconfClientSessionListener)sessionListener).sendRequest(message);
    }

    /**
     * @deprecated Use {@link sendRequest} instead
     */
    @Deprecated
    public NetconfMessage sendMessage(NetconfMessage message) throws ExecutionException, InterruptedException, TimeoutException {
        return sendMessage(message, 5, 1000);
    }

    /**
     * @deprecated Use {@link sendRequest} instead
     */
    @Deprecated
    public NetconfMessage sendMessage(NetconfMessage message, int attempts, int attemptMsDelay) throws ExecutionException, InterruptedException, TimeoutException {
        //logger.debug("Sending message: {}",XmlUtil.toString(message.getDocument()));
        final Stopwatch stopwatch = new Stopwatch().start();

        try {
            return sendRequest(message).get(attempts * attemptMsDelay, TimeUnit.MILLISECONDS);
        } finally {
            stopwatch.stop();
            logger.debug("Total time spent waiting for response from {}: {} ms", address, stopwatch.elapsed(TimeUnit.MILLISECONDS));
        }
    }

    @Override
    public void close() throws IOException {
        clientSession.close();
    }

    public NetconfClientDispatcher getNetconfClientDispatcher() {
        return dispatch;
    }

    private static ReconnectStrategy getReconnectStrategy(int connectionAttempts, int attemptMsTimeout) {
        return new TimedReconnectStrategy(GlobalEventExecutor.INSTANCE, attemptMsTimeout, 1000, 1.0, null,
                Long.valueOf(connectionAttempts), null);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("NetconfClient{");
        sb.append("label=").append(label);
        sb.append(", sessionId=").append(sessionId);
        sb.append('}');
        return sb.toString();
    }

    public long getSessionId() {
        return sessionId;
    }

    public Set<String> getCapabilities() {
        Preconditions.checkState(clientSession != null, "Client was not initialized successfully");
        return Sets.newHashSet(clientSession.getServerCapabilities());
    }

    public NetconfClientSession getClientSession() {
        return clientSession;
    }
}
