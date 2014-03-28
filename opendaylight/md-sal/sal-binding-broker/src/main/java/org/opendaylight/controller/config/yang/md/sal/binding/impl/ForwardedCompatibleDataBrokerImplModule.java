/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.md.sal.binding.impl;

import java.util.Collection;
import java.util.Collections;

import org.opendaylight.controller.md.sal.binding.impl.ForwardedBackwardsCompatibleDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.sal.binding.codegen.impl.SingletonHolder;
import org.opendaylight.controller.sal.binding.impl.connect.dom.BindingDomConnectorDeployer;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;
import org.osgi.framework.BundleContext;

import com.google.common.util.concurrent.ListeningExecutorService;

/**
*
*/
public final class ForwardedCompatibleDataBrokerImplModule extends
        org.opendaylight.controller.config.yang.md.sal.binding.impl.AbstractForwardedCompatibleDataBrokerImplModule
        implements Provider {

    private BundleContext bundleContext;

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
        BindingIndependentMappingService mappingService = getBindingMappingServiceDependency();

        Broker domBroker = getDomAsyncBrokerDependency();
        ProviderSession session = domBroker.registerProvider(this, getBundleContext());
        DOMDataBroker domDataBroker = session.getService(DOMDataBroker.class);
        ForwardedBackwardsCompatibleDataBroker dataBroker = new ForwardedBackwardsCompatibleDataBroker(domDataBroker,
                mappingService, listeningExecutor);

        session.getService(SchemaService.class).registerSchemaServiceListener(dataBroker);

        dataBroker.setConnector(BindingDomConnectorDeployer.createConnector(getBindingMappingServiceDependency()));
        dataBroker.setDomProviderContext(session);
        return dataBroker;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(final BundleContext bundleContext2) {
        this.bundleContext = bundleContext2;
    }

    @Override
    public void onSessionInitiated(final ProviderSession session) {

    }

    @Override
    public Collection<ProviderFunctionality> getProviderFunctionality() {
        return Collections.emptySet();
    }
}
