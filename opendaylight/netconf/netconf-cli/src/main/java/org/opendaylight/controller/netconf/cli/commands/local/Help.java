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
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableMapEntryNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableMapNodeBuilder;
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
        final ArrayList<MapEntryNode> value = Lists.newArrayList();

        for (final String id : commandDispatcher.getCommandIds()) {
            final Optional<Command> cmd = commandDispatcher.getCommand(id);
            Preconditions.checkState(cmd.isPresent(), "Command %s has to be present in command dispatcher", id);
            final Optional<String> description = cmd.get().getCommandDescription();
            final List<DataContainerChild<?, ?>> nameAndDescription = Lists.newArrayList();
            nameAndDescription.add(
                    ImmutableLeafNodeBuilder.create()
                            .withNodeIdentifier(new NodeIdentifier(QName.create(getCommandId(), "id")))
                            .withValue(id).build());
            if(description.isPresent()) {
                nameAndDescription.add(
                        ImmutableLeafNodeBuilder.create()
                                .withNodeIdentifier(new NodeIdentifier(QName.create(getCommandId(), "description")))
                                .withValue(description.get()).build());
            }
            value.add(ImmutableMapEntryNodeBuilder.create()
                    .withValue(nameAndDescription)
                    .withNodeIdentifier(
                            new NodeIdentifierWithPredicates(QName.create(getCommandId(), "commands"),
                                    QName.create(getCommandId(), "id"), id)).build());
        }
        MapNode mappedHelp = ImmutableMapNodeBuilder.create()
                .withNodeIdentifier(new NodeIdentifier(QName.create(getCommandId(), "commands")))
                .withValue(value).build();

        return new Output(mappedHelp);
    }

    public static Command create(final RpcDefinition rpcDefinition, final CommandDispatcher commandDispatcher) {
        return new Help(rpcDefinition.getQName(), getInputDefinition(rpcDefinition), getOutputDefinition(rpcDefinition), rpcDefinition.getDescription(), commandDispatcher);
    }
}
