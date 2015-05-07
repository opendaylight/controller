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
import org.opendaylight.controller.md.sal.binding.compat.HeliumNotificationProviderServiceWithInterestListeners;
import org.opendaylight.controller.md.sal.binding.compat.HydrogenNotificationBrokerImpl;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMNotificationPublishServiceAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMNotificationServiceAdapter;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService;
import org.opendaylight.controller.md.sal.dom.spi.DOMNotificationSubscriptionListenerRegistry;
import org.opendaylight.controller.sal.binding.codegen.impl.SingletonHolder;

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
            return createHeliumAdapter(notificationPublishService,notificationService);
        }

        /*
         *  FIXME: Switch to new broker (which has different threading model)
         *  once this change is communicated with downstream users or
         *  we will have adapter implementation which will honor Helium
         *  threading model for notifications.
         */
        return new HydrogenNotificationBrokerImpl(SingletonHolder.getDefaultNotificationExecutor());
    }

    private static AutoCloseable createHeliumAdapter(final NotificationPublishService publishService,
            final NotificationService listenService) {
        if (listenService instanceof BindingDOMNotificationServiceAdapter
                && publishService instanceof BindingDOMNotificationPublishServiceAdapter) {
            final BindingDOMNotificationPublishServiceAdapter castedPublish =
                    (BindingDOMNotificationPublishServiceAdapter) publishService;
            final BindingDOMNotificationServiceAdapter castedListen =
                    (BindingDOMNotificationServiceAdapter) listenService;
            final DOMNotificationPublishService domPublishService = castedPublish.getDomPublishService();
            if (domPublishService instanceof DOMNotificationSubscriptionListenerRegistry) {
                final DOMNotificationSubscriptionListenerRegistry subsRegistry =
                        (DOMNotificationSubscriptionListenerRegistry) domPublishService;
                return new HeliumNotificationProviderServiceWithInterestListeners(castedPublish, castedListen,
                        subsRegistry);
            }
        }
        return new HeliumNotificationProviderServiceAdapter(publishService, listenService);
    }
}
