/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc;


import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import org.opendaylight.controller.sal.connector.api.RpcRouter;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.RpcProvisionRegistry;
import org.opendaylight.yangtools.yang.common.QName;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;

public class RemoteRpcProvider implements AutoCloseable, Provider{

  private static final Logger LOG = LoggerFactory.getLogger(RemoteRpcProvider.class);

  private final ActorSystem actorSystem;
  private ActorRef rpcBroker;
  private ActorRef rpcRegistry;
  private final RoutingTable<RpcRouter.RouteIdentifier<?, ?, ?>, String> routingTable;
  private BundleContext context;
  private RpcProvisionRegistry rpcProvisionRegistry;
  private Broker.ProviderSession brokerSession;
  private RpcListener rpcListener;
  private RoutedRpcListener routeChangeListener;
  private RemoteRpcImplementation rpcImplementation;
  private ClusterHandler clusterHandler;
  public RemoteRpcProvider(ActorSystem actorSystem) {
    this.routingTable = new RoutingTable();
    this.actorSystem = actorSystem;
    this.clusterHandler = new ClusterHandler(actorSystem);
  }

  public void setContext(BundleContext context) {
    this.context = context;
  }

  public void setRpcProvisionRegistry(RpcProvisionRegistry rpcProvisionRegistry) {
    this.rpcProvisionRegistry = rpcProvisionRegistry;
  }

  @Override
  public void close() throws Exception {
    this.actorSystem.shutdown();
    unregisterSupportedRpcs();
    unregisterSupportedRoutedRpcs();
  }

  @Override
  public void onSessionInitiated(Broker.ProviderSession session) {
    this.brokerSession = session;
    start();
  }

  @Override
  public Collection<ProviderFunctionality> getProviderFunctionality() {
    return null;
  }

  private void start() {
    LOG.debug("Starting all rpc listeners.");
    rpcBroker = actorSystem.actorOf(RpcBroker.props(routingTable, brokerSession), "rpcBroker");
    rpcRegistry = actorSystem.actorOf(RpcRegistry.props(routingTable, clusterHandler), "rpcRegistry");
    String rpcBrokerPath = rpcBroker.path().toString();
    rpcListener = new RpcListener(routingTable, rpcBrokerPath);
    routeChangeListener = new RoutedRpcListener(routingTable, rpcBrokerPath);
    rpcImplementation = new RemoteRpcImplementation(rpcBroker);
    brokerSession.addRpcRegistrationListener(rpcListener);
    rpcProvisionRegistry.registerRouteChangeListener(routeChangeListener);
    rpcProvisionRegistry.setRoutedRpcDefaultDelegate(rpcImplementation);
    announceSupportedRpcs();
    announceSupportedRoutedRpcs();
  }

  /**
   * Add all the locally registered RPCs in the clustered routing table
   */
  private void announceSupportedRpcs(){
    LOG.debug("Adding all supported rpcs to routing table");
    Set<QName> currentlySupported = brokerSession.getSupportedRpcs();
    for (QName rpc : currentlySupported) {
      rpcListener.onRpcImplementationAdded(rpc);
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
    LOG.debug("removing all supported rpcs to routing table");
    Set<QName> currentlySupported = brokerSession.getSupportedRpcs();
    for (QName rpc : currentlySupported) {
      rpcListener.onRpcImplementationRemoved(rpc);
    }
  }

  /**
   * Un-Register all the locally supported Routed RPCs from clustered routing table
   */
  private void unregisterSupportedRoutedRpcs(){

    //TODO: remove all routed RPCs as well

  }
}
