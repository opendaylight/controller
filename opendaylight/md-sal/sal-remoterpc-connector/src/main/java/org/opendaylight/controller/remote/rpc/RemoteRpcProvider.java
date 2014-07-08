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
import org.opendaylight.controller.remote.rpc.registry.ClusterWrapper;
import org.opendaylight.controller.remote.rpc.registry.ClusterWrapperImpl;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.RpcProvisionRegistry;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;

/**
 * This is the base class which initialize all the actors, listeners and
 * default RPc implementation so remote invocation of rpcs.
 */
public class RemoteRpcProvider implements AutoCloseable, Provider{

  private static final Logger LOG = LoggerFactory.getLogger(RemoteRpcProvider.class);

  private final ActorSystem actorSystem;
  private ActorRef rpcBroker;
  private ActorRef rpcRegistry;
  private final RpcProvisionRegistry rpcProvisionRegistry;
  private Broker.ProviderSession brokerSession;
  private RpcListener rpcListener;
  private RoutedRpcListener routeChangeListener;
  private RemoteRpcImplementation rpcImplementation;
  public RemoteRpcProvider(ActorSystem actorSystem, RpcProvisionRegistry rpcProvisionRegistry) {
    this.actorSystem = actorSystem;
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
    // Create actor to handle and sync routing table in cluster
    ClusterWrapper clusterWrapper = new ClusterWrapperImpl(actorSystem);
    rpcRegistry = actorSystem.actorOf(RpcRegistry.props(clusterWrapper), "rpc-registry");

    // Create actor to invoke and execute rpc
    SchemaService schemaService = brokerSession.getService(SchemaService.class);
    SchemaContext schemaContext = schemaService.getGlobalContext();
    rpcBroker = actorSystem.actorOf(RpcBroker.props(brokerSession, rpcRegistry, schemaContext), "rpc-broker");
    String rpcBrokerPath = clusterWrapper.getAddress().toString() + "/user/rpc-broker";
    rpcListener = new RpcListener(rpcRegistry, rpcBrokerPath);
    routeChangeListener = new RoutedRpcListener(rpcRegistry, rpcBrokerPath);
    rpcImplementation = new RemoteRpcImplementation(rpcBroker, schemaContext);
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
