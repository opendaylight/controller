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
import org.opendaylight.controller.sal.connector.remoterpc.api.RoutingTableException;
import org.opendaylight.controller.sal.connector.remoterpc.api.SystemException;
import org.opendaylight.controller.sal.connector.remoterpc.dto.Message;
import org.opendaylight.controller.sal.connector.remoterpc.dto.RouteIdentifierImpl;
import org.opendaylight.controller.sal.connector.remoterpc.util.XmlUtils;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * An implementation of {@link RpcImplementation} that makes
 * remote RPC calls
 */
public class ClientImpl implements RemoteRpcClient {

  private final Logger _logger = LoggerFactory.getLogger(ClientImpl.class);

  private ZMQ.Context context = ZMQ.context(1);
  private ClientRequestHandler handler;
  private RoutingTableProvider routingTableProvider;

  public ClientImpl(){
    handler = new ClientRequestHandler(context);
    start();
  }

  public ClientImpl(ClientRequestHandler handler){
    this.handler = handler;
    start();
  }

  public RoutingTableProvider getRoutingTableProvider() {
    return routingTableProvider;
  }

  public void setRoutingTableProvider(RoutingTableProvider routingTableProvider) {
    this.routingTableProvider = routingTableProvider;
  }

  @Override
  public void start() {/*NOOPS*/}

  @Override
  public void stop() {
    closeZmqContext();
    handler.close();
    _logger.info("Stopped");
  }

  @Override
  public void close(){
    stop();
  }

  /**
   * Finds remote server that can execute this rpc and sends a message to it
   * requesting execution.
   * The call blocks until a response from remote server is received. Its upto
   * the client of this API to implement a timeout functionality.
   *
   * @param rpc   remote service to be executed
   * @param input payload for the remote service
   * @return
   */
  public RpcResult<CompositeNode> invokeRpc(QName rpc, CompositeNode input) {
    RouteIdentifierImpl routeId = new RouteIdentifierImpl();
    routeId.setType(rpc);

    String address = lookupRemoteAddressForGlobalRpc(routeId);
    return sendMessage(input, routeId, address);
  }

  /**
   * Finds remote server that can execute this routed rpc and sends a message to it
   * requesting execution.
   * The call blocks until a response from remote server is received. Its upto
   * the client of this API to implement a timeout functionality.
   *
   * @param rpc
   *          rpc to be called
   * @param identifier
   *          instance identifier on which rpc is to be executed
   * @param input
   *          payload
   * @return
   */
  public RpcResult<CompositeNode> invokeRpc(QName rpc, InstanceIdentifier identifier, CompositeNode input) {

    RouteIdentifierImpl routeId = new RouteIdentifierImpl();
    routeId.setType(rpc);
    routeId.setRoute(identifier);

    String address = lookupRemoteAddressForRpc(routeId);

    return sendMessage(input, routeId, address);
  }

  private RpcResult<CompositeNode> sendMessage(CompositeNode input, RouteIdentifierImpl routeId, String address) {
    Message request = new Message.MessageBuilder()
        .type(Message.MessageType.REQUEST)
        .sender(Context.getInstance().getLocalUri())
        .recipient(address)
        .route(routeId)
        .payload(XmlUtils.compositeNodeToXml(input))
        .build();

    List<RpcError> errors = new ArrayList<RpcError>();

    try{
      Message response = handler.handle(request);
      CompositeNode payload = null;

      if ( response != null )
        payload = XmlUtils.xmlToCompositeNode((String) response.getPayload());

      return Rpcs.getRpcResult(true, payload, errors);

    } catch (Exception e){
      collectErrors(e, errors);
      return Rpcs.getRpcResult(false, null, errors);
    }
  }

  /**
   * Find address for the given route identifier in routing table
   * @param  routeId route identifier
   * @return         remote network address
   */
  private String lookupRemoteAddressForGlobalRpc(RpcRouter.RouteIdentifier routeId){
    checkNotNull(routeId, "route must not be null");

    Optional<RoutingTable<RpcRouter.RouteIdentifier, String>> routingTable = routingTableProvider.getRoutingTable();
    checkNotNull(routingTable.isPresent(), "Routing table is null");

    String address = null;
    try {
      address = routingTable.get().getGlobalRoute(routeId);
    } catch (RoutingTableException|SystemException e) {
      _logger.error("Exception caught while looking up remote address " + e);
    }
    checkState(address != null, "Address not found for route [%s]", routeId);

    return address;
  }

  /**
   * Find address for the given route identifier in routing table
   * @param  routeId route identifier
   * @return         remote network address
   */
  private String lookupRemoteAddressForRpc(RpcRouter.RouteIdentifier routeId){
    checkNotNull(routeId, "route must not be null");

    Optional<RoutingTable<RpcRouter.RouteIdentifier, String>> routingTable = routingTableProvider.getRoutingTable();
    checkNotNull(routingTable.isPresent(), "Routing table is null");

    String address = routingTable.get().getLastAddedRoute(routeId);
    checkState(address != null, "Address not found for route [%s]", routeId);

    return address;
  }

  private void collectErrors(Exception e, List<RpcError> errors){
    if (e == null) return;
    if (errors == null) errors = new ArrayList<RpcError>();

    errors.add(RpcErrors.getRpcError(null, null, null, null, e.getMessage(), null, e.getCause()));
    for (Throwable t : e.getSuppressed()) {
      errors.add(RpcErrors.getRpcError(null, null, null, null, t.getMessage(), null, t));
    }
  }

  /**
   * Closes ZMQ Context. It tries to gracefully terminate the context. If
   * termination takes more than a second, its forcefully shutdown.
   */
  private void closeZmqContext() {
    ExecutorService exec = Executors.newSingleThreadExecutor();
    FutureTask zmqTermination = new FutureTask(new Runnable() {

      @Override
      public void run() {
        try {
          if (context != null)
            context.term();
          _logger.debug("ZMQ Context terminated");
        } catch (Exception e) {
          _logger.debug("ZMQ Context termination threw exception [{}]. Continuing shutdown...", e);
        }
      }
    }, null);

    exec.execute(zmqTermination);

    try {
      zmqTermination.get(1L, TimeUnit.SECONDS);
    } catch (Exception e) {/*ignore and continue with shutdown*/}

    exec.shutdownNow();
  }
}
