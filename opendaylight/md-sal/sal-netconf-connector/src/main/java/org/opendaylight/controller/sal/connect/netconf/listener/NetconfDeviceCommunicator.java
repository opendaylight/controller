/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf.listener;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.api.NetconfTerminationReason;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcher;
import org.opendaylight.controller.netconf.client.NetconfClientSession;
import org.opendaylight.controller.netconf.client.NetconfClientSessionListener;
import org.opendaylight.controller.netconf.client.conf.NetconfClientConfiguration;
import org.opendaylight.controller.netconf.client.conf.NetconfReconnectingClientConfiguration;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.controller.sal.connect.api.RemoteDevice;
import org.opendaylight.controller.sal.connect.api.RemoteDeviceCommunicator;
import org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetconfDeviceCommunicator implements NetconfClientSessionListener, RemoteDeviceCommunicator<NetconfMessage> {

    private static final Logger logger = LoggerFactory.getLogger(NetconfDeviceCommunicator.class);

    private final RemoteDevice<NetconfSessionCapabilities, NetconfMessage, NetconfDeviceCommunicator> remoteDevice;
    private final Optional<NetconfSessionCapabilities> overrideNetconfCapabilities;
    private final RemoteDeviceId id;
    private final Lock sessionLock = new ReentrantLock();

    // TODO implement concurrent message limit
    private final Queue<Request> requests = new ArrayDeque<>();
    private NetconfClientSession session;
    private Future<?> initFuture;

    public NetconfDeviceCommunicator(final RemoteDeviceId id, final RemoteDevice<NetconfSessionCapabilities, NetconfMessage, NetconfDeviceCommunicator> remoteDevice,
            final NetconfSessionCapabilities netconfSessionCapabilities) {
        this(id, remoteDevice, Optional.of(netconfSessionCapabilities));
    }

    public NetconfDeviceCommunicator(final RemoteDeviceId id,
                                     final RemoteDevice<NetconfSessionCapabilities, NetconfMessage, NetconfDeviceCommunicator> remoteDevice) {
        this(id, remoteDevice, Optional.<NetconfSessionCapabilities>absent());
    }

    private NetconfDeviceCommunicator(final RemoteDeviceId id, final RemoteDevice<NetconfSessionCapabilities, NetconfMessage, NetconfDeviceCommunicator> remoteDevice,
            final Optional<NetconfSessionCapabilities> overrideNetconfCapabilities) {
        this.id = id;
        this.remoteDevice = remoteDevice;
        this.overrideNetconfCapabilities = overrideNetconfCapabilities;
    }

    @Override
    public void onSessionUp(final NetconfClientSession session) {
        sessionLock.lock();
        try {
            logger.debug("{}: Session established", id);
            this.session = session;

            NetconfSessionCapabilities netconfSessionCapabilities =
                                             NetconfSessionCapabilities.fromNetconfSession(session);
            logger.trace("{}: Session advertised capabilities: {}", id, netconfSessionCapabilities);

            if(overrideNetconfCapabilities.isPresent()) {
                netconfSessionCapabilities = netconfSessionCapabilities.replaceModuleCaps(overrideNetconfCapabilities.get());
                logger.debug("{}: Session capabilities overridden, capabilities that will be used: {}", id, netconfSessionCapabilities);
            }

            remoteDevice.onRemoteSessionUp(netconfSessionCapabilities, this);
        } finally {
            sessionLock.unlock();
        }
    }

    public void initializeRemoteConnection(final NetconfClientDispatcher dispatcher, final NetconfClientConfiguration config) {
        // TODO 2313 extract listener from configuration
        if(config instanceof NetconfReconnectingClientConfiguration) {
            initFuture = dispatcher.createReconnectingClient((NetconfReconnectingClientConfiguration) config);
        } else {
            initFuture = dispatcher.createClient(config);
        }
    }

    public void disconnect() {
        if(session != null) {
            session.close();
        }
    }

    private void tearDown( String reason ) {
        List<UncancellableFuture<RpcResult<NetconfMessage>>> futuresToCancel = Lists.newArrayList();
        sessionLock.lock();
        try {
            if( session != null ) {
                session = null;

                /*
                 * Walk all requests, check if they have been executing
                 * or cancelled and remove them from the queue.
                 */
                final Iterator<Request> it = requests.iterator();
                while (it.hasNext()) {
                    final Request r = it.next();
                    if (r.future.isUncancellable()) {
                        futuresToCancel.add( r.future );
                        it.remove();
                    } else if (r.future.isCancelled()) {
                        // This just does some house-cleaning
                        it.remove();
                    }
                }

                remoteDevice.onRemoteSessionDown();
            }
        }
        finally {
            sessionLock.unlock();
        }

        // Notify pending request futures outside of the sessionLock to avoid unnecessarily
        // blocking the caller.
        for( UncancellableFuture<RpcResult<NetconfMessage>> future: futuresToCancel ) {
            if( Strings.isNullOrEmpty( reason ) ) {
                future.set( createSessionDownRpcResult() );
            } else {
                future.set( createErrorRpcResult( RpcError.ErrorType.TRANSPORT, reason ) );
            }
        }
    }

    private RpcResult<NetconfMessage> createSessionDownRpcResult() {
        return createErrorRpcResult( RpcError.ErrorType.TRANSPORT,
                             String.format( "The netconf session to %1$s is disconnected", id.getName() ) );
    }

    private RpcResult<NetconfMessage> createErrorRpcResult( RpcError.ErrorType errorType, String message ) {
        return RpcResultBuilder.<NetconfMessage>failed()
                .withError(errorType, NetconfDocumentedException.ErrorTag.operation_failed.getTagValue(), message).build();
    }

    @Override
    public void onSessionDown(final NetconfClientSession session, final Exception e) {
        logger.warn("{}: Session went down", id, e);
        tearDown( null );
    }

    @Override
    public void onSessionTerminated(final NetconfClientSession session, final NetconfTerminationReason reason) {
        logger.warn("{}: Session terminated {}", id, reason);
        tearDown( reason.getErrorMessage() );
    }

    @Override
    public void close() {
        // Cancel reconnect if in progress
        if(initFuture != null) {
            initFuture.cancel(false);
        }
        // Disconnect from device
        if(session != null) {
            session.close();
        }

        tearDown(id + ": Netconf session closed");
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

    private void processMessage(final NetconfMessage message) {
        Request request = null;
        sessionLock.lock();

        try {
            request = requests.peek();
            if (request != null && request.future.isUncancellable()) {
                requests.poll();
            } else {
                request = null;
                logger.warn("{}: Ignoring unsolicited message {}", id, msgToS(message));
            }
        }
        finally {
            sessionLock.unlock();
        }

        if( request != null ) {

            logger.debug("{}: Message received {}", id, message);

            if(logger.isTraceEnabled()) {
                logger.trace( "{}: Matched request: {} to response: {}", id, msgToS( request.request ), msgToS( message ) );
            }

            try {
                NetconfMessageTransformUtil.checkValidReply( request.request, message );
            } catch (final NetconfDocumentedException e) {
                logger.warn( "{}: Invalid request-reply match, reply message contains different message-id, request: {}, response: {}",
                             id, msgToS( request.request ), msgToS( message ), e );

                request.future.set( RpcResultBuilder.<NetconfMessage>failed()
                        .withRpcError( NetconfMessageTransformUtil.toRpcError( e ) ).build() );
                return;
            }

            try {
                NetconfMessageTransformUtil.checkSuccessReply(message);
            } catch(final NetconfDocumentedException e) {
                logger.warn( "{}: Error reply from remote device, request: {}, response: {}", id,
                             msgToS( request.request ), msgToS( message ), e );

                request.future.set( RpcResultBuilder.<NetconfMessage>failed()
                        .withRpcError( NetconfMessageTransformUtil.toRpcError( e ) ).build() );
                return;
            }

            request.future.set( RpcResultBuilder.success( message ).build() );
        }
    }

    private static String msgToS(final NetconfMessage msg) {
        return XmlUtil.toString(msg.getDocument());
    }

    @Override
    public ListenableFuture<RpcResult<NetconfMessage>> sendRequest(final NetconfMessage message, final QName rpc) {
        sessionLock.lock();
        try {
            return sendRequestWithLock( message, rpc );
        } finally {
            sessionLock.unlock();
        }
    }

    private ListenableFuture<RpcResult<NetconfMessage>> sendRequestWithLock(
                                               final NetconfMessage message, final QName rpc) {
        if(logger.isTraceEnabled()) {
            logger.trace("{}: Sending message {}", id, msgToS(message));
        }

        if (session == null) {
            logger.warn("{}: Session is disconnected, failing RPC request {}", id, message);
            return Futures.immediateFuture( createSessionDownRpcResult() );
        }

        final Request req = new Request( new UncancellableFuture<RpcResult<NetconfMessage>>(true),
                                         message );
        requests.add(req);

        session.sendMessage(req.request).addListener(new FutureListener<Void>() {
            @Override
            public void operationComplete(final Future<Void> future) throws Exception {
                if( !future.isSuccess() ) {
                    // We expect that a session down will occur at this point
                    logger.debug( "{}: Failed to send request {}", id,
                                  XmlUtil.toString(req.request.getDocument()), future.cause() );

                    if( future.cause() != null ) {
                        req.future.set( createErrorRpcResult( RpcError.ErrorType.TRANSPORT,
                                                              future.cause().getLocalizedMessage() ) );
                    } else {
                        req.future.set( createSessionDownRpcResult() ); // assume session is down
                    }
                    req.future.setException( future.cause() );
                }
                else {
                    logger.trace( "Finished sending request {}", req.request );
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

        private Request(final UncancellableFuture<RpcResult<NetconfMessage>> future,
                        final NetconfMessage request) {
            this.future = future;
            this.request = request;
        }
    }
}
