/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.test.impl;

/**
*
*/
public final class IdentityTestModule extends org.opendaylight.controller.config.yang.test.impl.AbstractIdentityTestModule
 {

    public IdentityTestModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public IdentityTestModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            IdentityTestModule oldModule, java.lang.AutoCloseable oldInstance) {

        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    protected void customValidation(){
        // Add custom validation for module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        System.err.println(getAfi());
        System.err.println(getAfiIdentity());

        getAfiIdentity();
        for (Identities identities : getIdentities()) {
            identities.resolveAfi();
            identities.resolveSafi();
        }
        getIdentitiesContainer().resolveAfi();

        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
            }
        };

    }
}
