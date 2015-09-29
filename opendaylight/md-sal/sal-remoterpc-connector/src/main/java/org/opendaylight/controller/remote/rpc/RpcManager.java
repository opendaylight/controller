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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.broker.spi.rpc.RpcRoutingStrategy;
import org.opendaylight.controller.remote.rpc.messages.UpdateSchemaContext;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.Module;
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
    private RemoteRpcImplementation rpcImplementation;
    private final DOMRpcProviderService rpcProvisionRegistry;
    private final DOMRpcService rpcServices;

    private RpcManager(final SchemaContext schemaContext,
                       final DOMRpcProviderService rpcProvisionRegistry,
                       final DOMRpcService rpcSevices,
                       final RemoteRpcProviderConfig config) {
        this.schemaContext = schemaContext;
        this.rpcProvisionRegistry = rpcProvisionRegistry;
        rpcServices = rpcSevices;
        this.config = config;

        createRpcActors();
        startListeners();
    }


      public static Props props(final SchemaContext schemaContext,
              final DOMRpcProviderService rpcProvisionRegistry, final DOMRpcService rpcServices,
              final RemoteRpcProviderConfig config) {
          Preconditions.checkNotNull(schemaContext, "SchemaContext can not be null!");
          Preconditions.checkNotNull(rpcProvisionRegistry, "RpcProviderService can not be null!");
          Preconditions.checkNotNull(rpcServices, "RpcService can not be null!");
          return Props.create(RpcManager.class, schemaContext, rpcProvisionRegistry, rpcServices, config);
      }

    private void createRpcActors() {
        LOG.debug("Create rpc registry and broker actors");

        rpcRegistry =
                getContext().actorOf(RpcRegistry.props(config).
                    withMailbox(config.getMailBoxName()), config.getRpcRegistryName());

        rpcBroker =
                getContext().actorOf(RpcBroker.props(rpcServices).
                    withMailbox(config.getMailBoxName()), config.getRpcBrokerName());

        final RpcRegistry.Messages.SetLocalRouter localRouter = new RpcRegistry.Messages.SetLocalRouter(rpcBroker);
        rpcRegistry.tell(localRouter, self());
    }

    private void startListeners() {
        LOG.debug("Registers rpc listeners");

        rpcListener = new RpcListener(rpcRegistry);
        rpcImplementation = new RemoteRpcImplementation(rpcRegistry, config);

        rpcServices.registerRpcListener(rpcListener);

        registerRoutedRpcDelegate();
        announceSupportedRpcs();
    }

    private void registerRoutedRpcDelegate() {
        final Set<DOMRpcIdentifier> rpcIdentifiers = new HashSet<>();
        final Set<Module> modules = schemaContext.getModules();
        for(final Module module : modules){
            for(final RpcDefinition rpcDefinition : module.getRpcs()){
                if(RpcRoutingStrategy.from(rpcDefinition).isContextBasedRouted()) {
                    LOG.debug("Adding routed rpcDefinition for path {}", rpcDefinition.getPath());
                    rpcIdentifiers.add(DOMRpcIdentifier.create(rpcDefinition.getPath(), YangInstanceIdentifier.EMPTY));
                }
            }
        }
        rpcProvisionRegistry.registerRpcImplementation(rpcImplementation, rpcIdentifiers);
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
        if(!rpcs.isEmpty()) {
            rpcListener.onRpcAvailable(rpcs);
        }
    }


    @Override
    protected void handleReceive(final Object message) throws Exception {
      if(message instanceof UpdateSchemaContext) {
        updateSchemaContext((UpdateSchemaContext) message);
      }

    }

    private void updateSchemaContext(final UpdateSchemaContext message) {
      schemaContext = message.getSchemaContext();
      registerRoutedRpcDelegate();
      rpcBroker.tell(message, ActorRef.noSender());
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
      return new OneForOneStrategy(10, Duration.create("1 minute"),
          new Function<Throwable, SupervisorStrategy.Directive>() {
            @Override
            public SupervisorStrategy.Directive apply(final Throwable t) {
              LOG.error("An exception happened actor will be resumed", t);

              return SupervisorStrategy.resume();
            }
          }
      );
    }
}
