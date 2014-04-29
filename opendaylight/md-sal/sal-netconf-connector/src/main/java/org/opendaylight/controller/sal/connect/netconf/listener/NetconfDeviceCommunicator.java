/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf.listener;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Iterator;
import java.util.Queue;

import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.api.NetconfTerminationReason;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcher;
import org.opendaylight.controller.netconf.client.NetconfClientSession;
import org.opendaylight.controller.netconf.client.NetconfClientSessionListener;
import org.opendaylight.controller.netconf.client.conf.NetconfClientConfiguration;
import org.opendaylight.controller.netconf.client.conf.NetconfReconnectingClientConfiguration;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.controller.sal.common.util.RpcErrors;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.controller.sal.connect.api.RemoteDevice;
import org.opendaylight.controller.sal.connect.api.RemoteDeviceCommunicator;
import org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil;
import org.opendaylight.controller.sal.connect.util.FailedRpcResult;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

public class NetconfDeviceCommunicator implements NetconfClientSessionListener, RemoteDeviceCommunicator<NetconfMessage> {

    private static final Logger logger = LoggerFactory.getLogger(NetconfDeviceCommunicator.class);

    private static final RpcResult<NetconfMessage> FAILED_RPC_RESULT = new FailedRpcResult<>(RpcErrors.getRpcError(
            null, null, null, RpcError.ErrorSeverity.ERROR, "Netconf session disconnected",
            RpcError.ErrorType.PROTOCOL, null));

    private final RemoteDevice<NetconfSessionCapabilities, NetconfMessage> remoteDevice;
    private final RemoteDeviceId id;

    public NetconfDeviceCommunicator(final RemoteDeviceId id,
            final RemoteDevice<NetconfSessionCapabilities, NetconfMessage> remoteDevice) {
        this.id = id;
        this.remoteDevice = remoteDevice;
    }

    private final Queue<Request> requests = new ArrayDeque<>();
    private NetconfClientSession session;

    @Override
    public synchronized void onSessionUp(final NetconfClientSession session) {
        logger.debug("{}: Session established", id);
        this.session = session;

        final NetconfSessionCapabilities netconfSessionCapabilities = NetconfSessionCapabilities.fromNetconfSession(session);
        logger.trace("{}: Session advertised capabilities: {}", id, netconfSessionCapabilities);

        remoteDevice.onRemoteSessionUp(netconfSessionCapabilities, this);
    }

    public void initializeRemoteConnection(final NetconfClientDispatcher dispatch,
                                           final NetconfReconnectingClientConfiguration config) {
        dispatch.createReconnectingClient(config);
    }

    private synchronized void tearDown(final Exception e) {
        remoteDevice.onRemoteSessionDown();
        session = null;

        /*
         * Walk all requests, check if they have been executing
         * or cancelled and remove them from the queue.
         */
        final Iterator<Request> it = requests.iterator();
        while (it.hasNext()) {
            final Request r = it.next();
            if (r.future.isUncancellable()) {
                r.future.setException(e);
                it.remove();
            } else if (r.future.isCancelled()) {
                // This just does some house-cleaning
                it.remove();
            }
        }
    }

    @Override
    public void onSessionDown(final NetconfClientSession session, final Exception e) {
        logger.warn("{}: Session went down", id, e);
        tearDown(e);
    }

    @Override
    public void onSessionTerminated(final NetconfClientSession session, final NetconfTerminationReason reason) {
        logger.warn("{}: Session terminated {}", id, reason);
        tearDown(new RuntimeException(reason.getErrorMessage()));
    }

    @Override
    public void onMessage(final NetconfClientSession session, final NetconfMessage message) {
        /*
         * Dispatch between notifications and messages. Messages need to be processed
         * with lock held, notifications do not.
         */
        if (isNotification(message)) {
            processNotification(message);
        } else {
            processMessage(message);
        }
    }

    private synchronized void processMessage(final NetconfMessage message) {
        final Request r = requests.peek();
        if (r.future.isUncancellable()) {
            requests.poll();

            logger.debug("{}: Message received {}", id, message);

            if(logger.isTraceEnabled()) {
                logger.trace("{}: Matched request: {} to response: {}", id, msgToS(r.request), msgToS(message));
            }

            try {
                NetconfMessageTransformUtil.checkValidReply(r.request, message);
            } catch (final IllegalStateException e) {
                logger.warn("{}: Invalid request-reply match, reply message contains different message-id, request: {}, response: {}", id,
                        msgToS(r.request), msgToS(message), e);
                r.future.setException(e);
                return;
            }

            try {
                NetconfMessageTransformUtil.checkSuccessReply(message);
            } catch (NetconfDocumentedException | IllegalStateException e) {
                logger.warn("{}: Error reply from remote device, request: {}, response: {}", id,
                        msgToS(r.request), msgToS(message), e);
                r.future.setException(e);
                return;
            }

            r.future.set(Rpcs.getRpcResult(true, message, Collections.<RpcError>emptySet()));
        } else {
            logger.warn("{}: Ignoring unsolicited message {}", id, msgToS(message));
        }
    }

    @Override
    public void close() {
        tearDown(new RuntimeException("Closed"));
    }

    private static String msgToS(final NetconfMessage msg) {
        return XmlUtil.toString(msg.getDocument());
    }

    @Override
    public synchronized ListenableFuture<RpcResult<NetconfMessage>> sendRequest(final NetconfMessage message, final QName rpc) {
        if(logger.isTraceEnabled()) {
            logger.trace("{}: Sending message {}", id, msgToS(message));
        }

        if (session == null) {
            logger.warn("{}: Session is disconnected, failing RPC request {}", id, message);
            return Futures.immediateFuture(FAILED_RPC_RESULT);
        }

        final Request req = new Request(new UncancellableFuture<RpcResult<NetconfMessage>>(true), message, rpc);
        requests.add(req);

        session.sendMessage(req.request).addListener(new FutureListener<Void>() {
            @Override
            public void operationComplete(final Future<Void> future) throws Exception {
                if (!future.isSuccess()) {
                    // We expect that a session down will occur at this point
                    logger.debug("{}: Failed to send request {}", id, XmlUtil.toString(req.request.getDocument()), future.cause());
                    req.future.setException(future.cause());
                } else {
                    logger.trace("{}: Finished sending request {}", id, req.request);
                }
            }
        });

        return req.future;
    }

    private void processNotification(final NetconfMessage notification) {
        logger.debug("{}: Notification received: {}", id, notification);

        if(logger.isTraceEnabled()) {
            logger.trace("{}: Notification received: {}", id, msgToS(notification));
        }

        remoteDevice.onNotification(notification);
    }

    private static boolean isNotification(final NetconfMessage message) {
        final XmlElement xmle = XmlElement.fromDomDocument(message.getDocument());
        return XmlNetconfConstants.NOTIFICATION_ELEMENT_NAME.equals(xmle.getName()) ;
    }

    private static final class Request {
        final UncancellableFuture<RpcResult<NetconfMessage>> future;
        final NetconfMessage request;
        final QName rpc;

        private Request(final UncancellableFuture<RpcResult<NetconfMessage>> future, final NetconfMessage request, final QName rpc) {
            this.future = future;
            this.request = request;
            this.rpc = rpc;
        }
    }
}
