/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.api;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;

/**
 *
 * A developer implemented component that gets registered with the Broker.
 *
 * Semantically, a provider may:
 *
 * <ol>
 *   <li> Emit Notifications</li>
 *   <li> Provide the implementation of RPCs </li>
 *   <li> Write to the operational data tree </li>
 * </ol>
 *
 * If a class is not doing at least one of those three, consider using
 * a BindingAwareConsumer instead:
 * @see org.opendaylight.controller.sal.binding.api.BindingAwareConsumer
 *
 * <p>
 *
 *In addition, a BindingAwareProvider can in pursuit of its goals:
 *
 * <ol>
 *   <li>Subscribe for Notifications </li>
 *   <li>Invoke RPCs</li>
 *   <li>Read from either the operational or config data tree</li>
 *   <li>Write to the config data tree</li>
 * </ol>
 * (All of the above are things a Consumer can also do).
 *
 *<p>
 *
 * Examples:
 *
 *<p>
 *
 * To get a NotificationService:
 *
 * {code
 * public void onSessionInitiated(ProviderContext session) {
 *      NotificationProviderService notificationService = session.getSALService(NotificationProviderService.class);
 * }
 * For more information on sending notifications via the NotificationProviderService
 * @see org.opendaylight.controller.sal.binding.api.NotificationProviderService
 *
 * To register an RPC implementation:
 *
 * {code
 * public void onSessionInitiated(ProviderContext session) {
 *    RpcRegistration<MyService> registration = session.addRpcImplementation(MyService.class, myImplementationInstance);
 * }
 *
 * <p>
 *
 * Where MyService.class is a Service interface generated from a yang model with RPCs modeled in it and myImplementationInstance
 * is an instance of a class that implements MyService.
 *
 * To register a Routed RPC Implementation:
 * {code
 * public void onSessionInitiated(ProviderContext session) {
 *   RoutedRpcRegistration<SalFlowService> flowRegistration = session.addRoutedRpcImplementation(SalFlowService.class, salFlowServiceImplementationInstance);
     flowRegistration.registerPath(NodeContext.class, nodeInstanceId);
 * }
 * }
 *
 * Where SalFlowService.class is a Service interface generated from a yang model with RPCs modeled in it and salFlowServiceImplementationInstance is an instance
 * of a class that implements SalFlowService.
 * <p>
 * The line:
 * {code
 * flowRegistration.registerPath(NodeContext.class, nodeInstanceId);
 * }
 * Is indicating that the RPC implementation is registered to handle RPC invocations that have their NodeContext pointing to the node with instance id nodeInstanceId.
 * This bears a bit of further explanation.  RoutedRPCs can be 'routed' to an implementation based upon 'context'.  'context' is a pointer (instanceId) to some place
 * in the data tree.  In this example, the 'context' is a pointer to a Node.  In this way, a provider can register its ability to provide a service for a particular
 * Node, but not *all* Nodes.  The Broker routes the RPC by 'context' to the correct implementation, without the caller having to do extra work.  Because of this when
 * a RoutedRPC is registered, it needs to also be able to indicate for which 'contexts' it is providing an implementation.
 *
 * An example of a Routed RPC would be an updateFlow(node, flow) that would be routed based on node to the provider which had registered to provide
 * it *for that node*.
 *
 *<p>
 *
 * To get a DataBroker to allow access to the data tree:
 *
 * {code
 * public void onSessionInitiated(final ProviderContext session) {
 *      DataBroker databroker = session.getSALService(BindingDataBroker.class);
 * }
 * }
 * @see org.opendaylight.controller.md.sal.common.api.data.BindingDataBroker
 * for more info on using the DataBroker.
 *
 */
public interface BindingAwareProvider {

    /**
     * Callback signaling initialization of the consumer session to the SAL.
     *
     * The consumer MUST use the session for all communication with SAL or
     * retrieving SAL infrastructure services.
     *
     * This method is invoked by
     * {@link BindingAwareBroker#registerProvider(BindingAwareProvider)}
     *
     * @param session Unique session between consumer and SAL.
     */
    void onSessionInitiated(ProviderContext session);
}
