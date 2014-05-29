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
import org.opendaylight.controller.netconf.cli.commands.CommandDispatcher;
import org.opendaylight.controller.netconf.cli.commands.CommandInvocationException;
import org.opendaylight.controller.netconf.cli.commands.input.Input;
import org.opendaylight.controller.netconf.cli.commands.input.InputDefinition;
import org.opendaylight.controller.netconf.cli.commands.output.Output;
import org.opendaylight.controller.netconf.cli.io.ConsoleContext;
import org.opendaylight.controller.netconf.cli.io.ConsoleIOImpl;
import org.opendaylight.controller.netconf.cli.reader.Reader;
import org.opendaylight.controller.netconf.cli.reader.ReadingException;
import org.opendaylight.controller.netconf.cli.reader.impl.GenericReader;
import org.opendaylight.controller.netconf.cli.writer.WriteException;
import org.opendaylight.controller.netconf.cli.writer.Writer;
import org.opendaylight.controller.netconf.cli.writer.impl.GenericWriter;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

public class Cli implements Runnable {
    private final CommandDispatcher commandRegistry;
    private final RpcImplementation rpcImplementation;
    private final ConsoleIOImpl consoleIO;

    public Cli(final CommandDispatcher commandRegistry, final RpcImplementation rpcImplementation) {
        this.commandRegistry = commandRegistry;
        this.rpcImplementation = rpcImplementation;
        try {
            // TODO instantiate IO sooner and inject here
            // TODO add status output during initialization
            consoleIO = new ConsoleIOImpl();
        } catch (final IOException e) {
            // FIXME remove ex from ConsoleIOImpl
            throw new RuntimeException(e);
        }

        // TODO start the CLI here and read commands based on commandRegistry
    }

    @Override
    public void run() {
        consoleIO.enterContext(new RootConsoleContext(commandRegistry));
        // FIXME end
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
                    Output response = command.invoke(rpcImplementation, handleInput(command.getInputDefinition()));
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

        final Writer<DataSchemaNode> outHandler = new GenericWriter(consoleIO, "  ");

        for (final DataSchemaNode schema : unwrap.keySet()) {
            try {
                // TODO remove indent,
                // TODO make writers extensible, same as readers
                outHandler.write(schema, unwrap.get(schema));
            } catch (final WriteException e) {
                // FIXME
                throw new RuntimeException(e);
            }
        }
    }

    private Input handleInput(final InputDefinition inputDefinition) {
        final List<Node<?>> allArgs = Lists.newArrayList();

        final Reader<DataSchemaNode> argHandler = new GenericReader(consoleIO);
        for (final DataSchemaNode dataSchemaNode : inputDefinition) {
            try {
                // TODO add option to provide custom argument handlers
                // e.g. handle data for edit-config according to device schema
                // e.g.2. handle filter for get-config with schema as well,
                // maybe in format of a path

                final List<Node<?>> read = argHandler.read(dataSchemaNode);
                // FIXME read should not return null elements in list
                allArgs.addAll(Collections2.filter(read, new Predicate<Node<?>>() {
                    @Override
                    public boolean apply(@Nullable final Node<?> input) {
                        return input != null;
                    }
                }));
            } catch (final ReadingException e) {
                // FIXME
                throw new RuntimeException(e);
            }
        }

        return new Input(allArgs);
    }

    private static final class RootConsoleContext implements ConsoleContext {

        private final Completer completer;

        public RootConsoleContext(final CommandDispatcher commandRegistry) {
            // FIXME make top level completer more robust
            // e.g. duplicate local name
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
            return "";
        }
    }

}
