/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.client.test;

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
import org.opendaylight.controller.netconf.client.NetconfClientDispatcher;
import org.opendaylight.controller.netconf.client.NetconfClientSession;
import org.opendaylight.controller.netconf.client.NetconfClientSessionListener;
import org.opendaylight.controller.netconf.client.SimpleNetconfClientSessionListener;
import org.opendaylight.protocol.framework.NeverReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategy;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;


/**
 * Synchronous netconf client suitable for testing
 */
public class TestingNetconfClient implements Closeable {

    public static final int DEFAULT_CONNECT_TIMEOUT = 5000;

    private final String label;
    private final NetconfClientSession clientSession;
    private final NetconfClientSessionListener sessionListener;
    private final long sessionId;

    private TestingNetconfClient(String clientLabel, InetSocketAddress address, ReconnectStrategy strat,
            NetconfClientDispatcher netconfClientDispatcher) throws InterruptedException {
        this.label = clientLabel;
        sessionListener = new SimpleNetconfClientSessionListener();
        Future<NetconfClientSession> clientFuture = netconfClientDispatcher.createClient(address, sessionListener, strat);
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

    public TestingNetconfClient(String clientLabelForLogging, InetSocketAddress address, int connectTimeoutMs,
                                NetconfClientDispatcher netconfClientDispatcher) throws InterruptedException {
        this(clientLabelForLogging, address,
                new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE, connectTimeoutMs), netconfClientDispatcher);
    }

    public TestingNetconfClient(String clientLabelForLogging, InetSocketAddress address,
                                NetconfClientDispatcher netconfClientDispatcher) throws InterruptedException {
        this(clientLabelForLogging, address, new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE,
                DEFAULT_CONNECT_TIMEOUT), netconfClientDispatcher);
    }

    public Future<NetconfMessage> sendRequest(NetconfMessage message) {
        return ((SimpleNetconfClientSessionListener)sessionListener).sendRequest(message);
    }

    public NetconfMessage sendMessage(NetconfMessage message, int attemptMsDelay) throws ExecutionException,
            InterruptedException, TimeoutException {
        return sendRequest(message).get(attemptMsDelay, TimeUnit.MILLISECONDS);
    }

    public NetconfMessage sendMessage(NetconfMessage message) throws ExecutionException,
            InterruptedException, TimeoutException {
        return sendMessage(message, DEFAULT_CONNECT_TIMEOUT);
    }

    @Override
    public void close() throws IOException {
        clientSession.close();
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("TestingNetconfClient{");
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
}