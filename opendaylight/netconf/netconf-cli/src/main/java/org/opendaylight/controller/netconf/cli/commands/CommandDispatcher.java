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

import org.opendaylight.yangtools.yang.common.QName;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

public class CommandDispatcher {

    // TODO extract interface

    private final Map<QName, Command> commands;
    private final Multimap<String, QName> localNameToQName;

    public CommandDispatcher(final Map<QName, Command> commands) {
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

}
