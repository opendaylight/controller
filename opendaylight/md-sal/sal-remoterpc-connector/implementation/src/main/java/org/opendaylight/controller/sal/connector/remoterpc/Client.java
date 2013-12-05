/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connector.remoterpc;

import com.google.common.base.Optional;

import org.opendaylight.controller.sal.common.util.RpcErrors;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.controller.sal.connector.api.RpcRouter;
import org.opendaylight.controller.sal.connector.remoterpc.api.RoutingTable;
import org.opendaylight.controller.sal.connector.remoterpc.dto.Message;
import org.opendaylight.controller.sal.connector.remoterpc.dto.MessageWrapper;
import org.opendaylight.controller.sal.connector.remoterpc.dto.RouteIdentifierImpl;
import org.opendaylight.controller.sal.connector.remoterpc.util.XmlUtils;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static com.google.common.base.Preconditions.*;

/**
 * An implementation of {@link RpcImplementation} that makes remote RPC calls
 */
public class Client implements RemoteRpcClient {

    private final Logger _logger = LoggerFactory.getLogger(Client.class);

    private final LinkedBlockingQueue<MessageWrapper> requestQueue = new LinkedBlockingQueue<MessageWrapper>(100);

    private final ExecutorService pool = Executors.newSingleThreadExecutor();
    private final long TIMEOUT = 5000; // in ms

    private  RoutingTableProvider routingTableProvider;

    public RoutingTableProvider getRoutingTableProvider() {
        return routingTableProvider;
    }

    public void setRoutingTableProvider(RoutingTableProvider routingTableProvider) {
        this.routingTableProvider = routingTableProvider;
    }

    public LinkedBlockingQueue<MessageWrapper> getRequestQueue() {
        return requestQueue;
    }

    @Override
    public Set<QName> getSupportedRpcs() {
        // TODO: Find the entries from routing table
        return Collections.emptySet();
    }

    public void start() {
        pool.execute(new Sender(this));

    }

    public void stop() {

        _logger.debug("Client stopping...");
        Context.getInstance().getZmqContext().term();
        _logger.debug("ZMQ context terminated");

        pool.shutdown(); // intiate shutdown
        try {
            if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                pool.shutdownNow();
                if (!pool.awaitTermination(10, TimeUnit.SECONDS))
                    _logger.error("Client thread pool did not shut down");
            }
        } catch (InterruptedException e) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
        _logger.debug("Client stopped");
    }

    @Override
    public RpcResult<CompositeNode> invokeRpc(QName rpc, CompositeNode input) {

        RouteIdentifierImpl routeId = new RouteIdentifierImpl();
        routeId.setType(rpc);

        String address = lookupRemoteAddress(routeId);

        Message request = new Message.MessageBuilder().type(Message.MessageType.REQUEST)
                .sender(Context.getInstance().getLocalUri()).recipient(address).route(routeId)
                .payload(XmlUtils.compositeNodeToXml(input)).build();

        List<RpcError> errors = new ArrayList<RpcError>();

        try (SocketPair pair = new SocketPair()) {

            MessageWrapper messageWrapper = new MessageWrapper(request, pair.getSender());
            process(messageWrapper);
            Message response = parseMessage(pair.getReceiver());

            CompositeNode payload = XmlUtils.xmlToCompositeNode((String) response.getPayload());

            return Rpcs.getRpcResult(true, payload, errors);

        } catch (Exception e) {
            collectErrors(e, errors);
            return Rpcs.getRpcResult(false, null, errors);
        }

    }

    public void process(MessageWrapper msg) throws TimeoutException, InterruptedException {
        _logger.debug("Processing message [{}]", msg);

        boolean success = requestQueue.offer(msg, TIMEOUT, TimeUnit.MILLISECONDS);
        if (!success)
            throw new TimeoutException("Queue is full");
    }

    /**
     * Block on socket for reply
     * 
     * @param receiver
     * @return
     */
    private Message parseMessage(ZMQ.Socket receiver) throws IOException, ClassNotFoundException {
        return (Message) Message.deserialize(receiver.recv());
    }

    /**
     * Find address for the given route identifier in routing table
     * 
     * @param routeId
     *            route identifier
     * @return remote network address
     */
    private String lookupRemoteAddress(RpcRouter.RouteIdentifier routeId) {
        checkNotNull(routeId, "route must not be null");

        Optional<RoutingTable<String, String>> routingTable = routingTableProvider.getRoutingTable();
        checkNotNull(routingTable.isPresent(), "Routing table is null");

        Set<String> addresses = routingTable.get().getRoutes(routeId.toString());
        checkNotNull(addresses, "Address not found for route [%s]", routeId);
        checkState(addresses.size() == 1, "Multiple remote addresses found for route [%s], \nonly 1 expected", routeId); // its
                                                                                                                         // a
                                                                                                                         // global
                                                                                                                         // service.

        String address = addresses.iterator().next();
        checkNotNull(address, "Address not found for route [%s]", routeId);

        return address;
    }

    private void collectErrors(Exception e, List<RpcError> errors) {
        if (e == null)
            return;
        if (errors == null)
            errors = new ArrayList<RpcError>();

        errors.add(RpcErrors.getRpcError(null, null, null, null, e.getMessage(), null, e.getCause()));
        for (Throwable t : e.getSuppressed()) {
            errors.add(RpcErrors.getRpcError(null, null, null, null, t.getMessage(), null, t));
        }
    }

    @Override
    public void close() throws Exception {
        stop();
    }
}
