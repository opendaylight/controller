/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.commands;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.opendaylight.controller.netconf.cli.commands.local.Close;
import org.opendaylight.controller.netconf.cli.commands.local.Help;
import org.opendaylight.controller.netconf.cli.commands.remote.RemoteCommand;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

/**
 * The registry of available commands local + remote. Created from schema contexts.
 */
public class CommandDispatcher {

    // TODO extract interface

    private final Map<QName, Command> commands;
    private final Multimap<String, QName> localNameToQName;

    CommandDispatcher(final Map<QName, Command> commands) {
        this.commands = commands;
        localNameToQName = mapCommandsByName(commands);
    }

    private static Multimap<String, QName> mapCommandsByName(final Map<QName, Command> commands) {
        final HashMultimap<String, QName> stringQNameHashMultimap = HashMultimap.create();
        for (final QName qName : commands.keySet()) {
            stringQNameHashMultimap.put(qName.getLocalName(), qName);
        }

        return stringQNameHashMultimap;
    }

    public Map<QName, Command> getCommands() {
        return commands;
    }

    public Set<QName> getCommandIds() {
        return commands.keySet();
    }

    public Command getCommand(final QName id) {
        Preconditions.checkArgument(commands.containsKey(id), "Command %s not found, available commands %s",
                commands.keySet());
        return commands.get(id);
    }

    public Map<QName, Command> getCommand(final String localName) {
        final Collection<QName> availableCommands = localNameToQName.get(localName);
        return Maps.filterEntries(commands, new Predicate<Map.Entry<QName, Command>>() {
            @Override
            public boolean apply(@Nullable final Map.Entry<QName, Command> input) {
                return availableCommands.contains(input.getKey());
            }
        });
    }

    public Command getCommand(final String localName, final String moduleName) {
        // TODO implement to support commands with duplicate local name
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param localCommandsSchema schema context that describes local commands
     * @param schemaContexts schema contexts describing remote commands
     * @return
     */
    public static CommandDispatcher fromSchemas(final SchemaContext localCommandsSchema,
            final SchemaContext... schemaContexts) {

        final Map<QName, Command> commands = Maps.newHashMap();

        for (final Module module : localCommandsSchema.getModules()) {
            for (final RpcDefinition rpcDefinition : module.getRpcs()) {
                // FIXME make local commands extensible
                // e.g. by yang extension defining java class to be instantiated
                // problem is with command specific resources
                // e.g. Help would need command registry
                if (rpcDefinition.getQName().equals(CommandConstants.HELP_QNAME)) {
                    commands.put(CommandConstants.HELP_QNAME, Help.create(rpcDefinition));
                }

                if (rpcDefinition.getQName().equals(CommandConstants.CLOSE_QNAME)) {
                    commands.put(CommandConstants.CLOSE_QNAME, Close.create(rpcDefinition));
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
