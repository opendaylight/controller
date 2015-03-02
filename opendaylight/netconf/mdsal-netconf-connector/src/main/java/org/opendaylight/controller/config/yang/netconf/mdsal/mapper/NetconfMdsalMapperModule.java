/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.netconf.mdsal.mapper;

import java.util.Collection;
import org.opendaylight.controller.netconf.mdsal.connector.MdsalNetconfOperationServiceFactory;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.Provider;

public class NetconfMdsalMapperModule extends org.opendaylight.controller.config.yang.netconf.mdsal.mapper.AbstractNetconfMdsalMapperModule {
    public NetconfMdsalMapperModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public NetconfMdsalMapperModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.netconf.mdsal.mapper.NetconfMdsalMapperModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        ProviderSession session = getDomBrokerDependency().registerProvider(new MappingProvider());
        final MdsalNetconfOperationServiceFactory mdsalNetconfOperationServiceFactory = new MdsalNetconfOperationServiceFactory(getRootSchemaServiceDependency(), session) {
            @Override
            public void close() throws Exception {
                super.close();
                getMapperAggregatorDependency().onRemoveNetconfOperationServiceFactory(this);
            }
        };
        getMapperAggregatorDependency().onAddNetconfOperationServiceFactory(mdsalNetconfOperationServiceFactory);
        return mdsalNetconfOperationServiceFactory;
    }

    private class MappingProvider implements Provider {

        @Override
        public void onSessionInitiated(ProviderSession session) {

        }

        @Override
        public Collection<ProviderFunctionality> getProviderFunctionality() {
            return null;
        }
    }

}
