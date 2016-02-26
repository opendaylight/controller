/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.config.legacy_entity_ownership_service_provider;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.md.sal.common.impl.clustering.LegacyEntityOwnershipServiceAdapter;

public class LegacyEntityOwnershipServiceProviderModule extends AbstractLegacyEntityOwnershipServiceProviderModule {
    public LegacyEntityOwnershipServiceProviderModule(ModuleIdentifier identifier, DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public LegacyEntityOwnershipServiceProviderModule(ModuleIdentifier identifier, DependencyResolver dependencyResolver,
            LegacyEntityOwnershipServiceProviderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return new LegacyEntityOwnershipServiceAdapter(getDomEntityOwnershipServiceDependency());
    }
}
