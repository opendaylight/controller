/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.api.notify;

import org.opendaylight.controller.sal.core.api.BrokerService;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.yang.common.QName;


/**
 * NotificationService provides access to the notification functionality of the
 * SAL.
 * 
 * NotificationService allows for consumption of notifications by registering
 * implementations of NotificationListener.
 * 
 * The registration of notification listeners could be done by:
 * <ul>
 * <li>returning an instance of implementation in the return value of
 * {@link Provider#getProviderFunctionality()}
 * <li>passing an instance of implementation and {@link QName} of rpc as an
 * arguments to the
 * {@link ProviderSession#addRpcImplementation(QName, RpcImplementation)}
 * </ul>
 * 
 * 
 */
public interface NotificationService extends BrokerService {

    /**
     * Registers a notification listener for supplied notification type.
     * 
     * @param notification
     * @param listener
     */
    void addNotificationListener(QName notification,
            NotificationListener listener);

    /**
     * Removes a notification listener for supplied notification type.
     * 
     * @param notification
     * @param listener
     */
    void removeNotificationListener(QName notification,
            NotificationListener listener);
}
