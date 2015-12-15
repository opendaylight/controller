/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.md.sal.binding.impl;

import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMDataBrokerAdapter;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;

public class BindingAsyncDataBrokerImplModule extends
        org.opendaylight.controller.config.yang.md.sal.binding.impl.AbstractBindingAsyncDataBrokerImplModule {

    public BindingAsyncDataBrokerImplModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public BindingAsyncDataBrokerImplModule(
            final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            final org.opendaylight.controller.config.yang.md.sal.binding.impl.BindingAsyncDataBrokerImplModule oldModule,
            final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final BindingToNormalizedNodeCodec mappingService = getBindingMappingServiceDependency();
        final DOMDataBroker domDataBroker = getDomAsyncBrokerDependency();
        return new BindingDOMDataBrokerAdapter(domDataBroker, mappingService);
    }

}
