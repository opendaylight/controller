/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.md.sal.binding.impl;

import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.md.sal.binding.compat.HeliumNotificationProviderServiceAdapter;
import org.opendaylight.controller.sal.binding.codegen.impl.SingletonHolder;
import org.opendaylight.controller.sal.binding.impl.NotificationBrokerImpl;

/**
*
*/
public final class NotificationBrokerImplModule extends
        org.opendaylight.controller.config.yang.md.sal.binding.impl.AbstractNotificationBrokerImplModule {

    public NotificationBrokerImplModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public NotificationBrokerImplModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            final NotificationBrokerImplModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void validate() {
        super.validate();
        // Add custom validation for module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {

        final NotificationPublishService notificationPublishService = getNotificationPublishAdapterDependency();
        final NotificationService notificationService = getNotificationAdapterDependency();

        if(notificationPublishService != null & notificationService != null) {
            return new HeliumNotificationProviderServiceAdapter(notificationPublishService, notificationService);
        }

        /*
         *  FIXME: Switch to new broker (which has different threading model)
         *  once this change is communicated with downstream users or
         *  we will have adapter implementation which will honor Helium
         *  threading model for notifications.
         */

        return new NotificationBrokerImpl(SingletonHolder.getDefaultNotificationExecutor());
    }
}
