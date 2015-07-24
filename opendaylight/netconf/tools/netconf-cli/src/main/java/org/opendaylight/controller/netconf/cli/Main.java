/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli;

import static com.google.common.base.Throwables.getStackTraceAsString;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentGroup;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.opendaylight.controller.netconf.cli.commands.CommandDispatcher;
import org.opendaylight.controller.netconf.cli.commands.local.Connect;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.io.ConsoleIOImpl;
import org.opendaylight.controller.netconf.client.conf.NetconfClientConfiguration;
import org.opendaylight.controller.netconf.client.conf.NetconfClientConfigurationBuilder;
import org.opendaylight.controller.netconf.nettyutil.handler.ssh.authentication.LoginPassword;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Parse arguments, start remote device connection and start CLI after the
 * connection is fully up
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

        final SchemaContext localSchema = CommandDispatcher.parseSchema(CommandDispatcher.LOCAL_SCHEMA_PATHS);
        final SchemaContextRegistry schemaContextRegistry = new SchemaContextRegistry(localSchema);

        final CommandDispatcher commandDispatcher = new CommandDispatcher();
        final CommandArgHandlerRegistry argumentHandlerRegistry = new CommandArgHandlerRegistry(consoleIO,
                schemaContextRegistry);
        final NetconfDeviceConnectionManager connectionManager = new NetconfDeviceConnectionManager(commandDispatcher,
                argumentHandlerRegistry, schemaContextRegistry, consoleIO);

        commandDispatcher.addLocalCommands(connectionManager, localSchema, cliArgs.getConnectionTimeoutMs());

        switch (cliArgs.connectionArgsPresent()) {
        case TCP: {
            // FIXME support pure TCP
            handleRunningException(new UnsupportedOperationException("PURE TCP CONNECTIONS ARE NOT SUPPORTED YET, USE SSH INSTEAD BY PROVIDING USERNAME AND PASSWORD AS WELL"));
            return;
        }
        case SSH: {
            writeStatus(consoleIO, "Connecting to %s via SSH. Please wait.", cliArgs.getAddress());
            connectionManager.connectBlocking(cliArgs.getAddress(), cliArgs.getServerAddress(), getClientSshConfig(cliArgs));
            break;
        }
        case NONE: {/* Do not connect initially */
            writeStatus(consoleIO, "No initial connection. To connect use the connect command");
        }
        }

        try {
            new Cli(consoleIO, commandDispatcher, argumentHandlerRegistry, schemaContextRegistry).run();
        } catch (final Exception e) {
            // TODO Running exceptions have to be handled properly
            handleRunningException(e);
            System.exit(0);
        }
    }

    private static NetconfClientConfigurationBuilder getClientConfig(final CliArgumentParser cliArgs) {
        return NetconfClientConfigurationBuilder.create().withAddress(cliArgs.getServerAddress())
                .withConnectionTimeoutMillis(cliArgs.getConnectionTimeoutMs())
                .withReconnectStrategy(Connect.getReconnectStrategy())
                .withProtocol(NetconfClientConfiguration.NetconfClientProtocol.TCP);
    }

    private static NetconfClientConfigurationBuilder getClientSshConfig(final CliArgumentParser cliArgs) {
        return NetconfClientConfigurationBuilder.create().withAddress(cliArgs.getServerAddress())
                .withConnectionTimeoutMillis(cliArgs.getConnectionTimeoutMs())
                .withReconnectStrategy(Connect.getReconnectStrategy())
                .withAuthHandler(cliArgs.getCredentials())
                .withProtocol(NetconfClientConfiguration.NetconfClientProtocol.SSH);
    }

    private static void handleStartupException(final IOException e) {
        handleException(e, "Unable to initialize CLI");
    }

    private static void handleException(final Exception e, final String message) {
        System.console().writer().println(String.format("Error %s cause %s", message, getStackTraceAsString(e.fillInStackTrace())));
    }

    private static void writeStatus(final ConsoleIO io, final String blueprint, final Object... args) {
        try {
            io.formatLn(blueprint, args);
        } catch (final IOException e) {
            handleStartupException(e);
        }
    }

    private static void handleRunningException(final Exception e) {
        handleException(e, "Unexpected CLI runtime exception");
    }

    private static final class CliArgumentParser {

        public static final String USERNAME = "username";
        public static final String PASSWORD = "password";
        public static final String SERVER = "server";
        public static final String PORT = "port";

        public static final String CONNECT_TIMEOUT = "connectionTimeout";
        public static final int DEFAULT_CONNECTION_TIMEOUT_MS = 50000;

        private final ArgumentParser parser;
        private Namespace parsed;

        private CliArgumentParser() {
            parser = ArgumentParsers.newArgumentParser("Netconf cli").defaultHelp(true)
                    .description("Generic cli for netconf devices")
                    .usage("Submit address + port for initial TCP connection (PURE TCP CONNECTIONS ARE NOT SUPPORTED YET)\n" +
                            "Submit username + password in addition to address + port for initial SSH connection\n" +
                            "If no arguments(or unexpected combination) is submitted, cli will be started without initial connection\n" +
                            "To use with ODL controller, run with: java -jar netconf-cli-0.2.5-SNAPSHOT-executable.jar  --server localhost --port 1830 --username admin --password admin");

            final ArgumentGroup tcpGroup = parser.addArgumentGroup("TCP")
                    .description("Base arguments to initiate TCP connection right away");

            tcpGroup.addArgument("--" + SERVER).help("Netconf device ip-address/domain name");
            tcpGroup.addArgument("--" + PORT).type(Integer.class).help("Netconf device port");
            tcpGroup.addArgument("--" + CONNECT_TIMEOUT)
                    .type(Integer.class)
                    .setDefault(DEFAULT_CONNECTION_TIMEOUT_MS)
                    .help("Timeout(in ms) for connection to succeed, if the connection is not fully established by the time is up, " +
                            "connection attempt is considered a failure. This attribute is not working as expected yet");

            final ArgumentGroup sshGroup = parser.addArgumentGroup("SSH")
                    .description("SSH credentials, if provided, initial connection will be attempted using SSH");

            sshGroup.addArgument("--" + USERNAME).help("Username for SSH connection");
            sshGroup.addArgument("--" + PASSWORD).help("Password for SSH connection");
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
            return parsed.getInt(PORT);
        }

        private String getAddress() {
            checkParsed();
            return getString(SERVER);
        }

        private Integer getConnectionTimeoutMs() {
            checkParsed();
            return parsed.getInt(CONNECT_TIMEOUT);
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

        public InitialConnectionType connectionArgsPresent() {
            if(getAddress() != null && getPort() != null) {
                if(getUsername() != null && getPassword() != null) {
                    return InitialConnectionType.SSH;
                }
                return InitialConnectionType.TCP;
            }
            return InitialConnectionType.NONE;
        }

        enum InitialConnectionType {
            TCP, SSH, NONE
        }
    }
}
