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
package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.${artifactId}.cli.impl.rev160628;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import ${package}.cli.impl.${classPrefix}CliProvider;

public class ${classPrefix}CliModule extends Abstract${classPrefix}CliModule {

    public ${classPrefix}CliModule(ModuleIdentifier identifier, DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public ${classPrefix}CliModule(ModuleIdentifier identifier, DependencyResolver dependencyResolver, ${classPrefix}CliModule oldModule,
            AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        ${classPrefix}CliProvider provider = new ${classPrefix}CliProvider();
        getBrokerDependency().registerProvider(provider);
        return provider;
    }
}
