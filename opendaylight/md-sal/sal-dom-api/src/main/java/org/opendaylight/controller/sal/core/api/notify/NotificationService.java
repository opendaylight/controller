/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.api.notify;

import org.opendaylight.controller.sal.core.api.BrokerService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;


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
 * {@link org.opendaylight.controller.sal.core.api.Provider#getProviderFunctionality()}
 * <li>passing an instance of implementation and {@link QName} of an RPC as an
 * argument to
 * {@link org.opendaylight.controller.sal.core.api.Broker.ProviderSession#addRpcImplementation(QName, org.opendaylight.controller.sal.core.api.RpcImplementation)}
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
    ListenerRegistration<NotificationListener> addNotificationListener(QName notification,
            NotificationListener listener);
}
