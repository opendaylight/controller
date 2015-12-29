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
import com.google.common.base.Preconditions;
import java.util.Collection;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.remote.rpc.messages.UpdateSchemaContext;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the base class which initialize all the actors, listeners and
 * default RPc implementation so remote invocation of rpcs.
 */
public class RemoteRpcProvider implements AutoCloseable, Provider, SchemaContextListener {

  private static final Logger LOG = LoggerFactory.getLogger(RemoteRpcProvider.class);

  private final DOMRpcProviderService rpcProvisionRegistry;

  private ListenerRegistration<SchemaContextListener> schemaListenerRegistration;
  private final ActorSystem actorSystem;
  private Broker.ProviderSession brokerSession;
  private SchemaContext schemaContext;
  private ActorRef rpcManager;
  private final RemoteRpcProviderConfig config;


  public RemoteRpcProvider(final ActorSystem actorSystem,
                           final DOMRpcProviderService rpcProvisionRegistry,
                           final RemoteRpcProviderConfig config) {
    this.actorSystem = actorSystem;
    this.rpcProvisionRegistry = rpcProvisionRegistry;
    this.config = Preconditions.checkNotNull(config);
  }

  @Override
  public void close() throws Exception {
    if (schemaListenerRegistration != null) {
        schemaListenerRegistration.close();
        schemaListenerRegistration = null;
    }
  }

  @Override
  public void onSessionInitiated(final Broker.ProviderSession session) {
    brokerSession = session;
    start();
  }

  @Override
  public Collection<ProviderFunctionality> getProviderFunctionality() {
    return null;
  }

  private void start() {
    LOG.info("Starting remote rpc service...");

    final SchemaService schemaService = brokerSession.getService(SchemaService.class);
    final DOMRpcService rpcService = brokerSession.getService(DOMRpcService.class);
    schemaContext = schemaService.getGlobalContext();
    rpcManager = actorSystem.actorOf(RpcManager.props(schemaContext,
            rpcProvisionRegistry, rpcService, config), config.getRpcManagerName());
    schemaListenerRegistration = schemaService.registerSchemaContextListener(this);
    LOG.debug("rpc manager started");
  }

  @Override
  public void onGlobalContextUpdated(final SchemaContext schemaContext) {
    this.schemaContext = schemaContext;
    rpcManager.tell(new UpdateSchemaContext(schemaContext), null);
  }
}
