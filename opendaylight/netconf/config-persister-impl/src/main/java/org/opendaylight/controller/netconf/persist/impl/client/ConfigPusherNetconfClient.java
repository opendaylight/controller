/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.persist.impl.client;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.base.Stopwatch;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcher;
import org.opendaylight.controller.netconf.client.NetconfClientSession;
import org.opendaylight.controller.netconf.client.NetconfClientSessionListener;
import org.opendaylight.controller.netconf.client.SimpleNetconfClientSessionListener;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import io.netty.util.concurrent.Future;

/**
 * Netconf client that checks for required capabilities to be present in hello message retrieved from netconf server
 */
public class ConfigPusherNetconfClient implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(ConfigPusherNetconfClient.class);

    private final String label;
    private final NetconfClientSessionListener sessionListener;
    private final Set<String> expectedCapabilities;

    private NetconfClientSession clientSession;
    private InetSocketAddress netconfServerAddress;

    public ConfigPusherNetconfClient(String clientLabelForLogging, Set<String> expectedCapabilities) {
        this.label = clientLabelForLogging;
        this.expectedCapabilities = expectedCapabilities;
        sessionListener = new SimpleNetconfClientSessionListener();
    }

    /**
     * Tries to bind to a netconf endpoint and check for required capabilities.
     * This method might be called multiple times, in case of unsuccessful connection attempt.
     */
    public Set<String> connect(InetSocketAddress netconfAddress, NetconfClientDispatcher netconfClientDispatcher,
            ReconnectStrategy reconnectStrategy) throws ConnectFailedException, InterruptedException, MissingCapabilitiesException {
        Preconditions.checkState(clientSession == null, "Already connected");

        // TODO If connection fails during runtime, no reestablishment will be attempted
        // Reconnecting client might be used
        netconfServerAddress = netconfAddress;
        clientSession = get(netconfClientDispatcher.createClient(netconfAddress, sessionListener, reconnectStrategy));

        logger.trace("Connection established successfully with {}, {}", netconfAddress, this);
        Set<String> serverCapabilities = Sets.newHashSet(clientSession.getServerCapabilities());

        checkCapabilities(expectedCapabilities, serverCapabilities);
        logger.trace("Capabilities stabilized {}, {}", serverCapabilities, this);

        return serverCapabilities;
    }

    private void checkCapabilities(Set<String> expectedCapabilities, Set<String> serverCapabilities) throws MissingCapabilitiesException {
        if (isSubset(serverCapabilities, expectedCapabilities) == false) {
            resetSession();
            Set<String> allNotFound = computeNotFoundCapabilities(expectedCapabilities, serverCapabilities);
            throw new MissingCapabilitiesException(serverCapabilities, expectedCapabilities, allNotFound);
        }
    }

    private static Set<String> computeNotFoundCapabilities(Set<String> expectedCaps, Set<String> latestCapabilities) {
        Set<String> allNotFound = new HashSet<>(expectedCaps);
        allNotFound.removeAll(latestCapabilities);
        return allNotFound;
    }

    private static boolean isSubset(Set<String> currentCapabilities, Set<String> expectedCaps) {
        for (String exCap : expectedCaps) {
            if (currentCapabilities.contains(exCap) == false) {
                return false;
            }
        }
        return true;
    }

    private NetconfClientSession get(Future<NetconfClientSession> clientFuture) throws InterruptedException, ConnectFailedException {
        try {
            return clientFuture.get();
        } catch (CancellationException e) {
            logger.debug("Connection attempt cancelled for {}", this, e);
            throw new RuntimeException("Connection cancelled", e);
        } catch (ExecutionException e) {
            throw new ConnectFailedException("Unable to connect " + this, e);
        }
    }

    public Future<NetconfMessage> sendRequest(NetconfMessage message) {
        return ((SimpleNetconfClientSessionListener)sessionListener).sendRequest(message);
    }

    public NetconfMessage sendMessage(NetconfMessage message, int attemptMsDelay) throws InterruptedException,
            SendMessageException {
        Preconditions.checkState(clientSession != null, "Not connected");

        logger.trace("Sending message: {}", XmlUtil.toString(message.getDocument()));
        final Stopwatch stopwatch = new Stopwatch().start();

        try {
            return sendRequest(message).get(attemptMsDelay, TimeUnit.MILLISECONDS);
        } catch (ExecutionException | TimeoutException e) {
            throw new SendMessageException("Unable to send message", message, e);
        } finally {
            stopwatch.stop();
            logger.debug("Total time spent waiting for response from {}: {} ms", netconfServerAddress,
                    stopwatch.elapsed(TimeUnit.MILLISECONDS));
        }
    }

    @Override
    public void close() {
        resetSession();
    }

    private void resetSession() {
        netconfServerAddress = null;

        if(clientSession!=null) {
            clientSession.close();
            clientSession = null;
        }
    }


    @Override
    public String toString() {
        String notConnected = "Not connected";

        final StringBuffer sb = new StringBuffer("ConfigPusherNetconfClient{");
        sb.append("label='").append(label).append('\'');
        sb.append(", netconfServerAddress=").append(netconfServerAddress == null ? notConnected : netconfServerAddress);
        sb.append(", sessionId=").append(clientSession == null ? notConnected : clientSession.getSessionId());
        sb.append('}');
        return sb.toString();
    }

    public long getClientSessionId() {
        Preconditions.checkState(clientSession != null, "Not connected");
        return clientSession.getSessionId();
    }


    public static final class ConnectFailedException extends Exception {
        ConnectFailedException(String message, Exception e) {
            super(message,e);
        }
    }

    public static final class MissingCapabilitiesException extends Exception {

        private final Set<String> serverCapabilities;
        private final Set<String> expectedCapabilities;
        private final Set<String> allNotFound;

        MissingCapabilitiesException(Set<String> serverCapabilities, Set<String> expectedCapabilities,
                                            Set<String> allNotFound) {
            super(String.format(
                    "Capabilities missing from server: %s, capabilities provided by server: %s, all required capabilities: %s",
                    allNotFound, serverCapabilities, expectedCapabilities));
            this.serverCapabilities = serverCapabilities;
            this.expectedCapabilities = expectedCapabilities;
            this.allNotFound = allNotFound;
        }

        public Set<String> getServerCapabilities() {
            return serverCapabilities;
        }

        public Set<String> getExpectedCapabilities() {
            return expectedCapabilities;
        }

        public Set<String> getAllNotFound() {
            return allNotFound;
        }
    }

    public class SendMessageException extends Exception {
        private final NetconfMessage netconfMessage;

        SendMessageException(String message, NetconfMessage netconfMessage, Exception e) {
            super(String.format("%s, netconf mesage: %s", message, XmlUtil.toString(netconfMessage.getDocument())), e);
            this.netconfMessage = netconfMessage;
        }

        public NetconfMessage getNetconfMessage() {
            return netconfMessage;
        }
    }
}
