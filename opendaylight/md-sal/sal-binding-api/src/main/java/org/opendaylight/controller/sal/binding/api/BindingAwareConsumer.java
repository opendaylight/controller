/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.api;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;

/**
*
* A developer implemented component that gets registered with the Broker.
*
* Semantically, a consumer may:
*
* <ol>
*   <li>Subscribe for Notifications </li>
*   <li>Invoke RPCs</li>
*   <li>Read from either the operational or config data tree</li>
*   <li>Write to the config data tree</li>
* </ol>
* If you need to:
* <ol>
*   <li> Emit Notifications</li>
*   <li> Provide the implementation of RPCs </li>
*   <li> Write to the operational data tree </li>
* </ol>
*
* Consider using a BindingAwareProvider
*
* Examples:
*
* To get a NotificationService:
*
* {code
* public void onSessionInitiated(ProviderContext session) {
*      NotificationProviderService notificationService = session.getSALService(NotificationProviderService.class);
*      notificationService.publish(notification)
* }
* where notification is an instance of a modeled Notification.
* For more information on sending notifications via the NotificationProviderService
* @see org.opendaylight.controller.sal.binding.api.NotificationProviderService
*
*
* A consumer can *invoke* and RPC ( ie, call foo(fooArgs)) but it cannot register an RPC
* implementation with the MD-SAL that others can invoke(call).
* To get an invokable RPC:
*
* {code
* public void onSessionInitiated(ProviderContext session) {
*    MyService rpcFlowSalService = session.getRpcService(MyService.class);
* }
*
* Where MyService.class is a Service interface generated from a yang model with RPCs modeled in it.  The returned
* rpcFlowSalService can be used like any other object by invoking its methods.  Note, nothing special needs to be done
* for RoutedRPCs.  They just work.
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
public interface BindingAwareConsumer {

    /**
     * Callback signaling initialization of the consumer session to the SAL.
     *
     * The consumer MUST use the session for all communication with SAL or
     * retrieving SAL infrastructure services.
     *
     * This method is invoked by
     * {@link BindingAwareBroker#registerConsumer(BindingAwareConsumer)}
     *
     * @param session
     *            Unique session between consumer and SAL.
     */
    void onSessionInitialized(ConsumerContext session);

}
