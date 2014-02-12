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
import io.netty.util.concurrent.Promise;

import java.util.ArrayDeque;
import java.util.Queue;

import javax.annotation.concurrent.GuardedBy;

import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.api.NetconfTerminationReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class SimpleNetconfClientSessionListener implements NetconfClientSessionListener {
    private static final class RequestEntry {
        final Promise<NetconfMessage> promise;
        final NetconfMessage request;

        public RequestEntry(Promise<NetconfMessage> future, NetconfMessage request) {
            this.promise = Preconditions.checkNotNull(future);
            this.request = Preconditions.checkNotNull(request);
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(SimpleNetconfClientSessionListener.class);

    @GuardedBy("this")
    private final Queue<RequestEntry> requests = new ArrayDeque<>();

    @GuardedBy("this")
    private NetconfClientSession clientSession;

    @GuardedBy("this")
    private void dispatchRequest() {
        while (!requests.isEmpty()) {
            final RequestEntry e = requests.peek();
            if (e.promise.setUncancellable()) {
                logger.debug("Sending message {}", e.request);
                clientSession.sendMessage(e.request);
                break;
            }

            logger.debug("Message {} has been cancelled, skipping it", e.request);
            requests.poll();
        }
    }

    @Override
    public final synchronized void onSessionUp(NetconfClientSession clientSession) {
        this.clientSession = Preconditions.checkNotNull(clientSession);
        logger.debug("Client session {} went up", clientSession);
        dispatchRequest();
    }

    private synchronized void tearDown(final Exception cause) {
        final RequestEntry e = requests.poll();
        if (e != null) {
            e.promise.setFailure(cause);
        }

        this.clientSession = null;
    }

    @Override
    public final void onSessionDown(NetconfClientSession clientSession, Exception e) {
        logger.debug("Client Session {} went down unexpectedly", clientSession, e);
        tearDown(e);
    }

    @Override
    public final void onSessionTerminated(NetconfClientSession clientSession,
            NetconfTerminationReason netconfTerminationReason) {
        logger.debug("Client Session {} terminated, reason: {}", clientSession,
                netconfTerminationReason.getErrorMessage());
        tearDown(new RuntimeException(netconfTerminationReason.getErrorMessage()));
    }

    @Override
    public synchronized void onMessage(NetconfClientSession session, NetconfMessage message) {
        logger.debug("New message arrived: {}", message);

        final RequestEntry e = requests.poll();
        if (e != null) {
            e.promise.setSuccess(message);
            dispatchRequest();
        } else {
            logger.info("Ignoring unsolicited message {}", message);
        }
    }

    final synchronized Future<NetconfMessage> sendRequest(NetconfMessage message) {
        final RequestEntry req = new RequestEntry(GlobalEventExecutor.INSTANCE.<NetconfMessage>newPromise(), message);

        requests.add(req);
        if (clientSession != null) {
            dispatchRequest();
        }

        return req.promise;
    }
}
