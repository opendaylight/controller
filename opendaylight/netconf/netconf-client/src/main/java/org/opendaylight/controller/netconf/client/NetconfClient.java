/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.client;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.client.conf.NetconfClientConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import io.netty.util.concurrent.Future;

/**
 * @deprecated Use {@link NetconfClientDispatcherImpl.createClient()} or {@link NetconfClientDispatcherImpl.createReconnectingClient()} instead.
 */
@Deprecated
public class NetconfClient implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(NetconfClient.class);

    private final NetconfClientDispatcher dispatch;
    private final String label;
    private final NetconfClientSession clientSession;
    private final NetconfClientSessionListener sessionListener;
    private final long sessionId;
    private final InetSocketAddress address;


    private NetconfClientSession get(final Future<NetconfClientSession> clientFuture) throws InterruptedException {
        try {
            return clientFuture.get();
        } catch (final CancellationException e) {
            throw new RuntimeException("Cancelling " + this, e);
        } catch (final ExecutionException e) {
            throw new IllegalStateException("Unable to create " + this, e);
        }
    }

    public NetconfClient(final String clientLabelForLogging, final NetconfClientDispatcher netconfClientDispatcher, final NetconfClientConfiguration cfg) throws InterruptedException{
        this.label = clientLabelForLogging;
        dispatch = netconfClientDispatcher;
        sessionListener = cfg.getSessionListener();
        final Future<NetconfClientSession> clientFuture = dispatch.createClient(cfg);
        this.address = cfg.getAddress();
        clientSession = get(clientFuture);
        this.sessionId = clientSession.getSessionId();
    }

    public Future<NetconfMessage> sendRequest(final NetconfMessage message) {
        return ((SimpleNetconfClientSessionListener)sessionListener).sendRequest(message);
    }

    /**
     * @deprecated Use {@link sendRequest} instead
     */
    @Deprecated
    public NetconfMessage sendMessage(final NetconfMessage message) throws ExecutionException, InterruptedException, TimeoutException {
        return sendMessage(message, 5, 1000);
    }

    /**
     * @deprecated Use {@link sendRequest} instead
     */
    @Deprecated
    public NetconfMessage sendMessage(final NetconfMessage message, final int attempts, final int attemptMsDelay) throws ExecutionException, InterruptedException, TimeoutException {
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
