/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.io.File;
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

public class Main {

    public static void main(final String[] args) {
        final ArgumentParser parser = ArgumentParsers.newArgumentParser("Checksum").defaultHelp(true)
                .description("Calculate checksum of given files.");
        parser.addArgument("--server").setDefault("localhost").help("Specify address for a netconf server");
        parser.addArgument("--port").setDefault("830").help("Specify port for a netconf server");
        parser.addArgument("--username").required(true).help("Specify username");
        parser.addArgument("--password").required(true).help("Specify password");
        // TODO refactor arguments

        final Namespace ns;
        try {
            ns = parser.parseArgs(args);
        } catch (final ArgumentParserException e) {
            parser.handleError(e);
            return;
        }

        final RemoteDeviceId deviceId = new RemoteDeviceId("console");
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final DeviceCliHandler handler = new DeviceCliHandler();

        final NetconfDevice device = NetconfDevice.createNetconfDevice(deviceId, getGlobalNetconfSchemaProvider(),
                executor, handler);
        final NetconfDeviceCommunicator listener = new NetconfDeviceCommunicator(deviceId, device);

        // FIXME get client configuration from args or read them from a local
        // Connect command
        final NetconfReconnectingClientConfiguration clientConfig = getClientConfig(ns, listener);

        final NetconfClientDispatcher dispatcher = getClientDispatcher();

        // TODO make initial connect optional, move to connect command
        listener.initializeRemoteConnection(dispatcher, clientConfig);
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

    private static NetconfReconnectingClientConfiguration getClientConfig(final Namespace ns,
            final NetconfDeviceCommunicator listener) {
        final InetSocketAddress socketAddress;
        try {
            // FIXME
            socketAddress = new InetSocketAddress(InetAddress.getByName(ns.getString("server")), Integer.valueOf(ns
                    .getString("port")));
        } catch (final UnknownHostException e) {
            // FIXME
            throw new RuntimeException(e);
        }
        final ReconnectStrategy strategy = getReconnectStrategy();
        final long clientConnectionTimeoutMillis = 500;

        return NetconfReconnectingClientConfigurationBuilder.create().withAddress(socketAddress)
                .withConnectionTimeoutMillis(clientConnectionTimeoutMillis).withReconnectStrategy(strategy)
                .withSessionListener(listener)
                .withAuthHandler(new LoginPassword(ns.getString("username"), ns.getString("password")))
                .withProtocol(NetconfClientConfiguration.NetconfClientProtocol.SSH)
                .withConnectStrategyFactory(new ReconnectStrategyFactory() {
                    @Override
                    public ReconnectStrategy createReconnectStrategy() {
                        return getReconnectStrategy();
                    }
                }).build();
    }

    private static ReconnectStrategy getReconnectStrategy() {

        final double sleepFactor = 1.0;
        final int minSleep = 5000;
        final Long maxSleep = null;
        final Long deadline = null;

        return new TimedReconnectStrategy(GlobalEventExecutor.INSTANCE, 10000, minSleep, sleepFactor, maxSleep, null,
                deadline);
    }

}
