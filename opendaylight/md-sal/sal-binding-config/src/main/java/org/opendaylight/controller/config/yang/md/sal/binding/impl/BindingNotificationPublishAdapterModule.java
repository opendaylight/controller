/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.md.sal.binding.impl;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMNotificationPublishServiceAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService;
import org.opendaylight.controller.sal.core.api.Broker;

/**
 * @deprecated Replaced by blueprint wiring
 */
@Deprecated
public class BindingNotificationPublishAdapterModule extends AbstractBindingNotificationPublishAdapterModule {
    public BindingNotificationPublishAdapterModule(final ModuleIdentifier identifier, final DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public BindingNotificationPublishAdapterModule(final ModuleIdentifier identifier, final DependencyResolver dependencyResolver, final BindingNotificationPublishAdapterModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final BindingToNormalizedNodeCodec codec = getBindingMappingServiceDependency();
        final Broker.ProviderSession session = getDomAsyncBrokerDependency().registerProvider(new DummyDOMProvider());
        final DOMNotificationPublishService publishService = session.getService(DOMNotificationPublishService.class);
        return new BindingDOMNotificationPublishServiceAdapter(codec, publishService);
    }

}
