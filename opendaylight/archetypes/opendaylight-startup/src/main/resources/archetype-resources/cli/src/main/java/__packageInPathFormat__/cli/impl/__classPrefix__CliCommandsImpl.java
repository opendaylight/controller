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
import ${package}.cli.api.${classPrefix}CliCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ${classPrefix}CliCommandsImpl implements ${classPrefix}CliCommands {

    private static final Logger LOG = LoggerFactory.getLogger(${classPrefix}CliCommandsImpl.class);
    private final DataBroker dataBroker;

    public ${classPrefix}CliCommandsImpl(final DataBroker db) {
        this.dataBroker = db;
        LOG.info("${classPrefix}CliCommandImpl initialized");
    }

    @Override
    public Object testCommand(Object testArgument) {
        return "This is a test implementation of test-command";
    }
}
