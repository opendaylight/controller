/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.opendaylight.controller.netconf.cli.commands.CommandDispatcher;
import org.opendaylight.controller.netconf.cli.commands.local.Connect;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.io.ConsoleIOImpl;
import org.opendaylight.controller.netconf.client.conf.NetconfClientConfiguration;
import org.opendaylight.controller.netconf.client.conf.NetconfReconnectingClientConfigurationBuilder;
import org.opendaylight.controller.netconf.nettyutil.handler.ssh.authentication.LoginPassword;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;

/**
 * Parse arguments, start remote device connection and start CLI after the connection is fully up
 */
public class Main {

    public static void main(final String[] args) {
        final CliArgumentParser cliArgs = new CliArgumentParser();
        try {
            cliArgs.parse(args);
        } catch (final ArgumentParserException e) {
            // Just end the cli, exception was handled by the CliArgumentParser
            return;
        }

        final ConsoleIO consoleIO;
        try {
            consoleIO = new ConsoleIOImpl();
        } catch (final IOException e) {
            handleStartupException(e);
            return;
        }

        final CommandDispatcher commandDispatcher = new CommandDispatcher();
        final CommandArgHandlerRegistry argumentHandlerRegistry = new CommandArgHandlerRegistry(consoleIO);
        final NetconfDeviceConnectionManager connectionManager = new NetconfDeviceConnectionManager(commandDispatcher, argumentHandlerRegistry);

        commandDispatcher.addLocalCommands(connectionManager);

        if(cliArgs.connectionArgsPresent()) {
            connectionManager.connectBlocking("cli", getClientConfig(cliArgs));
        }

        try {
            new Cli(consoleIO, commandDispatcher, argumentHandlerRegistry).run();
        } catch (final Exception e) {
            // TODO Running exceptions have to be handled properly
            handleRunningException(e);
            System.exit(0);
        }
    }

    private static NetconfReconnectingClientConfigurationBuilder getClientConfig(final CliArgumentParser cliArgs) {
        return NetconfReconnectingClientConfigurationBuilder.create().withAddress(cliArgs.getServerAddress())
                .withConnectionTimeoutMillis(Connect.DEFAULT_CONNECTION_TIMEOUT_MS).withReconnectStrategy(Connect.getReconnectStrategy())
                .withAuthHandler(cliArgs.getCredentials())
                .withProtocol(NetconfClientConfiguration.NetconfClientProtocol.SSH)
                .withConnectStrategyFactory(new ReconnectStrategyFactory() {
                    @Override
                    public ReconnectStrategy createReconnectStrategy() {
                        return Connect.getReconnectStrategy();
                    }
                });
    }

    private static void handleStartupException(final IOException e) {
        handleException(e, "Unable to initialize CLI");
    }

    private static void handleException(final Exception e, final String message) {
        System.err.println(message);
        e.printStackTrace(System.err);
    }

    private static void handleRunningException(final Exception e) {
        handleException(e, "CLI IO failed");
    }

    private static final class CliArgumentParser {

        public static final String USERNAME = "username";
        public static final String PASSWORD = "password";
        public static final String SERVER = "server";
        public static final String PORT = "port";

        private final ArgumentParser parser;
        private Namespace parsed;

        private CliArgumentParser() {
            parser = ArgumentParsers.newArgumentParser("Netconf cli").defaultHelp(true).description("Netconf cli");

            parser.addArgument("--" + SERVER).help("Specify address for a netconf " + SERVER);
            parser.addArgument("--" + PORT).setDefault("830").help("Specify " + PORT + " for a netconf " + SERVER);
            parser.addArgument("--" + USERNAME).help("Specify " + USERNAME);
            parser.addArgument("--" + PASSWORD).help("Specify " + PASSWORD);
        }

        public void parse(final String[] args) throws ArgumentParserException {
            try {
                this.parsed = parser.parseArgs(args);
            } catch (final ArgumentParserException e) {
                parser.handleError(e);
                throw e;
            }
        }

        public InetSocketAddress getServerAddress() {
            try {
                return new InetSocketAddress(InetAddress.getByName(getAddress()), getPort());
            } catch (final UnknownHostException e) {
                throw new IllegalArgumentException(e);
            }
        }

        private Integer getPort() {
            checkParsed();
            return Integer.valueOf(parsed.getString(PORT));
        }

        private String getAddress() {
            checkParsed();
            return parsed.getString(SERVER);
        }

        private void checkParsed() {
            Preconditions.checkState(parsed != null, "No arguments were parsed yet");
        }

        public String getUsername() {
            checkParsed();
            return getString(USERNAME);
        }

        private String getString(final String key) {
            return parsed.getString(key);
        }

        public LoginPassword getCredentials() {
            return new LoginPassword(getUsername(), getPassword());
        }

        public String getPassword() {
            checkParsed();
            return getString(PASSWORD);
        }

        public boolean connectionArgsPresent() {
            boolean present = true;
            present &= getAddress() != null;
            present &= getUsername() != null;
            present &= getPassword() != null;
            return present;
        }
    }
}
