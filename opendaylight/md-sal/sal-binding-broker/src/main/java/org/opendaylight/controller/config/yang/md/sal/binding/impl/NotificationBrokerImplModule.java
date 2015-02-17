/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.md.sal.binding.impl;

import java.util.Collection;
import org.opendaylight.controller.md.sal.binding.impl.BackwardsCompatibleNotificationBroker;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
*
*/
public final class NotificationBrokerImplModule extends
        AbstractNotificationBrokerImplModule implements Provider {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationBrokerImplModule.class);

    public NotificationBrokerImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public NotificationBrokerImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            NotificationBrokerImplModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void validate() {
        super.validate();
        // Add custom validation for module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LOG.debug("CREATING INSTANCE OF NOTIF BROKER");
        final BindingToNormalizedNodeCodec codec = getBindingMappingServiceOldNotifDependency();
        final Broker.ProviderSession session = getDomAsyncBrokerOldNotifDependency().registerProvider(this);
        final DOMNotificationService notifService = session.getService(DOMNotificationService.class);
        return new BackwardsCompatibleNotificationBroker(getNewNotificationPublishServiceDependency(),
                getNewNotificationServiceDependency(), notifService, codec.getCodecRegistry());
    }

    @Override
    public void onSessionInitiated(Broker.ProviderSession session) {

    }

    @Override
    public Collection<ProviderFunctionality> getProviderFunctionality() {
        return null;
    }
}
