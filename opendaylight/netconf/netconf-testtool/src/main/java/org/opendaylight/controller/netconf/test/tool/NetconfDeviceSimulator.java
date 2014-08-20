/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.test.tool;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.CharStreams;
import com.google.common.util.concurrent.CheckedFuture;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.AbstractMap;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.opendaylight.controller.netconf.api.monitoring.NetconfManagementSession;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.impl.DefaultCommitNotificationProducer;
import org.opendaylight.controller.netconf.impl.NetconfServerDispatcher;
import org.opendaylight.controller.netconf.impl.NetconfServerSessionNegotiatorFactory;
import org.opendaylight.controller.netconf.impl.SessionIdProvider;
import org.opendaylight.controller.netconf.impl.osgi.NetconfMonitoringServiceImpl;
import org.opendaylight.controller.netconf.impl.osgi.SessionMonitoringService;
import org.opendaylight.controller.netconf.mapping.api.Capability;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperation;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationProvider;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationService;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceSnapshot;
import org.opendaylight.controller.netconf.monitoring.osgi.NetconfMonitoringOperationService;
import org.opendaylight.controller.netconf.ssh.NetconfSSHServer;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaSourceException;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaSourceRepresentation;
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.model.repo.spi.PotentialSchemaSource;
import org.opendaylight.yangtools.yang.model.repo.spi.SchemaSourceListener;
import org.opendaylight.yangtools.yang.model.repo.util.FilesystemSchemaSourceCache;
import org.opendaylight.yangtools.yang.parser.builder.impl.BuilderUtils;
import org.opendaylight.yangtools.yang.parser.builder.impl.ModuleBuilder;
import org.opendaylight.yangtools.yang.parser.impl.YangParserListenerImpl;
import org.opendaylight.yangtools.yang.parser.repo.SharedSchemaRepository;
import org.opendaylight.yangtools.yang.parser.util.ASTSchemaSource;
import org.opendaylight.yangtools.yang.parser.util.TextToASTTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetconfDeviceSimulator implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfDeviceSimulator.class);

    public static final int CONNECTION_TIMEOUT_MILLIS = 20000;

    private final NioEventLoopGroup nettyThreadgroup;
    private final HashedWheelTimer hashedWheelTimer;
    private final List<Channel> devicesChannels = Lists.newArrayList();

    public NetconfDeviceSimulator() {
        this(new NioEventLoopGroup(), new HashedWheelTimer());
    }

    public NetconfDeviceSimulator(final NioEventLoopGroup eventExecutors, final HashedWheelTimer hashedWheelTimer) {
        this.nettyThreadgroup = eventExecutors;
        this.hashedWheelTimer = hashedWheelTimer;
    }

    private NetconfServerDispatcher createDispatcher(final Map<ModuleBuilder, String> moduleBuilders, final boolean exi) {

        final Set<Capability> capabilities = Sets.newHashSet(Collections2.transform(moduleBuilders.keySet(), new Function<ModuleBuilder, Capability>() {
            @Override
            public Capability apply(final ModuleBuilder input) {
                return new ModuleBuilderCapability(input, moduleBuilders.get(input));
            }
        }));

        final SessionIdProvider idProvider = new SessionIdProvider();

        final SimulatedOperationProvider simulatedOperationProvider = new SimulatedOperationProvider(idProvider, capabilities);
        final NetconfMonitoringOperationService monitoringService = new NetconfMonitoringOperationService(new NetconfMonitoringServiceImpl(simulatedOperationProvider));
        simulatedOperationProvider.addService(monitoringService);

        final DefaultCommitNotificationProducer commitNotifier = new DefaultCommitNotificationProducer(ManagementFactory.getPlatformMBeanServer());

        final Set<String> serverCapabilities = exi
                ? NetconfServerSessionNegotiatorFactory.DEFAULT_BASE_CAPABILITIES
                : Sets.newHashSet(XmlNetconfConstants.URN_IETF_PARAMS_NETCONF_BASE_1_0, XmlNetconfConstants.URN_IETF_PARAMS_NETCONF_BASE_1_1);

        final NetconfServerSessionNegotiatorFactory serverNegotiatorFactory = new NetconfServerSessionNegotiatorFactory(
                hashedWheelTimer, simulatedOperationProvider, idProvider, CONNECTION_TIMEOUT_MILLIS, commitNotifier, new LoggingMonitoringService(), serverCapabilities);

        final NetconfServerDispatcher.ServerChannelInitializer serverChannelInitializer = new NetconfServerDispatcher.ServerChannelInitializer(
                serverNegotiatorFactory);
        return new NetconfServerDispatcher(serverChannelInitializer, nettyThreadgroup, nettyThreadgroup);
    }

    private Map<ModuleBuilder, String> toModuleBuilders(final Map<SourceIdentifier, Map.Entry<ASTSchemaSource, YangTextSchemaSource>> sources) {
            final Map<SourceIdentifier, ParserRuleContext> asts = Maps.transformValues(sources,  new Function<Map.Entry<ASTSchemaSource, YangTextSchemaSource>, ParserRuleContext>() {
                @Override
                public ParserRuleContext apply(final Map.Entry<ASTSchemaSource, YangTextSchemaSource> input) {
                    return input.getKey().getAST();
                }
            });
            final Map<String, TreeMap<Date, URI>> namespaceContext = BuilderUtils.createYangNamespaceContext(
                    asts.values(), Optional.<SchemaContext>absent());

            final ParseTreeWalker walker = new ParseTreeWalker();
            final Map<ModuleBuilder, String> sourceToBuilder = new HashMap<>();

            for (final Map.Entry<SourceIdentifier, ParserRuleContext> entry : asts.entrySet()) {
                final ModuleBuilder moduleBuilder = YangParserListenerImpl.create(namespaceContext, entry.getKey().getName(),
                        walker, entry.getValue()).getModuleBuilder();

                try(InputStreamReader stream = new InputStreamReader(sources.get(entry.getKey()).getValue().openStream(), Charsets.UTF_8)) {
                    sourceToBuilder.put(moduleBuilder, CharStreams.toString(stream));
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
            }

            return sourceToBuilder;
        }


    public List<Integer> start(final Main.Params params) {
        final Map<ModuleBuilder, String> moduleBuilders = parseSchemasToModuleBuilders(params);

        final NetconfServerDispatcher dispatcher = createDispatcher(moduleBuilders, params.exi);

        int currentPort = params.startingPort;

        final List<Integer> openDevices = Lists.newArrayList();
        for (int i = 0; i < params.deviceCount; i++) {
            final InetSocketAddress address = getAddress(currentPort);

            final ChannelFuture server;
            if(params.ssh) {
                final LocalAddress tcpLocalAddress = new LocalAddress(address.toString());

                server = dispatcher.createLocalServer(tcpLocalAddress);
                try {
                    NetconfSSHServer.start(currentPort, tcpLocalAddress, new AcceptingAuthProvider(), nettyThreadgroup);
                } catch (final Exception e) {
                    LOG.warn("Cannot start simulated device on {}, skipping", address, e);
                    // Close local server and continue
                    server.cancel(true);
                    if(server.isDone()) {
                        server.channel().close();
                    }
                    continue;
                } finally {
                    currentPort++;
                }

                try {
                    server.get();
                } catch (final InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (final ExecutionException e) {
                    LOG.warn("Cannot start ssh simulated device on {}, skipping", address, e);
                    continue;
                }

                LOG.debug("Simulated SSH device started on {}", address);

            } else {
                server = dispatcher.createServer(address);
                currentPort++;

                try {
                    server.get();
                } catch (final InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (final ExecutionException e) {
                    LOG.warn("Cannot start tcp simulated device on {}, skipping", address, e);
                    continue;
                }

                LOG.debug("Simulated TCP device started on {}", address);
            }

            devicesChannels.add(server.channel());
            openDevices.add(currentPort - 1);

        }

        if(openDevices.size() == params.deviceCount) {
            LOG.info("All simulated devices started successfully from port {} to {}", params.startingPort, currentPort);
        } else {
            LOG.warn("Not all simulated devices started successfully. Started devices ar on ports {}", openDevices);
        }

        return openDevices;
    }

    private Map<ModuleBuilder, String> parseSchemasToModuleBuilders(final Main.Params params) {
        final SharedSchemaRepository consumer = new SharedSchemaRepository("netconf-simulator");
        consumer.registerSchemaSourceListener(TextToASTTransformer.create(consumer, consumer));

        final Set<SourceIdentifier> loadedSources = Sets.newHashSet();

        consumer.registerSchemaSourceListener(new SchemaSourceListener() {
            @Override
            public void schemaSourceEncountered(final SchemaSourceRepresentation schemaSourceRepresentation) {}

            @Override
            public void schemaSourceRegistered(final Iterable<PotentialSchemaSource<?>> potentialSchemaSources) {
                for (final PotentialSchemaSource<?> potentialSchemaSource : potentialSchemaSources) {
                    loadedSources.add(potentialSchemaSource.getSourceIdentifier());
                }
            }

            @Override
            public void schemaSourceUnregistered(final PotentialSchemaSource<?> potentialSchemaSource) {}
        });

        final FilesystemSchemaSourceCache<YangTextSchemaSource> cache = new FilesystemSchemaSourceCache<>(consumer, YangTextSchemaSource.class, params.schemasDir);
        consumer.registerSchemaSourceListener(cache);

        final Map<SourceIdentifier, Map.Entry<ASTSchemaSource, YangTextSchemaSource>> asts = Maps.newHashMap();
        for (final SourceIdentifier loadedSource : loadedSources) {
                try {
                    final CheckedFuture<ASTSchemaSource, SchemaSourceException> ast = consumer.getSchemaSource(loadedSource, ASTSchemaSource.class);
                    final CheckedFuture<YangTextSchemaSource, SchemaSourceException> text = consumer.getSchemaSource(loadedSource, YangTextSchemaSource.class);
                    asts.put(loadedSource, new AbstractMap.SimpleEntry<>(ast.get(), text.get()));
                } catch (final InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (final ExecutionException e) {
                    throw new RuntimeException("Cannot parse schema context", e);
                }
        }
        return toModuleBuilders(asts);
    }

    private static InetSocketAddress getAddress(final int port) {
        try {
            // TODO make address configurable
            return new InetSocketAddress(Inet4Address.getByName("0.0.0.0"), port);
        } catch (final UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        for (final Channel deviceCh : devicesChannels) {
            deviceCh.close();
        }
        nettyThreadgroup.shutdownGracefully();
        // close Everything
    }

    private static class SimulatedOperationProvider implements NetconfOperationProvider {
        private final SessionIdProvider idProvider;
        private final Set<NetconfOperationService> netconfOperationServices;


        public SimulatedOperationProvider(final SessionIdProvider idProvider, final Set<Capability> caps) {
            this.idProvider = idProvider;
            final SimulatedOperationService simulatedOperationService = new SimulatedOperationService(caps, idProvider.getCurrentSessionId());
            this.netconfOperationServices = Sets.<NetconfOperationService>newHashSet(simulatedOperationService);
        }

        @Override
        public NetconfOperationServiceSnapshot openSnapshot(final String sessionIdForReporting) {
            return new SimulatedServiceSnapshot(idProvider, netconfOperationServices);
        }

        public void addService(final NetconfOperationService monitoringService) {
            netconfOperationServices.add(monitoringService);
        }

        private static class SimulatedServiceSnapshot implements NetconfOperationServiceSnapshot {
            private final SessionIdProvider idProvider;
            private final Set<NetconfOperationService> netconfOperationServices;

            public SimulatedServiceSnapshot(final SessionIdProvider idProvider, final Set<NetconfOperationService> netconfOperationServices) {
                this.idProvider = idProvider;
                this.netconfOperationServices = netconfOperationServices;
            }

            @Override
            public String getNetconfSessionIdForReporting() {
                return String.valueOf(idProvider.getCurrentSessionId());
            }

            @Override
            public Set<NetconfOperationService> getServices() {
                return netconfOperationServices;
            }

            @Override
            public void close() throws Exception {}
        }

        static class SimulatedOperationService implements NetconfOperationService {
            private final Set<Capability> capabilities;
            private static SimulatedGet sGet;

            public SimulatedOperationService(final Set<Capability> capabilities, final long currentSessionId) {
                this.capabilities = capabilities;
                sGet = new SimulatedGet(String.valueOf(currentSessionId));
            }

            @Override
            public Set<Capability> getCapabilities() {
                return capabilities;
            }

            @Override
            public Set<NetconfOperation> getNetconfOperations() {
                return Sets.<NetconfOperation>newHashSet(sGet);
            }

            @Override
            public void close() {
            }

        }
    }

    private class LoggingMonitoringService implements SessionMonitoringService {
        @Override
        public void onSessionUp(final NetconfManagementSession session) {
            LOG.debug("Session {} established", session);
        }

        @Override
        public void onSessionDown(final NetconfManagementSession session) {
            LOG.debug("Session {} down", session);
        }
    }

}
