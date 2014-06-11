/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.commands.local;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.netconf.cli.commands.AbstractCommand;
import org.opendaylight.controller.netconf.cli.commands.Command;
import org.opendaylight.controller.netconf.cli.commands.CommandDispatcher;
import org.opendaylight.controller.netconf.cli.commands.input.Input;
import org.opendaylight.controller.netconf.cli.commands.input.InputDefinition;
import org.opendaylight.controller.netconf.cli.commands.output.Output;
import org.opendaylight.controller.netconf.cli.commands.output.OutputDefinition;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.impl.CompositeNodeTOImpl;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.data.impl.NodeFactory;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;

/**
 * Local Help command. Displays all commands with description.
 */
public class Help extends AbstractCommand {

    private final CommandDispatcher commandDispatcher;

    public Help(final QName qName, final InputDefinition argsDefinition, final OutputDefinition output, final String description, final CommandDispatcher commandDispatcher) {
        super(qName, argsDefinition, output, description);
        this.commandDispatcher = commandDispatcher;
    }

    @Override
    public Output invoke(final Input inputArgs) {
        final ArrayList<Node<?>> value = Lists.newArrayList();

        for (final String id : commandDispatcher.getCommandIds()) {
            final Optional<Command> cmd = commandDispatcher.getCommand(id);
            Preconditions.checkState(cmd.isPresent(), "Command %s has to be present in command dispatcher", id);
            final Optional<String> description = cmd.get().getCommandDescription();
            final List<Node<?>> nameAndDescription = Lists.newArrayList();
            nameAndDescription.add(NodeFactory.createImmutableSimpleNode(QName.create(getCommandId(), "id"), null, id));
            if(description.isPresent()) {
                nameAndDescription.add(NodeFactory.createImmutableSimpleNode(QName.create(getCommandId(), "description"), null, description.get()));
            }
            value.add(ImmutableCompositeNode.create(QName.create(getCommandId(), "commands"), nameAndDescription));
        }

        return new Output(new CompositeNodeTOImpl(getCommandId(), null, value));
    }

    public static Command create(final RpcDefinition rpcDefinition, final CommandDispatcher commandDispatcher) {
        return new Help(rpcDefinition.getQName(), getInputDefinition(rpcDefinition), getOutputDefinition(rpcDefinition), rpcDefinition.getDescription(), commandDispatcher);
    }
}
