/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.md.sal.binding.impl;

import org.opendaylight.controller.md.sal.binding.compat.HydrogenDataBrokerAdapter;

import java.util.Collection;
import java.util.Collections;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.Provider;

/**
*
*/
@Deprecated
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
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final HydrogenDataBrokerAdapter dataBroker = new HydrogenDataBrokerAdapter(getDataBrokerDependency());
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
