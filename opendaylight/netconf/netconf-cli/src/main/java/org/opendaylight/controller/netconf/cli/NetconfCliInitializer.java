/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli;

import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.opendaylight.controller.netconf.cli.commands.Command;
import org.opendaylight.controller.netconf.cli.commands.CommandDispatcher;
import org.opendaylight.controller.netconf.cli.commands.local.Help;
import org.opendaylight.controller.netconf.cli.commands.remote.RemoteCommand;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import com.google.common.collect.Maps;

public class NetconfCliInitializer {

    private static final QName HELP_QNAME;
    static {
        final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        try {
            final Date date = formatter.parse("2014-05-22");
            HELP_QNAME = new QName(URI.create("netconf:cli"), date, "help");
        } catch (final ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public CommandDispatcher initCommands(final SchemaContext localCommandsSchema,
            final SchemaContext... schemaContexts) {

        final Map<QName, Command> commands = Maps.newHashMap();

        for (final Module module : localCommandsSchema.getModules()) {
            for (final RpcDefinition rpcDefinition : module.getRpcs()) {
                // FIXME make local commands extensible
                // e.g. by yang extension defining java class to be instantiated
                // problem is with command specific resources
                // e.g. Help would need command registry
                if (rpcDefinition.getQName().equals(HELP_QNAME)) {
                    commands.put(HELP_QNAME, Help.create(rpcDefinition));
                }
            }
        }

        for (final SchemaContext context : schemaContexts) {
            for (final Module module : context.getModules()) {
                for (final RpcDefinition rpcDefinition : module.getRpcs()) {
                    // FIXME check for duplicates also duplicates with local
                    commands.put(rpcDefinition.getQName(), RemoteCommand.fromRpc(rpcDefinition));
                }
            }
        }

        return new CommandDispatcher(commands);
    }

}
