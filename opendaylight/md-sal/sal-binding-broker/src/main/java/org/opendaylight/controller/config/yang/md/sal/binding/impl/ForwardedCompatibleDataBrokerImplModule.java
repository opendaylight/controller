/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.md.sal.binding.impl;

import org.opendaylight.controller.md.sal.binding.compat.hydrogen.ForwardedBackwardsCompatibleDataBroker;

import com.google.common.util.concurrent.ListeningExecutorService;
import java.util.Collection;
import java.util.Collections;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.sal.binding.codegen.impl.SingletonHolder;
import org.opendaylight.controller.sal.binding.impl.connect.dom.BindingDomConnectorDeployer;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.model.SchemaService;

/**
*
*/
public final class ForwardedCompatibleDataBrokerImplModule extends
        org.opendaylight.controller.config.yang.md.sal.binding.impl.AbstractForwardedCompatibleDataBrokerImplModule
        implements Provider {

    public ForwardedCompatibleDataBrokerImplModule(
            final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public ForwardedCompatibleDataBrokerImplModule(
            final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            final ForwardedCompatibleDataBrokerImplModule oldModule, final java.lang.AutoCloseable oldInstance) {

        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    protected void customValidation() {
        // Add custom validation for module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        ListeningExecutorService listeningExecutor = SingletonHolder.getDefaultCommitExecutor();
        BindingToNormalizedNodeCodec mappingService = getBindingMappingServiceDependency();

        Broker domBroker = getDomAsyncBrokerDependency();
        ProviderSession session = domBroker.registerProvider(this, null);
        DOMDataBroker domDataBroker = session.getService(DOMDataBroker.class);
        SchemaService schemaService = session.getService(SchemaService.class);
        ForwardedBackwardsCompatibleDataBroker dataBroker = new ForwardedBackwardsCompatibleDataBroker(domDataBroker,
                mappingService, schemaService,listeningExecutor);

        dataBroker.setConnector(BindingDomConnectorDeployer.createConnector(mappingService.getLegacy()));
        dataBroker.setDomProviderContext(session);
        return dataBroker;
    }

    @Override
    public void onSessionInitiated(final ProviderSession session) {

    }

    @Override
    public Collection<ProviderFunctionality> getProviderFunctionality() {
        return Collections.emptySet();
    }
}
