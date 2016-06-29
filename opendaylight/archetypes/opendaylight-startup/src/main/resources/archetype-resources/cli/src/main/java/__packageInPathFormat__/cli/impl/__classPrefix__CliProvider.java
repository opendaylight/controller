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
package ${package}.cli.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ${package}.cli.api.${classPrefix}CliCommands;

public class ${classPrefix}CliProvider implements BindingAwareProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(${classPrefix}CliProvider.class);
    private ServiceRegistration<${classPrefix}CliCommands> cliCommandsRegistration;

    @Override
    public void onSessionInitiated(ProviderContext session) {

        final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

        // Retrieve DataBroker service to interact with md-sal
        final DataBroker dataBroker =  session.getSALService(DataBroker.class);

        // Initialize ${classPrefix}CliCommandImpl class
        final ${classPrefix}CliCommandImpl cliCommandImpl = new ${classPrefix}CliCommandImpl(dataBroker);

        // Register the ${classPrefix}CliCommands service
        cliCommandsRegistration = context.registerService(${classPrefix}CliCommands.class, cliCommandImpl, null);
        LOG.info("${classPrefix}CliProvider Session Initiated");
    }

    @Override
    public void close() throws Exception {
        cliCommandsRegistration.unregister();
        LOG.info("${classPrefix}CliProvider Session Closed");
    }
}
