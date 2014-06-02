/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;

import org.opendaylight.controller.netconf.cli.commands.Command;
import org.opendaylight.controller.netconf.cli.commands.CommandConstants;
import org.opendaylight.controller.netconf.cli.commands.CommandDispatcher;
import org.opendaylight.controller.netconf.cli.commands.CommandInvocationException;
import org.opendaylight.controller.netconf.cli.commands.input.Input;
import org.opendaylight.controller.netconf.cli.commands.input.InputDefinition;
import org.opendaylight.controller.netconf.cli.commands.output.Output;
import org.opendaylight.controller.netconf.cli.io.ConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.reader.Reader;
import org.opendaylight.controller.netconf.cli.reader.ReadingException;
import org.opendaylight.controller.netconf.cli.reader.custom.FilterReader;
import org.opendaylight.controller.netconf.cli.reader.impl.GenericReader;
import org.opendaylight.controller.netconf.cli.writer.WriteException;
import org.opendaylight.controller.netconf.cli.writer.Writer;
import org.opendaylight.controller.netconf.cli.writer.impl.GenericWriter;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.UnknownSchemaNode;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * The top level cli state that dispatches command executions
 */
public class Cli implements Runnable {
    private final CommandDispatcher commandRegistry;
    private final RpcImplementation rpcImplementation;
    private final ConsoleIO consoleIO;

    private final Map<Class<? extends Reader<DataSchemaNode>>, Reader<DataSchemaNode>> customReaders;

    public Cli(final ConsoleIO consoleIO, final CommandDispatcher commandRegistry, final RpcImplementation rpcImplementation, final SchemaContext remoteSchemaContext) {
        this.consoleIO = consoleIO;
        this.commandRegistry = commandRegistry;
        this.rpcImplementation = rpcImplementation;

        // TODO make custom readers extensible
        customReaders = Maps.newHashMap();
        customReaders.put(FilterReader.class, new FilterReader(this.consoleIO, remoteSchemaContext));
    }

    @Override
    public void run() {
        consoleIO.enterContext(new RootConsoleContext(commandRegistry));

        while (true) {
            try {
                final String commandName = consoleIO.read();
                final Map<QName, Command> commands = commandRegistry.getCommand(commandName);

                if (commands.size() != 1) {
                    // FIXME duplicate localname rpcs
                    continue;
                }
                final Command command = commands.values().iterator().next();
                try {
                    final Output response = command
                            .invoke(rpcImplementation, handleInput(command.getInputDefinition()));
                    handleOutput(command, response);
                } catch (final CommandInvocationException e) {
                    consoleIO.write(e.getMessage());
                }

            } catch (final IOException e) {
                throw new RuntimeException("IO failure", e);
            }
        }

    }

    private void handleOutput(final Command command, final Output response) {
        final Map<DataSchemaNode, List<Node<?>>> unwrap = response.unwrap(command.getOutputDefinition());

        final Writer<DataSchemaNode> outHandler = new GenericWriter(consoleIO, "");

        for (final DataSchemaNode schemaNode : unwrap.keySet()) {
            try {
                // TODO make writers extensible, same as readers
                // TODO implement custom writer for get/get-config anyxml to write output with schema
                outHandler.write(schemaNode, unwrap.get(schemaNode));
            } catch (final WriteException e) {
                throw new IllegalStateException("Unable to write value for: " + schemaNode.getQName() + " from: " + unwrap.get(schemaNode), e);
            }
        }
    }

    private Input handleInput(final InputDefinition inputDefinition) {
        final List<Node<?>> allArgs = Lists.newArrayList();

        final Reader<DataSchemaNode> argHandler = new GenericReader(consoleIO);
        for (final DataSchemaNode schemaNode : inputDefinition) {
            try {

                final Optional<Class<? extends Reader<DataSchemaNode>>> customReaderClassOpt = tryGetCustomReader(schemaNode);
                final List<Node<?>> read;

                if (customReaderClassOpt.isPresent()) {
                    final Reader<DataSchemaNode> customReaderInstance = customReaders.get(customReaderClassOpt.get());
                    Preconditions.checkNotNull(customReaderInstance, "Unknown custom reader: %s available readers: %s", customReaderClassOpt.get(), customReaders);
                    read = customReaderInstance.read(schemaNode);
                } else {
                    read = argHandler.read(schemaNode);
                }

                // FIXME read should not return null elements in list
                allArgs.addAll(Collections2.filter(read, new Predicate<Node<?>>() {
                    @Override
                    public boolean apply(@Nullable final Node<?> input) {
                        return input != null;
                    }
                }));
            } catch (final ReadingException e) {
                throw new IllegalStateException("Unable to read value for: " + schemaNode.getQName(), e);
            }
        }

        return new Input(allArgs);
    }

    private Optional<Class<? extends Reader<DataSchemaNode>>> tryGetCustomReader(final DataSchemaNode dataSchemaNode) {

        for (final UnknownSchemaNode unknownSchemaNode : dataSchemaNode.getUnknownSchemaNodes()) {

            final QName qName = unknownSchemaNode.getExtensionDefinition().getQName();
            if (qName.equals(CommandConstants.INPUT_ARG_HANDLER_EXT_QNAME)) {
                final String argumentHandlerClassName = unknownSchemaNode.getNodeParameter();
                try {
                    final Class<?> argumentClass = Class.forName(argumentHandlerClassName);
                    // TODO add check before cast
                    return Optional
                            .<Class<? extends Reader<DataSchemaNode>>> of((Class<? extends Reader<DataSchemaNode>>) argumentClass);
                } catch (final ClassNotFoundException e) {
                    throw new IllegalArgumentException("Unknown custom reader class " + argumentHandlerClassName
                            + " for: " + dataSchemaNode.getQName() + " available readers: " + customReaders);
                }
            }
        }

        return Optional.absent();
    }

    private static final class RootConsoleContext implements ConsoleContext {

        private final Completer completer;

        public RootConsoleContext(final CommandDispatcher commandRegistry) {
            // FIXME make top level completer more robust
            // e.g. duplicate local name, rpcs should be showed as "get-config(netconf)"
            completer = new StringsCompleter(Collections2.transform(commandRegistry.getCommandIds(),
                    new Function<QName, String>() {
                        @Override
                        public String apply(final QName input) {
                            return input.getLocalName();
                        }
                    }));
        }

        @Override
        public Completer getCompleter() {
            return completer;
        }

        @Override
        public String getPrompt() {
            return null;
        }
    }

}
