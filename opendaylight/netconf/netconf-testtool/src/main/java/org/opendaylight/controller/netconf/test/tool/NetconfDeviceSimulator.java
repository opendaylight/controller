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
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.CharStreams;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.BindException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.sshd.common.util.ThreadUtils;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.keyprovider.PEMGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.opendaylight.controller.netconf.api.Capability;
import org.opendaylight.controller.netconf.api.monitoring.CapabilityListener;
import org.opendaylight.controller.netconf.api.monitoring.NetconfMonitoringService;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.impl.DefaultCommitNotificationProducer;
import org.opendaylight.controller.netconf.impl.NetconfServerDispatcherImpl;
import org.opendaylight.controller.netconf.impl.NetconfServerSessionNegotiatorFactory;
import org.opendaylight.controller.netconf.impl.SessionIdProvider;
import org.opendaylight.controller.netconf.impl.osgi.AggregatedNetconfOperationServiceFactory;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperation;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationService;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory;
import org.opendaylight.controller.netconf.monitoring.osgi.NetconfMonitoringActivator;
import org.opendaylight.controller.netconf.monitoring.osgi.NetconfMonitoringOperationService;
import org.opendaylight.controller.netconf.ssh.SshProxyServer;
import org.opendaylight.controller.netconf.ssh.SshProxyServerConfiguration;
import org.opendaylight.controller.netconf.ssh.SshProxyServerConfigurationBuilder;
import org.opendaylight.controller.netconf.test.tool.rpc.DataList;
import org.opendaylight.controller.netconf.test.tool.rpc.SimulatedCommit;
import org.opendaylight.controller.netconf.test.tool.rpc.SimulatedCreateSubscription;
import org.opendaylight.controller.netconf.test.tool.rpc.SimulatedEditConfig;
import org.opendaylight.controller.netconf.test.tool.rpc.SimulatedGet;
import org.opendaylight.controller.netconf.test.tool.rpc.SimulatedGetConfig;
import org.opendaylight.controller.netconf.test.tool.rpc.SimulatedLock;
import org.opendaylight.controller.netconf.test.tool.rpc.SimulatedUnLock;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaSourceException;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaSourceRepresentation;
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.model.repo.spi.PotentialSchemaSource;
import org.opendaylight.yangtools.yang.model.repo.spi.SchemaSourceListener;
import org.opendaylight.yangtools.yang.model.repo.spi.SchemaSourceProvider;
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

    private final NioEventLoopGroup nettyThreadgroup;
    private final HashedWheelTimer hashedWheelTimer;
    private final List<Channel> devicesChannels = Lists.newArrayList();
    private final List<SshProxyServer> sshWrappers = Lists.newArrayList();
    private final ScheduledExecutorService minaTimerExecutor;
    private final ExecutorService nioExecutor;

    private boolean sendFakeSchema = false;

    public NetconfDeviceSimulator() {
        // TODO make pool size configurable
        this(new NioEventLoopGroup(), new HashedWheelTimer(),
                Executors.newScheduledThreadPool(8, new ThreadFactoryBuilder().setNameFormat("netconf-ssh-server-mina-timers-%d").build()),
                ThreadUtils.newFixedThreadPool("netconf-ssh-server-nio-group", 8));
    }

    private NetconfDeviceSimulator(final NioEventLoopGroup eventExecutors, final HashedWheelTimer hashedWheelTimer, final ScheduledExecutorService minaTimerExecutor, final ExecutorService nioExecutor) {
        this.nettyThreadgroup = eventExecutors;
        this.hashedWheelTimer = hashedWheelTimer;
        this.minaTimerExecutor = minaTimerExecutor;
        this.nioExecutor = nioExecutor;
    }

    private NetconfServerDispatcherImpl createDispatcher(final Map<ModuleBuilder, String> moduleBuilders, final boolean exi, final int generateConfigsTimeout, final Optional<File> notificationsFile) {

        final Set<Capability> capabilities = Sets.newHashSet(Collections2.transform(moduleBuilders.keySet(), new Function<ModuleBuilder, Capability>() {
            @Override
            public Capability apply(final ModuleBuilder input) {
                if (sendFakeSchema) {
                    sendFakeSchema = false;
                    return new FakeModuleBuilderCapability(input, moduleBuilders.get(input));
                } else {
                    return new ModuleBuilderCapability(input, moduleBuilders.get(input));
                }
            }
        }));

        final SessionIdProvider idProvider = new SessionIdProvider();

        final AggregatedNetconfOperationServiceFactory aggregatedNetconfOperationServiceFactory = new AggregatedNetconfOperationServiceFactory();
        final SimulatedOperationProvider simulatedOperationProvider = new SimulatedOperationProvider(idProvider, capabilities, notificationsFile);

        final NetconfMonitoringService monitoringService1 = new DummyMonitoringService(capabilities);

        final NetconfMonitoringActivator.NetconfMonitoringOperationServiceFactory monitoringService =
                new NetconfMonitoringActivator.NetconfMonitoringOperationServiceFactory(
                        new NetconfMonitoringOperationService(monitoringService1));
        aggregatedNetconfOperationServiceFactory.onAddNetconfOperationServiceFactory(simulatedOperationProvider);
        aggregatedNetconfOperationServiceFactory.onAddNetconfOperationServiceFactory(monitoringService);

        final DefaultCommitNotificationProducer commitNotifier = new DefaultCommitNotificationProducer(ManagementFactory.getPlatformMBeanServer());

        final Set<String> serverCapabilities = exi
                ? NetconfServerSessionNegotiatorFactory.DEFAULT_BASE_CAPABILITIES
                : Sets.newHashSet(XmlNetconfConstants.URN_IETF_PARAMS_NETCONF_BASE_1_0, XmlNetconfConstants.URN_IETF_PARAMS_NETCONF_BASE_1_1);

        final NetconfServerSessionNegotiatorFactory serverNegotiatorFactory = new NetconfServerSessionNegotiatorFactory(
                hashedWheelTimer, aggregatedNetconfOperationServiceFactory, idProvider, generateConfigsTimeout, commitNotifier, monitoringService1, serverCapabilities);

        final NetconfServerDispatcherImpl.ServerChannelInitializer serverChannelInitializer = new NetconfServerDispatcherImpl.ServerChannelInitializer(
                serverNegotiatorFactory);
        return new NetconfServerDispatcherImpl(serverChannelInitializer, nettyThreadgroup, nettyThreadgroup);
    }

    private Map<ModuleBuilder, String> toModuleBuilders(final Map<SourceIdentifier, Map.Entry<ASTSchemaSource, YangTextSchemaSource>> sources) {
        final Map<SourceIdentifier, ParserRuleContext> asts = Maps.transformValues(sources, new Function<Map.Entry<ASTSchemaSource, YangTextSchemaSource>, ParserRuleContext>() {
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
        LOG.info("Starting {}, {} simulated devices starting on port {}", params.deviceCount, params.ssh ? "SSH" : "TCP", params.startingPort);

        final Map<ModuleBuilder, String> moduleBuilders = parseSchemasToModuleBuilders(params);

        final NetconfServerDispatcherImpl dispatcher = createDispatcher(moduleBuilders, params.exi, params.generateConfigsTimeout, Optional.fromNullable(params.notificationFile));

        int currentPort = params.startingPort;

        final List<Integer> openDevices = Lists.newArrayList();

        // Generate key to temp folder
        final PEMGeneratorHostKeyProvider keyPairProvider = getPemGeneratorHostKeyProvider();

        for (int i = 0; i < params.deviceCount; i++) {
            if (currentPort > 65535) {
                LOG.warn("Port cannot be greater than 65535, stopping further attempts.");
                break;
            }
            final InetSocketAddress address = getAddress(currentPort);

            final ChannelFuture server;
            if(params.ssh) {
                final InetSocketAddress bindingAddress = InetSocketAddress.createUnresolved("0.0.0.0", currentPort);
                final LocalAddress tcpLocalAddress = new LocalAddress(address.toString());

                server = dispatcher.createLocalServer(tcpLocalAddress);
                try {
                    final SshProxyServer sshServer = new SshProxyServer(minaTimerExecutor, nettyThreadgroup, nioExecutor);
                    sshServer.bind(getSshConfiguration(bindingAddress, tcpLocalAddress, keyPairProvider));
                    sshWrappers.add(sshServer);
                } catch (final BindException e) {
                    LOG.warn("Cannot start simulated device on {}, port already in use. Skipping.", address);
                    // Close local server and continue
                    server.cancel(true);
                    if(server.isDone()) {
                        server.channel().close();
                    }
                    continue;
                } catch (final IOException e) {
                    LOG.warn("Cannot start simulated device on {} due to IOException.", address, e);
                    break;
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
            LOG.info("All simulated devices started successfully from port {} to {}", params.startingPort, currentPort - 1);
        } else if (openDevices.size() == 0) {
            LOG.warn("No simulated devices started.");
        } else {
            LOG.warn("Not all simulated devices started successfully. Started devices ar on ports {}", openDevices);
        }

        return openDevices;
    }

    private SshProxyServerConfiguration getSshConfiguration(final InetSocketAddress bindingAddress, final LocalAddress tcpLocalAddress, final PEMGeneratorHostKeyProvider keyPairProvider) throws IOException {
        return new SshProxyServerConfigurationBuilder()
                .setBindingAddress(bindingAddress)
                .setLocalAddress(tcpLocalAddress)
                .setAuthenticator(new PasswordAuthenticator() {
                    @Override
                    public boolean authenticate(final String username, final String password, final ServerSession session) {
                        return true;
                    }
                })
                .setKeyPairProvider(keyPairProvider)
                .setIdleTimeout(Integer.MAX_VALUE)
                .createSshProxyServerConfiguration();
    }

    private PEMGeneratorHostKeyProvider getPemGeneratorHostKeyProvider() {
        try {
            final Path tempFile = Files.createTempFile("tempKeyNetconfTest", "suffix");
            return new PEMGeneratorHostKeyProvider(tempFile.toAbsolutePath().toString());
        } catch (final IOException e) {
            LOG.error("Unable to generate PEM key", e);
            throw new RuntimeException(e);
        }
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

        if(params.schemasDir != null) {
            final FilesystemSchemaSourceCache<YangTextSchemaSource> cache = new FilesystemSchemaSourceCache<>(consumer, YangTextSchemaSource.class, params.schemasDir);
            consumer.registerSchemaSourceListener(cache);
        }

        addDefaultSchemas(consumer);

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

    private void addDefaultSchemas(final SharedSchemaRepository consumer) {
        SourceIdentifier sId = new SourceIdentifier("ietf-netconf-monitoring", "2010-10-04");
        registerSource(consumer, "/META-INF/yang/ietf-netconf-monitoring.yang", sId);

        sId = new SourceIdentifier("ietf-netconf-monitoring-extension", "2013-12-10");
        registerSource(consumer, "/META-INF/yang/ietf-netconf-monitoring-extension.yang", sId);

        sId = new SourceIdentifier("ietf-yang-types", "2010-09-24");
        registerSource(consumer, "/META-INF/yang/ietf-yang-types.yang", sId);

        sId = new SourceIdentifier("ietf-inet-types", "2010-09-24");
        registerSource(consumer, "/META-INF/yang/ietf-inet-types.yang", sId);
    }

    private void registerSource(final SharedSchemaRepository consumer, final String resource, final SourceIdentifier sourceId) {
        consumer.registerSchemaSource(new SchemaSourceProvider<SchemaSourceRepresentation>() {
            @Override
            public CheckedFuture<? extends SchemaSourceRepresentation, SchemaSourceException> getSource(final SourceIdentifier sourceIdentifier) {
                return Futures.immediateCheckedFuture(new YangTextSchemaSource(sourceId) {
                    @Override
                    protected MoreObjects.ToStringHelper addToStringAttributes(final MoreObjects.ToStringHelper toStringHelper) {
                        return toStringHelper;
                    }

                    @Override
                    public InputStream openStream() throws IOException {
                        return getClass().getResourceAsStream(resource);
                    }
                });
            }
        }, PotentialSchemaSource.create(sourceId, YangTextSchemaSource.class, PotentialSchemaSource.Costs.IMMEDIATE.getValue()));
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
        for (final SshProxyServer sshWrapper : sshWrappers) {
            sshWrapper.close();
        }
        for (final Channel deviceCh : devicesChannels) {
            deviceCh.close();
        }
        nettyThreadgroup.shutdownGracefully();
        minaTimerExecutor.shutdownNow();
        nioExecutor.shutdownNow();
        // close Everything
    }

    private static class SimulatedOperationProvider implements NetconfOperationServiceFactory {
        private final Set<Capability> caps;
        private final SimulatedOperationService simulatedOperationService;


        public SimulatedOperationProvider(final SessionIdProvider idProvider, final Set<Capability> caps, final Optional<File> notificationsFile) {
            this.caps = caps;
            simulatedOperationService = new SimulatedOperationService(idProvider.getCurrentSessionId(), notificationsFile);
        }

        @Override
        public Set<Capability> getCapabilities() {
            return caps;
        }

        @Override
        public AutoCloseable registerCapabilityListener(final CapabilityListener listener) {
            listener.onCapabilitiesAdded(caps);
            return new AutoCloseable() {
                @Override
                public void close() throws Exception {}
            };
        }

        @Override
        public NetconfOperationService createService(final String netconfSessionIdForReporting) {
            return simulatedOperationService;
        }

        static class SimulatedOperationService implements NetconfOperationService {
            private final long currentSessionId;
            private final Optional<File> notificationsFile;

            public SimulatedOperationService(final long currentSessionId, final Optional<File> notificationsFile) {
                this.currentSessionId = currentSessionId;
                this.notificationsFile = notificationsFile;
            }

            @Override
            public Set<NetconfOperation> getNetconfOperations() {
                final DataList storage = new DataList();
                final SimulatedGet sGet = new SimulatedGet(String.valueOf(currentSessionId), storage);
                final SimulatedEditConfig sEditConfig = new SimulatedEditConfig(String.valueOf(currentSessionId), storage);
                final SimulatedGetConfig sGetConfig = new SimulatedGetConfig(String.valueOf(currentSessionId), storage);
                final SimulatedCommit sCommit = new SimulatedCommit(String.valueOf(currentSessionId));
                final SimulatedLock sLock = new SimulatedLock(String.valueOf(currentSessionId));
                final SimulatedUnLock sUnlock = new SimulatedUnLock(String.valueOf(currentSessionId));
                final SimulatedCreateSubscription sCreateSubs = new SimulatedCreateSubscription(String.valueOf(currentSessionId), notificationsFile);
                return Sets.<NetconfOperation>newHashSet(sGet,  sGetConfig, sEditConfig, sCommit, sLock, sUnlock, sCreateSubs);
            }

            @Override
            public void close() {
            }

        }
    }

}
