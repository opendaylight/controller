/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import jline.console.UserInterruptException;
import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;
import org.opendaylight.controller.netconf.cli.commands.Command;
import org.opendaylight.controller.netconf.cli.commands.CommandConstants;
import org.opendaylight.controller.netconf.cli.commands.CommandDispatcher;
import org.opendaylight.controller.netconf.cli.commands.CommandInvocationException;
import org.opendaylight.controller.netconf.cli.commands.input.Input;
import org.opendaylight.controller.netconf.cli.commands.input.InputDefinition;
import org.opendaylight.controller.netconf.cli.commands.output.Output;
import org.opendaylight.controller.netconf.cli.commands.output.OutputDefinition;
import org.opendaylight.controller.netconf.cli.io.ConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.reader.ReadingException;
import org.opendaylight.controller.netconf.cli.writer.OutFormatter;
import org.opendaylight.controller.netconf.cli.writer.WriteException;
import org.opendaylight.controller.netconf.cli.writer.Writer;
import org.opendaylight.controller.netconf.cli.writer.impl.CompositeNodeWriter;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.UnknownSchemaNode;

/**
 * The top level cli state that dispatches command executions
 */
public class Cli implements Runnable {
    private final CommandDispatcher commandRegistry;
    private final CommandArgHandlerRegistry argumentHandlerRegistry;
    private final SchemaContextRegistry schemaContextRegistry;
    private final ConsoleIO consoleIO;

    public Cli(final ConsoleIO consoleIO, final CommandDispatcher commandRegistry,
            final CommandArgHandlerRegistry argumentHandlerRegistry, final SchemaContextRegistry schemaContextRegistry) {
        this.consoleIO = consoleIO;
        this.commandRegistry = commandRegistry;
        this.argumentHandlerRegistry = argumentHandlerRegistry;
        this.schemaContextRegistry = schemaContextRegistry;
    }

    @Override
    public void run() {
        try {
            consoleIO.writeLn("Cli is up, available commands:");
            final RootConsoleContext consoleContext = new RootConsoleContext(commandRegistry);
            consoleIO.enterContext(consoleContext);
            consoleIO.complete();
            consoleIO.writeLn("");

            while (true) {
                final String commandName = consoleIO.read();
                final Optional<Command> commandOpt = commandRegistry.getCommand(commandName);

                if (commandOpt.isPresent() == false) {
                    continue;
                }

                final Command command = commandOpt.get();
                try {
                    consoleIO.enterContext(command.getConsoleContext());
                    final Output response = command.invoke(handleInput(command.getInputDefinition()));
                    handleOutput(command, response);
                } catch (final CommandInvocationException e) {
                    consoleIO.write(e.getMessage());
                } catch (final UserInterruptException e) {
                    consoleIO.writeLn("Command " + command.getCommandId() + " was terminated.");
                } finally {
                    consoleIO.leaveContext();
                }

            }
        } catch (final IOException e) {
            throw new RuntimeException("IO failure", e);
        }
    }

    private void handleOutput(final Command command, final Output response) {
        final OutputDefinition outputDefinition = command.getOutputDefinition();

        final Writer<DataSchemaNode> outHandler = argumentHandlerRegistry.getGenericWriter();
        if (outputDefinition.isEmpty()) {
            handleEmptyOutput(command, response);
        } else {
            handleRegularOutput(response, outputDefinition, outHandler);
        }
    }

    private void handleRegularOutput(final Output response, final OutputDefinition outputDefinition,
            final Writer<DataSchemaNode> outHandler) {
        final Map<DataSchemaNode, List<Node<?>>> unwrap = response.unwrap(outputDefinition);

        for (final DataSchemaNode schemaNode : unwrap.keySet()) {
            Preconditions.checkNotNull(schemaNode);

            try {

                // FIXME move custom writer to GenericWriter/Serializers ...
                // this checks only first level
                final Optional<Class<? extends Writer<DataSchemaNode>>> customReaderClassOpt = tryGetCustomHandler(schemaNode);

                if (customReaderClassOpt.isPresent()) {
                    final Writer<DataSchemaNode> customReaderInstance = argumentHandlerRegistry
                            .getCustomWriter(customReaderClassOpt.get());
                    Preconditions.checkNotNull(customReaderInstance, "Unknown custom writer: %s",
                            customReaderClassOpt.get());
                    customReaderInstance.write(schemaNode, unwrap.get(schemaNode));
                } else {
                    outHandler.write(schemaNode, unwrap.get(schemaNode));
                }

            } catch (final WriteException e) {
                throw new IllegalStateException("Unable to write value for: " + schemaNode.getQName() + " from: "
                        + unwrap.get(schemaNode), e);
            }
        }
    }

    private void handleEmptyOutput(final Command command, final Output response) {
        try {
            new CompositeNodeWriter(consoleIO, new OutFormatter()).write(null,
                    Collections.<Node<?>> singletonList(response.getOutput()));
        } catch (final WriteException e) {
            throw new IllegalStateException("Unable to write value for: " + response.getOutput().getNodeType()
                    + " from: " + command.getCommandId(), e);
        }
    }

    private Input handleInput(final InputDefinition inputDefinition) {
        List<Node<?>> allArgs = Collections.emptyList();
        try {
            if (!inputDefinition.isEmpty()) {
                allArgs = argumentHandlerRegistry.getGenericReader(schemaContextRegistry.getLocalSchemaContext()).read(
                        inputDefinition.getInput());
            }
        } catch (final ReadingException e) {
            throw new IllegalStateException("Unable to read value for: " + inputDefinition.getInput().getQName(), e);
        }

        return new Input(allArgs);
    }

    // TODO move tryGet to GenericWriter, GenericReader has the same code
    private <T> Optional<Class<? extends T>> tryGetCustomHandler(final DataSchemaNode dataSchemaNode) {

        for (final UnknownSchemaNode unknownSchemaNode : dataSchemaNode.getUnknownSchemaNodes()) {

            if (isExtenstionForCustomHandler(unknownSchemaNode)) {
                final String argumentHandlerClassName = unknownSchemaNode.getNodeParameter();
                try {
                    final Class<?> argumentClass = Class.forName(argumentHandlerClassName);
                    // TODO add check before cast
                    return Optional.<Class<? extends T>> of((Class<? extends T>) argumentClass);
                } catch (final ClassNotFoundException e) {
                    throw new IllegalArgumentException("Unknown custom reader class " + argumentHandlerClassName
                            + " for: " + dataSchemaNode.getQName());
                }
            }
        }

        return Optional.absent();
    }

    private boolean isExtenstionForCustomHandler(final UnknownSchemaNode unknownSchemaNode) {
        final QName qName = unknownSchemaNode.getExtensionDefinition().getQName();
        return qName.equals(CommandConstants.ARG_HANDLER_EXT_QNAME);
    }

    private static final class RootConsoleContext implements ConsoleContext {

        private final Completer completer;

        public RootConsoleContext(final CommandDispatcher commandRegistry) {
            completer = new CommandCompleter(commandRegistry);
        }

        @Override
        public Completer getCompleter() {
            return completer;
        }

        @Override
        public Optional<String> getPrompt() {
            return Optional.absent();
        }

        private class CommandCompleter extends StringsCompleter {

            private final CommandDispatcher commandRegistry;

            public CommandCompleter(final CommandDispatcher commandRegistry) {
                this.commandRegistry = commandRegistry;
            }

            @Override
            public int complete(final String buffer, final int cursor, final List<CharSequence> candidates) {
                getStrings().clear();
                getStrings().addAll(commandRegistry.getCommandIds());
                return super.complete(buffer, cursor, candidates);
            }
        }
    }

}
