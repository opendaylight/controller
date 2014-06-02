/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.cli.io.ConsoleIOImpl;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcher;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcherImpl;
import org.opendaylight.controller.netconf.client.conf.NetconfClientConfiguration;
import org.opendaylight.controller.netconf.client.conf.NetconfReconnectingClientConfiguration;
import org.opendaylight.controller.netconf.client.conf.NetconfReconnectingClientConfigurationBuilder;
import org.opendaylight.controller.netconf.nettyutil.handler.ssh.authentication.LoginPassword;
import org.opendaylight.controller.sal.connect.netconf.NetconfDevice;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfDeviceCommunicator;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.protocol.framework.TimedReconnectStrategy;
import org.opendaylight.yangtools.yang.model.util.repo.AbstractCachingSchemaSourceProvider;
import org.opendaylight.yangtools.yang.model.util.repo.FilesystemSchemaCachingProvider;
import org.opendaylight.yangtools.yang.model.util.repo.SchemaSourceProvider;
import org.opendaylight.yangtools.yang.model.util.repo.SchemaSourceProviders;

import com.google.common.base.Preconditions;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * Parse arguments, start remote device connection and start CLI after the connection is fully up
 */
public class Main {

    // TODO add readme file
    public static void main(final String[] args) {
        final CliArgumentParser cliArgs = new CliArgumentParser();
        try {
            cliArgs.parse(args);
        } catch (final ArgumentParserException e) {
            return;
        }

        ConsoleIO consoleIO;
        try {
            consoleIO = new ConsoleIOImpl();
        } catch (final IOException e) {
            handleStartupException(e);
            return;
        }

        final RemoteDeviceId deviceId = new RemoteDeviceId(cliArgs.getServerAddress().toString());
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final DeviceCliHandler handler = new DeviceCliHandler(consoleIO);

        final NetconfDevice device = NetconfDevice.createNetconfDevice(deviceId, getGlobalNetconfSchemaProvider(),
                executor, handler);
        final NetconfDeviceCommunicator listener = new NetconfDeviceCommunicator(deviceId, device);

        // FIXME get client configuration from args or read them from a local
        // Connect command
        final NetconfReconnectingClientConfiguration clientConfig = getClientConfig(cliArgs, listener);

        // TODO make initial connect optional, move to connect command
        try {
            consoleIO.writeLn("Initializing remote connection to " + cliArgs.getServerAddress() + " as " + cliArgs.getUsername());
        } catch (final IOException e) {
            handleStartupException(e);
            return;
        }
        final NetconfClientDispatcher dispatcher = getClientDispatcher();
        listener.initializeRemoteConnection(dispatcher, clientConfig);
    }

    private static void handleStartupException(final IOException e) {
        System.err.println("Unable to initialize CLI");
        e.printStackTrace(System.err);
    }

    private static AbstractCachingSchemaSourceProvider<String, InputStream> getGlobalNetconfSchemaProvider() {
        // FIXME move to args
        final String storageFile = "cache/schema";
        final File directory = new File(storageFile);
        final SchemaSourceProvider<String> defaultProvider = SchemaSourceProviders.noopProvider();
        return FilesystemSchemaCachingProvider.createFromStringSourceProvider(defaultProvider, directory);
    }

    private static NetconfClientDispatcher getClientDispatcher() {
        final EventLoopGroup nettyThreadGroup = new NioEventLoopGroup();
        return new NetconfClientDispatcherImpl(nettyThreadGroup, nettyThreadGroup, new HashedWheelTimer());
    }

    private static NetconfReconnectingClientConfiguration getClientConfig(final CliArgumentParser args,
            final NetconfDeviceCommunicator listener) {

        final ReconnectStrategy strategy = getReconnectStrategy();
        final long clientConnectionTimeoutMillis = 50000;

        return NetconfReconnectingClientConfigurationBuilder.create().withAddress(args.getServerAddress())
                .withConnectionTimeoutMillis(clientConnectionTimeoutMillis).withReconnectStrategy(strategy)
                .withSessionListener(listener)
                .withAuthHandler(args.getCredentials())
                .withProtocol(NetconfClientConfiguration.NetconfClientProtocol.SSH)
                .withConnectStrategyFactory(new ReconnectStrategyFactory() {
                    @Override
                    public ReconnectStrategy createReconnectStrategy() {
                        return getReconnectStrategy();
                    }
                }).build();
    }

    private static ReconnectStrategy getReconnectStrategy() {
        // FIXME move to args
        final double sleepFactor = 1.0;
        final int minSleep = 5000;
        final Long maxSleep = null;
        final Long deadline = null;

        return new TimedReconnectStrategy(GlobalEventExecutor.INSTANCE, 10000, minSleep, sleepFactor, maxSleep, null,
                deadline);
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
            parser.addArgument("--" + USERNAME).required(true).help("Specify " + USERNAME);
            parser.addArgument("--" + PASSWORD).required(true).help("Specify " + PASSWORD);
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
            checkParsed();
            try {
                return new InetSocketAddress(InetAddress.getByName(parsed.getString(SERVER)), Integer.valueOf(parsed.getString(PORT)));
            } catch (final UnknownHostException e) {
                throw new IllegalArgumentException(e);
            }
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
    }
}
