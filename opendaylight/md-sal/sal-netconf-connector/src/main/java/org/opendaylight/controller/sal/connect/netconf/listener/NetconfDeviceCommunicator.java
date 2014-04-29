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
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.controller.sal.common.util.RpcErrors;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.controller.sal.connect.api.RemoteDevice;
import org.opendaylight.controller.sal.connect.api.RemoteDeviceCommunicator;
import org.opendaylight.controller.sal.connect.api.RemoteDeviceNotificationListener;
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

    private static final Logger LOG = LoggerFactory.getLogger(NetconfDeviceCommunicator.class);

    private static final RpcResult<NetconfMessage> FAILED_RPC_RESULT = new FailedRpcResult<>(RpcErrors.getRpcError(
            null, null, null, RpcError.ErrorSeverity.ERROR, "Netconf session disconnected",
            RpcError.ErrorType.PROTOCOL, null));

    private final RemoteDeviceId deviceId;
    private final RemoteDeviceNotificationListener<NetconfMessage> notificationListener;
    private final RemoteDevice<NetconfSessionCapabilities, NetconfMessage> remoteDevice;

    public NetconfDeviceCommunicator(final RemoteDeviceId id,
            final RemoteDeviceNotificationListener<NetconfMessage> notificationListener,
            final RemoteDevice<NetconfSessionCapabilities, NetconfMessage> remoteDevice) {
        this.deviceId = id;
        this.notificationListener = notificationListener;
        this.remoteDevice = remoteDevice;
    }

    private final Queue<Request> requests = new ArrayDeque<>();
    private NetconfClientSession session;

    @Override
    public synchronized void onSessionUp(final NetconfClientSession session) {
        LOG.debug("{}: Session established", deviceId);
        this.session = session;

        final NetconfSessionCapabilities netconfSessionCapabilities = NetconfSessionCapabilities.fromNetconfSession(session);
        LOG.trace("{}: Session advertised capabilities {}", deviceId, netconfSessionCapabilities);

        remoteDevice.onRemoteSessionInitialized(netconfSessionCapabilities, this);
    }

    public void initializeRemoteConnection(final NetconfClientDispatcher dispatch,
                                           final NetconfClientConfiguration config) {
        dispatch.createClient(config);
    }

    private synchronized void tearDown(final Exception e) {
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

        remoteDevice.onRemoteSessionDown();
    }

    @Override
    public void onSessionDown(final NetconfClientSession session, final Exception e) {
        LOG.debug("{}: Session went down", deviceId, e);
        tearDown(e);
    }

    @Override
    public void onSessionTerminated(final NetconfClientSession session, final NetconfTerminationReason reason) {
        LOG.debug("{}: Session terminated", deviceId, reason);
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
            LOG.trace("{}: Matched {} to {}", deviceId, r.request, message);

            try {
                NetconfMessageTransformUtil.checkValidReply(r.request, message);
            } catch (final IllegalStateException e) {
                // FIXME add request message
                LOG.warn("{}: Invalid request-reply match, reply message contains different message-id", deviceId, e);
                r.future.setException(e);
                return;
            }

            try {
                NetconfMessageTransformUtil.checkSuccessReply(message);
            } catch (NetconfDocumentedException | IllegalStateException e) {
                // FIXME add request message
                LOG.warn("{}: Error reply from remote device", deviceId, e);
                r.future.setException(e);
                return;
            }

            r.future.set(Rpcs.getRpcResult(true, message, Collections.<RpcError>emptySet()));
        } else {
            LOG.warn("{}: Ignoring unsolicited message {}", deviceId, message);
        }
    }

    @Override
    public synchronized ListenableFuture<RpcResult<NetconfMessage>> sendRequest(final NetconfMessage message, final QName rpc) {
        if (session == null) {
            LOG.warn("{}: Session is disconnected, failing RPC request {}", deviceId, message);
            return Futures.immediateFuture(FAILED_RPC_RESULT);
        }

        final Request req = new Request(new UncancellableFuture<RpcResult<NetconfMessage>>(true), message, rpc);
        requests.add(req);

        session.sendMessage(req.request).addListener(new FutureListener<Void>() {
            @Override
            public void operationComplete(final Future<Void> future) throws Exception {
                if (!future.isSuccess()) {
                    // We expect that a session down will occur at this point
                    LOG.debug("{}: Failed to send request {}", deviceId, XmlUtil.toString(req.request.getDocument()), future.cause());
                    req.future.setException(future.cause());
                } else {
                    LOG.trace("{}: Finished sending request {}", deviceId, req.request);
                }
            }
        });

        return req.future;
    }

    /**
     * Process an incoming notification.
     *
     * @param notification Notification message
     */
    private void processNotification(final NetconfMessage notification) {
        LOG.trace("{}: Received netconf notification {}", deviceId, notification);
        notificationListener.onNotification(notification);
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
