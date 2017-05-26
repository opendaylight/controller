/*
 * Copyright (c) 2015 Cisco Systems Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ntfbenchmark.impl.rev141210;

import ntfbenchmark.impl.NtfbenchmarkProvider;

public class NtfbenchmarkModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ntfbenchmark.impl.rev141210.AbstractNtfbenchmarkModule {
    public NtfbenchmarkModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public NtfbenchmarkModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ntfbenchmark.impl.rev141210.NtfbenchmarkModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        NtfbenchmarkProvider provider = new NtfbenchmarkProvider(getListenServiceDependency(), getPublishServiceDependency());
        getBrokerDependency().registerProvider(provider);
        return provider;
    }

}
