/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc;


import akka.actor.ActorSystem;
import org.opendaylight.controller.remote.rpc.registry.ClusterWrapper;
import org.opendaylight.controller.remote.rpc.registry.ClusterWrapperImpl;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.RpcProvisionRegistry;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * This is the base class which initialize all the actors, listeners and
 * default RPc implementation so remote invocation of rpcs.
 */
public class RemoteRpcProvider implements AutoCloseable, Provider{

  private static final Logger LOG = LoggerFactory.getLogger(RemoteRpcProvider.class);

  private final ActorSystem actorSystem;
  private final RpcProvisionRegistry rpcProvisionRegistry;
  private Broker.ProviderSession brokerSession;


  public RemoteRpcProvider(ActorSystem actorSystem, RpcProvisionRegistry rpcProvisionRegistry) {
    this.actorSystem = actorSystem;
    this.rpcProvisionRegistry = rpcProvisionRegistry;
  }

  @Override
  public void close() throws Exception {
    this.actorSystem.shutdown();
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
    LOG.debug("Starting all rpc listeners and actors.");
    // Create actor to handle and sync routing table in cluster
    ClusterWrapper clusterWrapper = new ClusterWrapperImpl(actorSystem);
    SchemaService schemaService = brokerSession.getService(SchemaService.class);
    SchemaContext schemaContext = schemaService.getGlobalContext();

    actorSystem.actorOf(RpcManager.props(clusterWrapper, schemaContext, brokerSession, rpcProvisionRegistry), "rpc");

    LOG.debug("Rpc actors are created.");
  }

}
