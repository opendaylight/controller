/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc;


import akka.actor.ActorRef;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.japi.Creator;
import akka.japi.Function;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.opendaylight.controller.remote.rpc.messages.UpdateSchemaContext;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry;
import org.opendaylight.controller.remote.rpc.utils.ActorUtil;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.RpcProvisionRegistry;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;
import java.util.Set;

/**
 * This class acts as a supervisor, creates all the actors, resumes them, if an exception is thrown.
 *
 * It also starts the rpc listeners
 */

public class RpcManager extends AbstractUntypedActor {

  private static final Logger LOG = LoggerFactory.getLogger(RpcManager.class);

  private SchemaContext schemaContext;
  private ActorRef rpcBroker;
  private ActorRef rpcRegistry;
  private final Broker.ProviderSession brokerSession;
  private RpcListener rpcListener;
  private RoutedRpcListener routeChangeListener;
  private RemoteRpcImplementation rpcImplementation;
  private final RpcProvisionRegistry rpcProvisionRegistry;

  private RpcManager(SchemaContext schemaContext,
                     Broker.ProviderSession brokerSession, RpcProvisionRegistry rpcProvisionRegistry) {
    this.schemaContext = schemaContext;
    this.brokerSession = brokerSession;
    this.rpcProvisionRegistry = rpcProvisionRegistry;

    createRpcActors();
    startListeners();
  }


  public static Props props(final SchemaContext schemaContext,
                            final Broker.ProviderSession brokerSession, final RpcProvisionRegistry rpcProvisionRegistry) {
    return Props.create(new Creator<RpcManager>() {
      @Override
      public RpcManager create() throws Exception {
        return new RpcManager(schemaContext, brokerSession, rpcProvisionRegistry);
      }
    });
  }

  private void createRpcActors() {
    LOG.debug("Create rpc registry and broker actors");

      Config conf = ConfigFactory.load();

    rpcRegistry =
            getContext().actorOf(Props.create(RpcRegistry.class).
                withMailbox(ActorUtil.MAILBOX), ActorConstants.RPC_REGISTRY);

    rpcBroker =
            getContext().actorOf(RpcBroker.props(brokerSession, rpcRegistry, schemaContext).
                withMailbox(ActorUtil.MAILBOX),ActorConstants.RPC_BROKER);

    RpcRegistry.Messages.SetLocalRouter localRouter = new RpcRegistry.Messages.SetLocalRouter(rpcBroker);
    rpcRegistry.tell(localRouter, self());
  }

  private void startListeners() {
    LOG.debug("Registers rpc listeners");

    rpcListener = new RpcListener(rpcRegistry);
    routeChangeListener = new RoutedRpcListener(rpcRegistry);
    rpcImplementation = new RemoteRpcImplementation(rpcBroker, schemaContext);

    brokerSession.addRpcRegistrationListener(rpcListener);
    rpcProvisionRegistry.registerRouteChangeListener(routeChangeListener);
    rpcProvisionRegistry.setRoutedRpcDefaultDelegate(rpcImplementation);
    announceSupportedRpcs();
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


  @Override
  protected void handleReceive(Object message) throws Exception {
    if(message instanceof UpdateSchemaContext) {
      updateSchemaContext((UpdateSchemaContext) message);
    }

  }

  private void updateSchemaContext(UpdateSchemaContext message) {
    this.schemaContext = message.getSchemaContext();
  }

  @Override
  public SupervisorStrategy supervisorStrategy() {
    return new OneForOneStrategy(10, Duration.create("1 minute"),
        new Function<Throwable, SupervisorStrategy.Directive>() {
          @Override
          public SupervisorStrategy.Directive apply(Throwable t) {
            return SupervisorStrategy.resume();
          }
        }
    );
  }
}
