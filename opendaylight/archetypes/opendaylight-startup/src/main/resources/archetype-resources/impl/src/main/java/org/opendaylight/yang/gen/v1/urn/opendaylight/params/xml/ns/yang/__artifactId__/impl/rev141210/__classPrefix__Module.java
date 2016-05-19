#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/*
 * Copyright © ${copyrightYear} ${copyright} and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.${artifactId}.impl.rev141210;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import ${package}.impl.${classPrefix}Provider;

public class ${classPrefix}Module extends Abstract${classPrefix}Module {

    public ${classPrefix}Module(ModuleIdentifier identifier, DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public ${classPrefix}Module(ModuleIdentifier identifier, DependencyResolver dependencyResolver, ${classPrefix}Module oldModule,
            AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        ${classPrefix}Provider provider = new ${classPrefix}Provider();
        getBrokerDependency().registerProvider(provider);
        return provider;
    }

}
