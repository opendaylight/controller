/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connector.remoterpc;

import com.google.common.base.Optional;

import org.opendaylight.controller.sal.connector.remoterpc.api.RouteChangeListener;
import org.opendaylight.controller.sal.connector.remoterpc.api.RoutingTable;
import org.opendaylight.controller.sal.connector.remoterpc.api.RoutingTableException;
import org.opendaylight.controller.sal.connector.remoterpc.api.SystemException;
import org.opendaylight.controller.sal.connector.remoterpc.dto.Message;
import org.opendaylight.controller.sal.connector.remoterpc.dto.Message.MessageType;
import org.opendaylight.controller.sal.connector.remoterpc.dto.RouteIdentifierImpl;
import org.opendaylight.controller.sal.connector.remoterpc.util.XmlUtils;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.controller.sal.core.api.RpcRegistrationListener;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * ZeroMq based implementation of RpcRouter TODO: 1. Make rpc request handling
 * async and non-blocking. Note zmq socket is not thread safe 2. Read properties
 * from config file using existing(?) ODL properties framework
 */
public class ServerImpl implements RemoteRpcServer, RouteChangeListener<String, Set> {

    private Logger _logger = LoggerFactory.getLogger(ServerImpl.class);

    private ExecutorService serverPool;

    // private RoutingTable<RpcRouter.RouteIdentifier, String> routingTable;
    private RoutingTableProvider routingTable;
    private Set<QName> remoteServices;
    private ProviderSession brokerSession;
    private ZMQ.Context context;
    private ZMQ.Socket replySocket;

    private final RpcListener listener = new RpcListener();

    private final String localUri = Context.getInstance().getLocalUri();

    private final int rpcPort;

    private RpcImplementation client;

    public RpcImplementation getClient() {
        return client;
    }

    public void setClient(RpcImplementation client) {
        this.client = client;
    }

    // Prevent instantiation
    public ServerImpl(int rpcPort) {
        this.rpcPort = rpcPort;
    }

    public void setBrokerSession(ProviderSession session) {
        this.brokerSession = session;
    }

    public ExecutorService getServerPool() {
        return serverPool;
    }

    public void setServerPool(ExecutorService serverPool) {
        this.serverPool = serverPool;
    }

    public void start() {
        context = ZMQ.context(1);
        serverPool = Executors.newSingleThreadExecutor();
        remoteServices = new HashSet<QName>();

        // Start listening rpc requests
        serverPool.execute(receive());

        brokerSession.addRpcRegistrationListener(listener);
        // routingTable.registerRouteChangeListener(routeChangeListener);

        Set<QName> currentlySupported = brokerSession.getSupportedRpcs();
        for (QName rpc : currentlySupported) {
            listener.onRpcImplementationAdded(rpc);
        }

        _logger.debug("RPC Server started [{}]", localUri);
    }

    public void stop() {
        // TODO: un-subscribe

        // if (context != null)
        // context.term();
        //
        // _logger.debug("ZMQ Context is terminated.");

        if (serverPool != null)
            serverPool.shutdown();

        _logger.debug("Thread pool is closed.");
    }

    private Runnable receive() {
        return new Runnable() {
            public void run() {

                // Bind to RPC reply socket
                replySocket = context.socket(ZMQ.REP);
                replySocket.bind("tcp://*:" + Context.getInstance().getRpcPort());

                // Poller enables listening on multiple sockets using a single
                // thread
                ZMQ.Poller poller = new ZMQ.Poller(1);
                poller.register(replySocket, ZMQ.Poller.POLLIN);
                try {
                    // TODO: Add code to restart the thread after exception
                    while (!Thread.currentThread().isInterrupted()) {

                        poller.poll();

                        if (poller.pollin(0)) {
                            handleRpcCall();
                        }
                    }
                } catch (Exception e) {
                    // log and continue
                    _logger.error("Unhandled exception [{}]", e);
                } finally {
                    poller.unregister(replySocket);
                    replySocket.close();
                }

            }
        };
    }

    /**
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private void handleRpcCall() {

        Message request = parseMessage(replySocket);

        _logger.debug("Received rpc request [{}]", request);

        // Call broker to process the message then reply
        Future<RpcResult<CompositeNode>> rpc = null;
        RpcResult<CompositeNode> result = null;
        try {
            rpc = brokerSession.rpc((QName) request.getRoute().getType(),
                    XmlUtils.xmlToCompositeNode((String) request.getPayload()));

            result = (rpc != null) ? rpc.get() : null;

        } catch (Exception e) {
            _logger.debug("Broker threw  [{}]", e);
        }

        CompositeNode payload = (result != null) ? result.getResult() : null;

        Message response = new Message.MessageBuilder().type(MessageType.RESPONSE).sender(localUri)
                .route(request.getRoute()).payload(XmlUtils.compositeNodeToXml(payload)).build();

        _logger.debug("Sending rpc response [{}]", response);

        try {
            replySocket.send(Message.serialize(response));
        } catch (Exception e) {
            _logger.debug("rpc response send failed for message [{}]", response);
            _logger.debug("{}", e);
        }

    }

    /**
     * @param socket
     * @return
     */
    private Message parseMessage(ZMQ.Socket socket) {

        Message msg = null;
        try {
            byte[] bytes = socket.recv();
            _logger.debug("Received bytes:[{}]", bytes.length);
            msg = (Message) Message.deserialize(bytes);
        } catch (Throwable t) {
            _logger.warn("Unhanded Exception ", t);
        }
        return msg;
    }

    @Override
    public void onRouteUpdated(String key, Set values) {
        RouteIdentifierImpl rId = new RouteIdentifierImpl();
        try {
            _logger.debug("Updating key/value {}-{}", key, values);
            brokerSession.addRpcImplementation((QName) rId.fromString(key).getType(), client);

        } catch (Exception e) {
            _logger.trace("Route update failed {}", e);
        }
    }

    @Override
    public void onRouteDeleted(String key) {
        // TODO: Broker session needs to be updated to support this
        throw new UnsupportedOperationException();
    }

    /**
     * Listener for rpc registrations
     */
    private class RpcListener implements RpcRegistrationListener {



        @Override
        public void onRpcImplementationAdded(QName name) {

            // if the service name exists in the set, this notice
            // has bounced back from the broker. It should be ignored
            if (remoteServices.contains(name))
                return;

            _logger.debug("Adding registration for [{}]", name);
            RouteIdentifierImpl routeId = new RouteIdentifierImpl();
            routeId.setType(name);

            try {
                routingTable.getRoutingTable().get().addGlobalRoute(routeId.toString(), localUri);
                _logger.debug("Route added [{}-{}]", name, localUri);
            } catch (RoutingTableException | SystemException e) {
                // TODO: This can be thrown when route already exists in the
                // table. Broker
                // needs to handle this.
                _logger.error("Unhandled exception while adding global route to routing table [{}]", e);

            }
        }

        @Override
        public void onRpcImplementationRemoved(QName name) {

            _logger.debug("Removing registration for [{}]", name);
            RouteIdentifierImpl routeId = new RouteIdentifierImpl();
            routeId.setType(name);

            try {
                routingTable.getRoutingTable().get().removeGlobalRoute(routeId.toString());
            } catch (RoutingTableException | SystemException e) {
                _logger.error("Route delete failed {}", e);
            }
        }
    }

    @Override
    public void close() throws Exception {
        stop();
    }

    public void setRoutingTableProvider(RoutingTableProvider provider) {
        this.routingTable = provider;
    }

}
