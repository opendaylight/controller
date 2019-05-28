//package org.opendaylight.controller.remote.rpc;
//
//import akka.actor.Address;
//import akka.actor.Props;
//import com.google.common.base.Preconditions;
//import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
//import org.opendaylight.mdsal.dom.api.DOMActionImplementation;
//import org.opendaylight.mdsal.dom.api.DOMActionProviderService;
//import org.opendaylight.yangtools.concepts.ObjectRegistration;
//
//import java.util.*;
//
//public class ActionRegistar extends AbstractUntypedActor {
//    private final Map<Address, ObjectRegistration<DOMActionImplementation>> actionRegs = new HashMap<>();
//    private final RemoteRpcProviderConfig config;
//    private final DOMActionProviderService actionProviderService;
//
//    ActionRegistar(final RemoteRpcProviderConfig config, final DOMActionProviderService actionProviderService) {
//        this.config = Preconditions.checkNotNull(config);
//        this.actionProviderService = Preconditions.checkNotNull(actionProviderService);
//    }
//
//    public static Props props(final RemoteRpcProviderConfig config, final DOMActionProviderService actionProviderService) {
//        Preconditions.checkNotNull(actionProviderService, "DOMActionProviderService cannot be null");
//        return Props.create(RpcRegistrar.class, config, actionProviderService);
//    }
//
//    @Override
//    public void postStop() throws Exception {
//        actionRegs.clear();
//
//        super.postStop();
//    }
//
//    @Override
//    protected void handleReceive(final Object message) {
//        if (message instanceof RpcRegistry.Messages.UpdateRemoteEndpoints) {
//            updateRemoteEndpoints(((RpcRegistry.Messages.UpdateRemoteEndpoints) message).getRpcEndpoints(), ((RpcRegistry.Messages.UpdateRemoteEndpoints) message).getActionEndpoints());
//        } else {
//            unknownMessage(message);
//        }
//    }
//
//    private void updateRemoteEndpoints(final Map<Address, Optional<RpcRegistry.RemoteRpcEndpoint>> rpcEndpoints, final Map<Address, Optional<RpcRegistry.RemoteActionEndpoint>> actionEndpoints) {
//        /*
//         * Updating RPC providers is a two-step process. We first add the newly-discovered RPCs and then close
//         * the old registration. This minimizes churn observed by listeners, as they will not observe RPC
//         * unavailability which would occur if we were to do it the other way around.
//         *
//         * Note that when an RPC moves from one remote node to another, we also do not want to expose the gap,
//         * hence we register all new implementations before closing all registrations.
//         */
//        final Collection<ObjectRegistration<DOMRpcImplementation>> prevRpcRegs = new ArrayList<>(rpcEndpoints.size());
//        final Collection<ObjectRegistration<DOMActionImplementation>> prevActionRegs = new ArrayList<>(actionEndpoints.size());
//
//        for (Map.Entry<Address, Optional<RpcRegistry.RemoteRpcEndpoint>> e : rpcEndpoints.entrySet()) {
//            LOG.debug("Updating RPC registrations for {}", e.getKey());
//
//            final ObjectRegistration<DOMRpcImplementation> prevRpcReg;
//            final Optional<RpcRegistry.RemoteRpcEndpoint> maybeEndpoint = e.getValue();
//            if (maybeEndpoint.isPresent()) {
//                final RpcRegistry.RemoteRpcEndpoint endpoint = maybeEndpoint.get();
//                final RemoteRpcImplementation impl = new RemoteRpcImplementation(endpoint.getRouter(), config);
//                prevRpcReg = rpcRegs.put(e.getKey(), rpcProviderService.registerRpcImplementation(impl,
//                        endpoint.getRpcs()));
//            } else {
//                prevRpcReg = rpcRegs.remove(e.getKey());
//            }
//
//            if (prevRpcReg != null) {
//                prevRpcRegs.add(prevRpcReg);
//            }
//        }
//
////        for (DOMRpcImplementationRegistration<?> r : prevRpcRegs) {
////            r.close();
////        }
//
//        for (Map.Entry<Address, Optional<RpcRegistry.RemoteActionEndpoint>> e : actionEndpoints.entrySet()) {
//            LOG.debug("Updating Action registrations for {}", e.getKey());
//
//            final ObjectRegistration<DOMActionImplementation> prevActionReg;
//            final Optional<RpcRegistry.RemoteActionEndpoint> maybeEndpoint = e.getValue();
//            if (maybeEndpoint.isPresent()) {
//                final RpcRegistry.RemoteActionEndpoint endpoint = maybeEndpoint.get();
//                final RemoteActionImplementation impl = new RemoteActionImplementation(endpoint.getRouter(), config);
//                prevActionReg = actionRegs.put(e.getKey(), actionProviderService.registerActionImplementation(impl, endpoint.getActions()));
//            } else {
//                prevActionReg = actionRegs.remove(e.getKey());
//            }
//
//            if (prevActionReg != null) {
//                prevActionRegs.add(prevActionReg);
//            }
//        }
//
////        for (DOMActionImplementationRegistration<?> r : prevActionRegs) {
////            r.close();
////        }
//    }