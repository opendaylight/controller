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
package ${package}.impl;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ${classPrefix}Provider implements BindingAwareProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(${classPrefix}Provider.class);
    private ServiceRegistration<${classPrefix}Commands> commandsConsoleRegistration;

    @Override
    public void onSessionInitiated(ProviderContext session) {

        // Retrieve DataBroker service to interact with md-sal
        final DataBroker dataBroker =  session.getSALService(DataBroker.class);

        // Initialize ${classPrefix}CommandImpl class
        final ${classPrefix}CommandImpl commandImpl = new ${classPrefix}CommandImpl(dataBroker);

        // Register the NetconfConsoleProvider service
        commandsConsoleRegistration = context.registerService(${classPrefix}Commands.class, commandImpl, null);
        LOG.info("${classPrefix}Provider Session Initiated");
    }

    @Override
    public void close() throws Exception {
        LOG.info("${classPrefix}Provider Session Closed");
    }
}
