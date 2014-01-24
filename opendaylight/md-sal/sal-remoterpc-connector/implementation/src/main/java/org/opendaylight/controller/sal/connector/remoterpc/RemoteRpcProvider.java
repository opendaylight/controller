/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connector.remoterpc;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChange;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChangeListener;
import org.opendaylight.controller.sal.connector.api.RpcRouter;
import org.opendaylight.controller.sal.connector.remoterpc.api.RoutingTable;
import org.opendaylight.controller.sal.connector.remoterpc.api.RoutingTableException;
import org.opendaylight.controller.sal.connector.remoterpc.api.SystemException;
import org.opendaylight.controller.sal.connector.remoterpc.dto.RouteIdentifierImpl;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.RoutedRpcDefaultImplementation;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.controller.sal.core.api.RpcProvisionRegistry;
import org.opendaylight.controller.sal.core.api.RpcRegistrationListener;
import org.opendaylight.controller.sal.core.api.RpcRoutingContext;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class RemoteRpcProvider implements
    RpcImplementation,
    RoutedRpcDefaultImplementation,
    AutoCloseable,
    Provider {

  private Logger _logger = LoggerFactory.getLogger(RemoteRpcProvider.class);

  private final ServerImpl server;
  private final ClientImpl client;
  private RoutingTableProvider routingTableProvider;
  private final RpcListener listener = new RpcListener();
  private final RoutedRpcListener routeChangeListener = new RoutedRpcListener();
  private ProviderSession brokerSession;
  private RpcProvisionRegistry rpcProvisionRegistry;
  private BundleContext context;
  private ServiceTracker clusterTracker;

  public RemoteRpcProvider(ServerImpl server, ClientImpl client) {
    this.server = server;
    this.client = client;
  }

  public void setRoutingTableProvider(RoutingTableProvider provider) {
    this.routingTableProvider = provider;
    client.setRoutingTableProvider(provider);
  }

  public void setContext(BundleContext context){
    this.context = context;
  }

  public void setRpcProvisionRegistry(RpcProvisionRegistry rpcProvisionRegistry){
    this.rpcProvisionRegistry = rpcProvisionRegistry;
  }

  @Override
  public void onSessionInitiated(ProviderSession session) {
    brokerSession = session;
    server.setBrokerSession(session);
    start();
  }

  @Override
  public Set<QName> getSupportedRpcs() {
    //TODO: Ask Tony if we need to get this from routing table
    return Collections.emptySet();
  }

  @Override
  public Collection<ProviderFunctionality> getProviderFunctionality() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public RpcResult<CompositeNode> invokeRpc(QName rpc, CompositeNode input) {
    return client.invokeRpc(rpc, input);
  }

  @Override
  public RpcResult<CompositeNode> invokeRpc(QName rpc, InstanceIdentifier identifier, CompositeNode input) {
    return client.invokeRpc(rpc, identifier, input);
  }

  public void start() {
    server.start();
    client.start();
    brokerSession.addRpcRegistrationListener(listener);
    rpcProvisionRegistry.setRoutedRpcDefaultDelegate(this);
    rpcProvisionRegistry.registerRouteChangeListener(routeChangeListener);

    announceSupportedRpcs();
    announceSupportedRoutedRpcs();
  }

  @Override
  public void close() throws Exception {
    unregisterSupportedRpcs();
    unregisterSupportedRoutedRpcs();
    server.close();
    client.close();
  }

  public void stop() {
    server.stop();
    client.stop();
  }

  /**
   * Add all the locally registered RPCs in the clustered routing table
   */
  private void announceSupportedRpcs(){
    Set<QName> currentlySupported = brokerSession.getSupportedRpcs();
    for (QName rpc : currentlySupported) {
      listener.onRpcImplementationAdded(rpc);
    }
  }

  /**
   * Add all the locally registered Routed RPCs in the clustered routing table
   */
  private void announceSupportedRoutedRpcs(){

    //TODO: announce all routed RPCs as well

  }

  /**
   * Un-Register all the supported RPCs from clustered routing table
   */
  private void unregisterSupportedRpcs(){
    Set<QName> currentlySupported = brokerSession.getSupportedRpcs();
    //TODO: remove all routed RPCs as well
    for (QName rpc : currentlySupported) {
      listener.onRpcImplementationRemoved(rpc);
    }
  }

  /**
   * Un-Register all the locally supported Routed RPCs from clustered routing table
   */
  private void unregisterSupportedRoutedRpcs(){

    //TODO: remove all routed RPCs as well

  }

  private RoutingTable<RpcRouter.RouteIdentifier, String> getRoutingTable(){
    Optional<RoutingTable<RpcRouter.RouteIdentifier, String>> routingTable =
        routingTableProvider.getRoutingTable();

    checkNotNull(routingTable.isPresent(), "Routing table is null");

    return routingTable.get();
  }

  /**
   * Listener for rpc registrations in broker
   */
  private class RpcListener implements RpcRegistrationListener {

    @Override
    public void onRpcImplementationAdded(QName rpc) {

      _logger.debug("Adding registration for [{}]", rpc);
      RouteIdentifierImpl routeId = new RouteIdentifierImpl();
      routeId.setType(rpc);

      RoutingTable<RpcRouter.RouteIdentifier, String> routingTable = getRoutingTable();

      try {
        routingTable.addGlobalRoute(routeId, server.getServerAddress());
        _logger.debug("Route added [{}-{}]", routeId, server.getServerAddress());

      } catch (RoutingTableException | SystemException e) {
        //TODO: This can be thrown when route already exists in the table. Broker
        //needs to handle this.
        _logger.error("Unhandled exception while adding global route to routing table [{}]", e);

      }
    }

    @Override
    public void onRpcImplementationRemoved(QName rpc) {

      _logger.debug("Removing registration for [{}]", rpc);
      RouteIdentifierImpl routeId = new RouteIdentifierImpl();
      routeId.setType(rpc);

      RoutingTable<RpcRouter.RouteIdentifier, String> routingTable = getRoutingTable();

      try {
        routingTable.removeGlobalRoute(routeId);
      } catch (RoutingTableException | SystemException e) {
        _logger.error("Route delete failed {}", e);
      }
    }
  }

  /**
   * Listener for Routed Rpc registrations in broker
   */
  private class RoutedRpcListener
      implements RouteChangeListener<RpcRoutingContext, InstanceIdentifier> {

    /**
     *
     * @param routeChange
     */
    @Override
    public void onRouteChange(RouteChange<RpcRoutingContext, InstanceIdentifier> routeChange) {
      Map<RpcRoutingContext, Set<InstanceIdentifier>> announcements = routeChange.getAnnouncements();
      announce(getRouteIdentifiers(announcements));

      Map<RpcRoutingContext, Set<InstanceIdentifier>> removals = routeChange.getRemovals();
      remove(getRouteIdentifiers(removals));
    }

    /**
     *
     * @param announcements
     */
    private void announce(Set<RpcRouter.RouteIdentifier> announcements) {
      _logger.debug("Announcing [{}]", announcements);
      RoutingTable<RpcRouter.RouteIdentifier, String> routingTable = getRoutingTable();
      try {
        routingTable.addRoutes(announcements, server.getServerAddress());
      } catch (RoutingTableException | SystemException e) {
        _logger.error("Route announcement failed {}", e);
      }
    }

    /**
     *
     * @param removals
     */
    private void remove(Set<RpcRouter.RouteIdentifier> removals){
      _logger.debug("Removing [{}]", removals);
      RoutingTable<RpcRouter.RouteIdentifier, String> routingTable = getRoutingTable();
      try {
        routingTable.removeRoutes(removals, server.getServerAddress());
      } catch (RoutingTableException | SystemException e) {
        _logger.error("Route removal failed {}", e);
      }
    }

    /**
     *
     * @param changes
     * @return
     */
    private Set<RpcRouter.RouteIdentifier> getRouteIdentifiers(Map<RpcRoutingContext, Set<InstanceIdentifier>> changes) {
      RouteIdentifierImpl routeId = null;
      Set<RpcRouter.RouteIdentifier> routeIdSet = new HashSet<RpcRouter.RouteIdentifier>();

      for (RpcRoutingContext context : changes.keySet()){
        routeId = new RouteIdentifierImpl();
        routeId.setType(context.getRpc());
        routeId.setContext(context.getContext());

        for (InstanceIdentifier instanceId : changes.get(context)){
          routeId.setRoute(instanceId);
          routeIdSet.add(routeId);
        }
      }
      return routeIdSet;
    }



  }

}
