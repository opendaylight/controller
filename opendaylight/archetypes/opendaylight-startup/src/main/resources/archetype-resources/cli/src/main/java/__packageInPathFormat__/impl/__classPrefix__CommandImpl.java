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

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ${package}.api.${classPrefix}Commands;

public class ${classPrefix}CommandImpl implements ${classPrefix}Commands {

    private static final Logger LOG = LoggerFactory.getLogger(${classPrefix}CommandImpl.class);
    private final DataBroker dataBroker;

    public ${classPrefix}CommandImpl(final DataBroker db) {
        this.dataBroker = db;
        LOG.info("${classPrefix}CommandImpl initialized");
    }

    @Override
    public Object testCommand(Object testArgument) {
        return "This is a test implementation of test-command";
    }
}