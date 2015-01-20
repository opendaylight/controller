/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli;

import com.google.common.base.Preconditions;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.opendaylight.controller.netconf.cli.commands.CommandDispatcher;
import org.opendaylight.controller.netconf.cli.io.ConsoleIO;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcherImpl;
import org.opendaylight.controller.netconf.client.conf.NetconfClientConfigurationBuilder;
import org.opendaylight.controller.sal.connect.netconf.NetconfDevice;
import org.opendaylight.controller.sal.connect.netconf.NetconfDevice.SchemaResourcesDTO;
import org.opendaylight.controller.sal.connect.netconf.NetconfStateSchemas.NetconfStateSchemasResolverImpl;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfDeviceCommunicator;
import org.opendaylight.controller.sal.connect.netconf.schema.mapping.NetconfMessageTransformer;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaContextFactory;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaSourceFilter;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.model.repo.util.FilesystemSchemaSourceCache;
import org.opendaylight.yangtools.yang.model.util.repo.AbstractCachingSchemaSourceProvider;
import org.opendaylight.yangtools.yang.model.util.repo.FilesystemSchemaCachingProvider;
import org.opendaylight.yangtools.yang.model.util.repo.SchemaSourceProvider;
import org.opendaylight.yangtools.yang.model.util.repo.SchemaSourceProviders;
import org.opendaylight.yangtools.yang.parser.repo.SharedSchemaRepository;
import org.opendaylight.yangtools.yang.parser.util.TextToASTTransformer;

/**
 * Manages connect/disconnect to 1 remote device
 */
public class NetconfDeviceConnectionManager implements Closeable {

    private final CommandDispatcher commandDispatcher;
    private final SchemaContextRegistry schemaContextRegistry;
    private final ConsoleIO console;

    private final ExecutorService executor;
    private final NioEventLoopGroup nettyThreadGroup;
    private final NetconfClientDispatcherImpl netconfClientDispatcher;

    private static final String CACHE = "cache/schema";

    // Connection
    private NetconfDeviceConnectionHandler handler;
    private NetconfDevice device;
    private NetconfDeviceCommunicator listener;

    public NetconfDeviceConnectionManager(final CommandDispatcher commandDispatcher,
            final CommandArgHandlerRegistry argumentHandlerRegistry, final SchemaContextRegistry schemaContextRegistry,
            final ConsoleIO consoleIO) {
        this.commandDispatcher = commandDispatcher;
        this.schemaContextRegistry = schemaContextRegistry;
        this.console = consoleIO;

        executor = Executors.newSingleThreadExecutor();
        nettyThreadGroup = new NioEventLoopGroup();
        netconfClientDispatcher = new NetconfClientDispatcherImpl(nettyThreadGroup, nettyThreadGroup,
                new HashedWheelTimer());
    }

    // TODO we receive configBuilder in order to add SessionListener, Session
    // Listener should not be part of config
    public synchronized void connect(final String name, final InetSocketAddress address, final NetconfClientConfigurationBuilder configBuilder) {
        // TODO change IllegalState exceptions to custom ConnectionException
        Preconditions.checkState(listener == null, "Already connected");

        final RemoteDeviceId deviceId = new RemoteDeviceId(name, address);

        handler = new NetconfDeviceConnectionHandler(commandDispatcher, schemaContextRegistry,
                console, name);

        final SharedSchemaRepository repository = new SharedSchemaRepository("repo");
        final SchemaContextFactory schemaContextFactory = repository.createSchemaContextFactory(SchemaSourceFilter.ALWAYS_ACCEPT);
        final FilesystemSchemaSourceCache<YangTextSchemaSource> cache = new FilesystemSchemaSourceCache<>(repository, YangTextSchemaSource.class, new File(CACHE));
        repository.registerSchemaSourceListener(cache);
        repository.registerSchemaSourceListener(TextToASTTransformer.create(repository, repository));

        device = new NetconfDevice(new SchemaResourcesDTO(repository, schemaContextFactory, new NetconfStateSchemasResolverImpl()),
                deviceId, handler, executor, new NetconfMessageTransformer());
        listener = new NetconfDeviceCommunicator(deviceId, device);
        configBuilder.withSessionListener(listener);
        listener.initializeRemoteConnection(netconfClientDispatcher, configBuilder.build());
    }

    /**
     * Blocks thread until connection is fully established
     */
    public synchronized Set<String> connectBlocking(final String name, final InetSocketAddress address, final NetconfClientConfigurationBuilder configBuilder) {
        this.connect(name, address, configBuilder);
        synchronized (handler) {
            while (handler.isUp() == false) {
                try {
                    // TODO implement Timeout for unsuccessful connection
                    handler.wait();
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalArgumentException(e);
                }
            }
        }

        return commandDispatcher.getRemoteCommandIds();
    }

    public synchronized void disconnect() {
        Preconditions.checkState(listener != null, "Not connected yet");
        Preconditions.checkState(handler.isUp(), "Not connected yet");
        listener.close();
        listener = null;
        device = null;
        handler.close();
        handler = null;
    }

    private static AbstractCachingSchemaSourceProvider<String, InputStream> getGlobalNetconfSchemaProvider() {
        // FIXME move to args
        final String storageFile = "cache/schema";
        final File directory = new File(storageFile);
        final SchemaSourceProvider<String> defaultProvider = SchemaSourceProviders.noopProvider();
        return FilesystemSchemaCachingProvider.createFromStringSourceProvider(defaultProvider, directory);
    }

    @Override
    public void close() throws IOException {
        executor.shutdownNow();
        nettyThreadGroup.shutdownGracefully();
    }
}
