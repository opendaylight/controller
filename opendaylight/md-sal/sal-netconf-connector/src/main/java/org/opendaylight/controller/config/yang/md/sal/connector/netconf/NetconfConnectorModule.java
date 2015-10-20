/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.md.sal.connector.netconf;

import static org.opendaylight.controller.config.api.JmxAttributeValidationException.checkCondition;
import static org.opendaylight.controller.config.api.JmxAttributeValidationException.checkNotNull;
import com.google.common.base.Optional;
import io.netty.util.concurrent.EventExecutor;
import java.io.File;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.controller.config.threadpool.ScheduledThreadPool;
import org.opendaylight.controller.config.threadpool.ThreadPool;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcher;
import org.opendaylight.controller.netconf.client.conf.NetconfClientConfiguration;
import org.opendaylight.controller.netconf.client.conf.NetconfReconnectingClientConfiguration;
import org.opendaylight.controller.netconf.client.conf.NetconfReconnectingClientConfigurationBuilder;
import org.opendaylight.controller.netconf.nettyutil.handler.ssh.authentication.LoginPassword;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.connect.api.RemoteDeviceHandler;
import org.opendaylight.controller.sal.connect.netconf.NetconfDevice;
import org.opendaylight.controller.sal.connect.netconf.NetconfStateSchemas;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfDeviceCommunicator;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfSessionPreferences;
import org.opendaylight.controller.sal.connect.netconf.sal.KeepaliveSalFacade;
import org.opendaylight.controller.sal.connect.netconf.sal.NetconfDeviceSalFacade;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.protocol.framework.TimedReconnectStrategy;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Host;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaContextFactory;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaRepository;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaSourceFilter;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.model.repo.spi.SchemaSourceRegistry;
import org.opendaylight.yangtools.yang.model.repo.util.FilesystemSchemaSourceCache;
import org.opendaylight.yangtools.yang.parser.repo.SharedSchemaRepository;
import org.opendaylight.yangtools.yang.parser.util.TextToASTTransformer;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public final class NetconfConnectorModule extends org.opendaylight.controller.config.yang.md.sal.connector.netconf.AbstractNetconfConnectorModule
{
    private static final Logger LOG = LoggerFactory.getLogger(NetconfConnectorModule.class);

    private static FilesystemSchemaSourceCache<YangTextSchemaSource> CACHE = null;
    // when no topology is defined in connector config, we need to add a default schema repository to main backwards compatibility
    private static SharedSchemaRepository DEFAULT_SCHEMA_REPOSITORY = null;

    // keep track of already initialized repositories to avoid adding redundant listeners
    private static final Set<SchemaRepository> INITIALIZED_SCHEMA_REPOSITORIES = new HashSet<>();

    private BundleContext bundleContext;
    private Optional<NetconfSessionPreferences> userCapabilities;
    private SchemaSourceRegistry schemaRegistry;
    private SchemaContextFactory schemaContextFactory;

    private Broker domRegistry;
    private NetconfClientDispatcher clientDispatcher;
    private BindingAwareBroker bindingRegistry;
    private ThreadPool processingExecutor;
    private ScheduledThreadPool keepaliveExecutor;
    private SharedSchemaRepository sharedSchemaRepository;
    private EventExecutor eventExecutor;

    public NetconfConnectorModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public NetconfConnectorModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, final NetconfConnectorModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    protected void customValidation() {
        checkNotNull(getAddress(), addressJmxAttribute);
        checkCondition(isHostAddressPresent(getAddress()), "Host address not present in " + getAddress(), addressJmxAttribute);
        checkNotNull(getPort(), portJmxAttribute);
//        checkNotNull(getDomRegistry(), portJmxAttribute);
//        checkNotNull(getDomRegistry(), domRegistryJmxAttribute);

        checkNotNull(getConnectionTimeoutMillis(), connectionTimeoutMillisJmxAttribute);
        checkCondition(getConnectionTimeoutMillis() > 0, "must be > 0", connectionTimeoutMillisJmxAttribute);

        checkNotNull(getConnectionTimeoutMillis(), defaultRequestTimeoutMillisJmxAttribute);
        checkCondition(getConnectionTimeoutMillis() > 0, "must be > 0", defaultRequestTimeoutMillisJmxAttribute);

        checkNotNull(getBetweenAttemptsTimeoutMillis(), betweenAttemptsTimeoutMillisJmxAttribute);
        checkCondition(getBetweenAttemptsTimeoutMillis() > 0, "must be > 0", betweenAttemptsTimeoutMillisJmxAttribute);

//        if (getNetconfTopology() == null) {
//            checkNotNull(getClientDispatcher(), clientDispatcherJmxAttribute);
//            checkNotNull(getBindingRegistry(), bindingRegistryJmxAttribute);
//            checkNotNull(getProcessingExecutor(), processingExecutorJmxAttribute);
//        }

        // Check username + password in case of ssh
        if(getTcpOnly() == false) {
            checkNotNull(getUsername(), usernameJmxAttribute);
            checkNotNull(getPassword(), passwordJmxAttribute);
        }

        userCapabilities = getUserCapabilities();
    }

    private boolean isHostAddressPresent(final Host address) {
        return address.getDomainName() != null ||
               address.getIpAddress() != null && (address.getIpAddress().getIpv4Address() != null || address.getIpAddress().getIpv6Address() != null);
    }

    @Deprecated
    private static ScheduledExecutorService DEFAULT_KEEPALIVE_EXECUTOR;

    @Override
    public java.lang.AutoCloseable createInstance() {
        initDependenciesFromTopology();
        final RemoteDeviceId id = new RemoteDeviceId(getIdentifier(), getSocketAddress());

        final ExecutorService globalProcessingExecutor = getProcessingExecutorDependency().getExecutor();

        RemoteDeviceHandler<NetconfSessionPreferences> salFacade = new NetconfDeviceSalFacade(id, domRegistry,
                bindingRegistry, bundleContext, getDefaultRequestTimeoutMillis());

        final Long keepaliveDelay = getKeepaliveDelay();
        if (shouldSendKeepalive()) {
            // Keepalive executor is optional for now and a default instance is supported
            final ScheduledExecutorService executor = getKeepaliveExecutor() == null ? DEFAULT_KEEPALIVE_EXECUTOR
                    : getKeepaliveExecutorDependency().getExecutor();

            salFacade = new KeepaliveSalFacade(id, salFacade, executor, keepaliveDelay);
        }

        final NetconfDevice.SchemaResourcesDTO schemaResourcesDTO = new NetconfDevice.SchemaResourcesDTO(
                schemaRegistry, schemaContextFactory, new NetconfStateSchemas.NetconfStateSchemasResolverImpl());

        final NetconfDevice device = new NetconfDevice(schemaResourcesDTO, id, salFacade, globalProcessingExecutor,
                getReconnectOnChangedSchema());

        final NetconfDeviceCommunicator listener = userCapabilities.isPresent() ? new NetconfDeviceCommunicator(id,
                device, userCapabilities.get()) : new NetconfDeviceCommunicator(id, device);

        if (shouldSendKeepalive()) {
            ((KeepaliveSalFacade) salFacade).setListener(listener);
        }

        final NetconfReconnectingClientConfiguration clientConfig = getClientConfig(listener);
        final NetconfClientDispatcher dispatcher = getClientDispatcherDependency();

        listener.initializeRemoteConnection(dispatcher, clientConfig);

        return new SalConnectorCloseable(listener, salFacade);
    }

    private void initDependenciesFromTopology() {
//      topology = getNetconfTopologyDependency();

//      domRegistry = topology.getDomRegistryDependency();
//      clientDispatcher = topology.getNetconfClientDispatcherDependency();
//      bindingRegistry = topology.getBindingAwareBroker();
//      processingExecutor = topology.getProcessingExecutorDependency();
//      keepaliveExecutor = topology.getKeepaliveExecutorDependency();
//      sharedSchemaRepository = topology.getSharedSchemaRepository().getSharedSchemaRepository();
//      eventExecutor = topology.getEventExecutorDependency();

//      initFilesystemSchemaSourceCache(sharedSchemaRepository);
        domRegistry = getDomRegistryDependency();
        clientDispatcher = getClientDispatcherDependency();
        bindingRegistry = getBindingRegistryDependency();
        processingExecutor = getProcessingExecutorDependency();
        eventExecutor = getEventExecutorDependency();

        if (getKeepaliveExecutor() == null) {
            LOG.warn("Keepalive executor missing. Using default instance for now, the configuration needs to be updated");

            // Instantiate the default executor, now we know its necessary
            if (DEFAULT_KEEPALIVE_EXECUTOR == null) {
                DEFAULT_KEEPALIVE_EXECUTOR = Executors.newScheduledThreadPool(2, new ThreadFactory() {
                    @Override
                    public Thread newThread(final Runnable r) {
                        final Thread thread = new Thread(r);
                        thread.setName("netconf-southound-keepalives-" + thread.getId());
                        thread.setDaemon(true);
                        return thread;
                    }
                });
            }
        }

        LOG.warn("No topology defined in config, using default-shared-schema-repo");
        if (DEFAULT_SCHEMA_REPOSITORY == null) {
            DEFAULT_SCHEMA_REPOSITORY = new SharedSchemaRepository("default shared schema repo");
        }
        initFilesystemSchemaSourceCache(DEFAULT_SCHEMA_REPOSITORY);
    }

    private void initFilesystemSchemaSourceCache(final SharedSchemaRepository repository) {
        LOG.warn("Schema repository used: {}", repository.getIdentifier());
        if (CACHE == null) {
            CACHE = new FilesystemSchemaSourceCache<>(repository, YangTextSchemaSource.class, new File("cache/schema"));
        }
        if (!INITIALIZED_SCHEMA_REPOSITORIES.contains(repository)) {
            repository.registerSchemaSourceListener(CACHE);
            repository.registerSchemaSourceListener(TextToASTTransformer.create(repository, repository));
            INITIALIZED_SCHEMA_REPOSITORIES.add(repository);
        }
        setSchemaRegistry(repository);
        setSchemaContextFactory(repository.createSchemaContextFactory(SchemaSourceFilter.ALWAYS_ACCEPT));
    }

    private boolean shouldSendKeepalive() {
        return getKeepaliveDelay() > 0;
    }

    private Optional<NetconfSessionPreferences> getUserCapabilities() {
        if(getYangModuleCapabilities() == null) {
            return Optional.absent();
        }

        final List<String> capabilities = getYangModuleCapabilities().getCapability();
        if(capabilities == null || capabilities.isEmpty()) {
            return Optional.absent();
        }

        final NetconfSessionPreferences parsedOverrideCapabilities = NetconfSessionPreferences.fromStrings(capabilities);
        JmxAttributeValidationException.checkCondition(
                parsedOverrideCapabilities.getNonModuleCaps().isEmpty(),
                "Capabilities to override can only contain module based capabilities, non-module capabilities will be retrieved from the device," +
                        " configured non-module capabilities: " + parsedOverrideCapabilities.getNonModuleCaps(),
                yangModuleCapabilitiesJmxAttribute);

        return Optional.of(parsedOverrideCapabilities);
    }

    public void setBundleContext(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public NetconfReconnectingClientConfiguration getClientConfig(final NetconfDeviceCommunicator listener) {
        final InetSocketAddress socketAddress = getSocketAddress();
        final long clientConnectionTimeoutMillis = getConnectionTimeoutMillis();

        final ReconnectStrategyFactory sf = new TimedReconnectStrategyFactory(eventExecutor,
                getMaxConnectionAttempts(), getBetweenAttemptsTimeoutMillis(), getSleepFactor());
        final ReconnectStrategy strategy = sf.createReconnectStrategy();

        return NetconfReconnectingClientConfigurationBuilder.create()
        .withAddress(socketAddress)
        .withConnectionTimeoutMillis(clientConnectionTimeoutMillis)
        .withReconnectStrategy(strategy)
        .withAuthHandler(new LoginPassword(getUsername(), getPassword()))
        .withProtocol(getTcpOnly() ?
                NetconfClientConfiguration.NetconfClientProtocol.TCP :
                NetconfClientConfiguration.NetconfClientProtocol.SSH)
        .withConnectStrategyFactory(sf)
        .withSessionListener(listener)
        .build();
    }

    private static final class SalConnectorCloseable implements AutoCloseable {
        private final RemoteDeviceHandler<NetconfSessionPreferences> salFacade;
        private final NetconfDeviceCommunicator listener;

        public SalConnectorCloseable(final NetconfDeviceCommunicator listener,
                                     final RemoteDeviceHandler<NetconfSessionPreferences> salFacade) {
            this.listener = listener;
            this.salFacade = salFacade;
        }

        @Override
        public void close() {
            listener.close();
            salFacade.close();
        }
    }

    private static final class TimedReconnectStrategyFactory implements ReconnectStrategyFactory {
        private final Long connectionAttempts;
        private final EventExecutor executor;
        private final double sleepFactor;
        private final int minSleep;

        TimedReconnectStrategyFactory(final EventExecutor executor, final Long maxConnectionAttempts, final int minSleep, final BigDecimal sleepFactor) {
            if (maxConnectionAttempts != null && maxConnectionAttempts > 0) {
                connectionAttempts = maxConnectionAttempts;
            } else {
                LOG.trace("Setting {} on {} to infinity", maxConnectionAttemptsJmxAttribute, this);
                connectionAttempts = null;
            }

            this.sleepFactor = sleepFactor.doubleValue();
            this.executor = executor;
            this.minSleep = minSleep;
        }

        @Override
        public ReconnectStrategy createReconnectStrategy() {
            final Long maxSleep = null;
            final Long deadline = null;

            return new TimedReconnectStrategy(executor, minSleep,
                    minSleep, sleepFactor, maxSleep, connectionAttempts, deadline);
        }
    }

    private InetSocketAddress getSocketAddress() {
        if(getAddress().getDomainName() != null) {
            return new InetSocketAddress(getAddress().getDomainName().getValue(), getPort().getValue());
        } else {
            final IpAddress ipAddress = getAddress().getIpAddress();
            final String ip = ipAddress.getIpv4Address() != null ? ipAddress.getIpv4Address().getValue() : ipAddress.getIpv6Address().getValue();
            return new InetSocketAddress(ip, getPort().getValue());
        }
    }

    public void setSchemaRegistry(final SchemaSourceRegistry schemaRegistry) {
        this.schemaRegistry = schemaRegistry;
    }

    public void setSchemaContextFactory(final SchemaContextFactory schemaContextFactory) {
        this.schemaContextFactory = schemaContextFactory;
    }
}
