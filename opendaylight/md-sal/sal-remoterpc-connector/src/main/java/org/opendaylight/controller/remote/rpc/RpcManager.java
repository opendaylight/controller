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
import akka.japi.Function;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.remote.rpc.messages.UpdateSchemaContext;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

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
    private final RemoteRpcProviderConfig config;
    private RpcListener rpcListener;
    private RoutedRpcListener routeChangeListener;
    private RemoteRpcImplementation rpcImplementation;
    private final DOMRpcProviderService rpcProvisionRegistry;
    private final DOMRpcService rpcServices;

    private RpcManager(final SchemaContext schemaContext,
                       final DOMRpcProviderService rpcProvisionRegistry,
                       final DOMRpcService rpcSevices) {
        this.schemaContext = schemaContext;
        this.rpcProvisionRegistry = rpcProvisionRegistry;
        rpcServices = rpcSevices;
        config = new RemoteRpcProviderConfig(getContext().system().settings().config());

        createRpcActors();
        startListeners();
    }


      public static Props props(final SchemaContext schemaContext,
              final DOMRpcProviderService rpcProvisionRegistry, final DOMRpcService rpcServices) {
          Preconditions.checkNotNull(schemaContext, "SchemaContext can not be null!");
          Preconditions.checkNotNull(rpcProvisionRegistry, "RpcProviderService can not be null!");
          Preconditions.checkNotNull(rpcServices, "RpcService can not be null!");
          return Props.create(RpcManager.class, schemaContext, rpcProvisionRegistry, rpcServices);
      }

    private void createRpcActors() {
        LOG.debug("Create rpc registry and broker actors");

        rpcRegistry =
                getContext().actorOf(RpcRegistry.props().
                    withMailbox(config.getMailBoxName()), config.getRpcRegistryName());

        rpcBroker =
                getContext().actorOf(RpcBroker.props(rpcServices, rpcRegistry).
                    withMailbox(config.getMailBoxName()), config.getRpcBrokerName());

        final RpcRegistry.Messages.SetLocalRouter localRouter = new RpcRegistry.Messages.SetLocalRouter(rpcBroker);
        rpcRegistry.tell(localRouter, self());
    }

    private void startListeners() {
        LOG.debug("Registers rpc listeners");

        rpcListener = new RpcListener(rpcRegistry);
        routeChangeListener = new RoutedRpcListener(rpcRegistry);
        rpcImplementation = new RemoteRpcImplementation(rpcBroker, config);

        rpcServices.registerRpcListener(rpcListener);

//        rpcProvisionRegistry.registerRouteChangeListener(routeChangeListener);
//        rpcProvisionRegistry.setRoutedRpcDefaultDelegate(rpcImplementation);
        announceSupportedRpcs();
    }

    /**
     * Add all the locally registered RPCs in the clustered routing table
     */
    private void announceSupportedRpcs(){
        LOG.debug("Adding all supported rpcs to routing table");
        final Set<RpcDefinition> currentlySupportedRpc = schemaContext.getOperations();
        final List<DOMRpcIdentifier> rpcs = new ArrayList<>();
        for (final RpcDefinition rpcDef : currentlySupportedRpc) {
            rpcs.add(DOMRpcIdentifier.create(rpcDef.getPath()));
        }
        rpcListener.onRpcAvailable(rpcs);
    }


    @Override
    protected void handleReceive(final Object message) throws Exception {
      if(message instanceof UpdateSchemaContext) {
        updateSchemaContext((UpdateSchemaContext) message);
      }

    }

    private void updateSchemaContext(final UpdateSchemaContext message) {
      schemaContext = message.getSchemaContext();
      rpcBroker.tell(message, ActorRef.noSender());
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
      return new OneForOneStrategy(10, Duration.create("1 minute"),
          new Function<Throwable, SupervisorStrategy.Directive>() {
            @Override
            public SupervisorStrategy.Directive apply(final Throwable t) {
              return SupervisorStrategy.resume();
            }
          }
      );
    }
}
