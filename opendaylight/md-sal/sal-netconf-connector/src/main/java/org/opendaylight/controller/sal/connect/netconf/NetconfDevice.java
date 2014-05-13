/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf;

import static com.google.common.base.Preconditions.checkState;
import static org.opendaylight.controller.sal.connect.netconf.InventoryUtils.INVENTORY_CONNECTED;
import static org.opendaylight.controller.sal.connect.netconf.InventoryUtils.INVENTORY_ID;
import static org.opendaylight.controller.sal.connect.netconf.InventoryUtils.INVENTORY_NODE;
import static org.opendaylight.controller.sal.connect.netconf.InventoryUtils.INVENTORY_PATH;
import static org.opendaylight.controller.sal.connect.netconf.InventoryUtils.NETCONF_INVENTORY_INITIAL_CAPABILITY;
import static org.opendaylight.controller.sal.connect.netconf.NetconfMapping.CONFIG_SOURCE_RUNNING;
import static org.opendaylight.controller.sal.connect.netconf.NetconfMapping.NETCONF_DATA_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.NetconfMapping.NETCONF_GET_CONFIG_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.NetconfMapping.NETCONF_GET_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.NetconfMapping.toFilterStructure;
import static org.opendaylight.controller.sal.connect.netconf.NetconfMapping.toRpcMessage;
import static org.opendaylight.controller.sal.connect.netconf.NetconfMapping.wrap;

import com.google.common.base.Preconditions;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.controller.md.sal.common.api.data.DataReader;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcher;
import org.opendaylight.controller.netconf.client.conf.NetconfClientConfiguration;
import org.opendaylight.controller.netconf.client.conf.NetconfReconnectingClientConfiguration;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.Broker.RpcRegistration;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.controller.sal.core.api.data.DataBrokerService;
import org.opendaylight.controller.sal.core.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionInstance;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionService;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.inventory.rev140108.NetconfNode;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.data.impl.CompositeNodeTOImpl;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.data.impl.SimpleNodeTOImpl;
import org.opendaylight.yangtools.yang.data.impl.util.CompositeNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.util.repo.AbstractCachingSchemaSourceProvider;
import org.opendaylight.yangtools.yang.model.util.repo.SchemaSourceProvider;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;
import org.opendaylight.yangtools.yang.parser.impl.util.YangSourceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.ListenableFuture;
import io.netty.util.concurrent.EventExecutor;

public class NetconfDevice implements Provider, //
        DataReader<InstanceIdentifier, CompositeNode>, //
        DataCommitHandler<InstanceIdentifier, CompositeNode>, //
        RpcImplementation, //
        AutoCloseable {

    InetSocketAddress socketAddress;

    MountProvisionInstance mountInstance;

    EventExecutor eventExecutor;

    ExecutorService processingExecutor;

    InstanceIdentifier path;

    ReconnectStrategy reconnectStrategy;

    AbstractCachingSchemaSourceProvider<String, InputStream> schemaSourceProvider;

    private NetconfDeviceSchemaContextProvider deviceContextProvider;

    protected Logger logger;

    Registration<DataReader<InstanceIdentifier, CompositeNode>> operReaderReg;
    Registration<DataReader<InstanceIdentifier, CompositeNode>> confReaderReg;
    Registration<DataCommitHandler<InstanceIdentifier, CompositeNode>> commitHandlerReg;
    List<RpcRegistration> rpcReg;

    String name;

    MountProvisionService mountService;

    NetconfClientDispatcher dispatcher;

    static InstanceIdentifier ROOT_PATH = InstanceIdentifier.builder().toInstance();

    SchemaSourceProvider<InputStream> remoteSourceProvider;

    private volatile DataBrokerService dataBroker;

    NetconfDeviceListener listener;

    private boolean rollbackSupported;

    private NetconfReconnectingClientConfiguration clientConfig;
    private volatile DataProviderService dataProviderService;

    public NetconfDevice(String name) {
        this.name = name;
        this.logger = LoggerFactory.getLogger(NetconfDevice.class + "#" + name);
        this.path = InstanceIdentifier.builder(INVENTORY_PATH)
                .nodeWithKey(INVENTORY_NODE, Collections.<QName, Object>singletonMap(INVENTORY_ID, name)).toInstance();
    }

    public void start() {
        checkState(dispatcher != null, "Dispatcher must be set.");
        checkState(schemaSourceProvider != null, "Schema Source Provider must be set.");
        checkState(eventExecutor != null, "Event executor must be set.");

        Preconditions.checkArgument(clientConfig.getSessionListener() instanceof NetconfDeviceListener);
        listener = (NetconfDeviceListener) clientConfig.getSessionListener();

        logger.info("Starting NETCONF Client {} for address {}", name, socketAddress);

        dispatcher.createReconnectingClient(clientConfig);
    }

    Optional<SchemaContext> getSchemaContext() {
        if (deviceContextProvider == null) {
            return Optional.absent();
        }
        return deviceContextProvider.currentContext;
    }

    void bringDown() {
        if (rpcReg != null) {
            for (RpcRegistration reg : rpcReg) {
                reg.close();
            }
            rpcReg = null;
        }
        closeGracefully(confReaderReg);
        confReaderReg = null;
        closeGracefully(operReaderReg);
        operReaderReg = null;
        closeGracefully(commitHandlerReg);
        commitHandlerReg = null;

        updateDeviceState(false, Collections.<QName> emptySet());
    }

    private void closeGracefully(final AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception e) {
                logger.warn("Ignoring exception while closing {}", resource, e);
            }
        }
    }

    void bringUp(final SchemaSourceProvider<String> delegate, final Set<QName> capabilities, final boolean rollbackSupported) {
        // This has to be called from separate thread, not from netty thread calling onSessionUp in DeviceListener.
        // Reason: delegate.getSchema blocks thread when waiting for response
        // however, if the netty thread is blocked, no incoming message can be processed
        // ... netty should pick another thread from pool to process incoming message, but it does not http://netty.io/wiki/thread-model.html
        // TODO redesign +refactor
        processingExecutor.submit(new Runnable() {
            @Override
            public void run() {
                NetconfDevice.this.rollbackSupported = rollbackSupported;
                remoteSourceProvider = schemaSourceProvider.createInstanceFor(delegate);
                deviceContextProvider = new NetconfDeviceSchemaContextProvider(NetconfDevice.this, remoteSourceProvider);
                deviceContextProvider.createContextFromCapabilities(capabilities);
                if (mountInstance != null && getSchemaContext().isPresent()) {
                    mountInstance.setSchemaContext(getSchemaContext().get());
                }

                updateDeviceState(true, capabilities);

                if (mountInstance != null) {
                    confReaderReg = mountInstance.registerConfigurationReader(ROOT_PATH, NetconfDevice.this);
                    operReaderReg = mountInstance.registerOperationalReader(ROOT_PATH, NetconfDevice.this);
                    commitHandlerReg = mountInstance.registerCommitHandler(ROOT_PATH, NetconfDevice.this);

                    List<RpcRegistration> rpcs = new ArrayList<>();
                    // TODO same condition twice
                    if (mountInstance != null && getSchemaContext().isPresent()) {
                        for (RpcDefinition rpc : mountInstance.getSchemaContext().getOperations()) {
                            rpcs.add(mountInstance.addRpcImplementation(rpc.getQName(), NetconfDevice.this));
                        }
                    }
                    rpcReg = rpcs;
                }
            }
        });
    }

    private void updateDeviceState(boolean up, Set<QName> capabilities) {
        checkDataStoreState();

        DataModificationTransaction transaction = dataBroker.beginTransaction();

        CompositeNodeBuilder<ImmutableCompositeNode> it = ImmutableCompositeNode.builder();
        it.setQName(INVENTORY_NODE);
        it.addLeaf(INVENTORY_ID, name);
        it.addLeaf(INVENTORY_CONNECTED, up);

        logger.debug("Client capabilities {}", capabilities);
        for (QName capability : capabilities) {
            it.addLeaf(NETCONF_INVENTORY_INITIAL_CAPABILITY, capability.toString());
        }

        logger.debug("Update device state transaction " + transaction.getIdentifier()
                + " putting operational data started.");
        transaction.removeOperationalData(path);
        transaction.putOperationalData(path, it.toInstance());
        logger.debug("Update device state transaction " + transaction.getIdentifier()
                + " putting operational data ended.");

        // FIXME: this has to be asynchronous
        RpcResult<TransactionStatus> transactionStatus = null;
        try {
            transactionStatus = transaction.commit().get();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while waiting for response", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Read configuration data " + path + " failed", e);
        }
        // TODO better ex handling

        if (transactionStatus.isSuccessful()) {
            logger.debug("Update device state transaction " + transaction.getIdentifier() + " SUCCESSFUL.");
        } else {
            logger.debug("Update device state transaction " + transaction.getIdentifier() + " FAILED!");
            logger.debug("Update device state transaction status " + transaction.getStatus());
        }
    }

    @Override
    public CompositeNode readConfigurationData(InstanceIdentifier path) {
        RpcResult<CompositeNode> result = null;
        try {
            result = this.invokeRpc(NETCONF_GET_CONFIG_QNAME,
                    wrap(NETCONF_GET_CONFIG_QNAME, CONFIG_SOURCE_RUNNING, toFilterStructure(path))).get();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while waiting for response", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Read configuration data " + path + " failed", e);
        }

        CompositeNode data = result.getResult().getFirstCompositeByName(NETCONF_DATA_QNAME);
        return data == null ? null : (CompositeNode) findNode(data, path);
    }

    @Override
    public CompositeNode readOperationalData(InstanceIdentifier path) {
        RpcResult<CompositeNode> result = null;
        try {
            result = invokeRpc(NETCONF_GET_QNAME, wrap(NETCONF_GET_QNAME, toFilterStructure(path))).get();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while waiting for response", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Read configuration data " + path + " failed", e);
        }

        CompositeNode data = result.getResult().getFirstCompositeByName(NETCONF_DATA_QNAME);
        return (CompositeNode) findNode(data, path);
    }

    @Override
    public Set<QName> getSupportedRpcs() {
        return Collections.emptySet();
    }

    @Override
    public ListenableFuture<RpcResult<CompositeNode>> invokeRpc(QName rpc, CompositeNode input) {
        return listener.sendRequest(toRpcMessage(rpc, input, getSchemaContext()), rpc);
    }

    @Override
    public Collection<ProviderFunctionality> getProviderFunctionality() {
        return Collections.emptySet();
    }

    @Override
    public void onSessionInitiated(ProviderSession session) {
        dataBroker = session.getService(DataBrokerService.class);

        processingExecutor.submit(new Runnable() {
            @Override
            public void run() {
                updateInitialState();
            }
        });

        mountService = session.getService(MountProvisionService.class);
        if (mountService != null) {
            mountInstance = mountService.createOrGetMountPoint(path);
        }
    }

    private void updateInitialState() {
        checkDataStoreState();

        DataModificationTransaction transaction = dataBroker.beginTransaction();
        if (operationalNodeNotExisting(transaction)) {
            transaction.putOperationalData(path, getNodeWithId());
        }
        if (configurationNodeNotExisting(transaction)) {
            transaction.putConfigurationData(path, getNodeWithId());
        }

        try {
            transaction.commit().get();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while waiting for response", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Read configuration data " + path + " failed", e);
        }
    }

    private void checkDataStoreState() {
        // read data from Nodes/Node in order to wait with write until schema for Nodes/Node is present in datastore
        dataProviderService.readOperationalData(org.opendaylight.yangtools.yang.binding.InstanceIdentifier.builder(
                Nodes.class).child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class).augmentation(NetconfNode.class).build());    }

    CompositeNode getNodeWithId() {
        SimpleNodeTOImpl id = new SimpleNodeTOImpl(INVENTORY_ID, null, name);
        return new CompositeNodeTOImpl(INVENTORY_NODE, null, Collections.<Node<?>> singletonList(id));
    }

    boolean configurationNodeNotExisting(DataModificationTransaction transaction) {
        return null == transaction.readConfigurationData(path);
    }

    boolean operationalNodeNotExisting(DataModificationTransaction transaction) {
        return null == transaction.readOperationalData(path);
    }

    static Node<?> findNode(CompositeNode node, InstanceIdentifier identifier) {

        Node<?> current = node;
        for (InstanceIdentifier.PathArgument arg : identifier.getPath()) {
            if (current instanceof SimpleNode<?>) {
                return null;
            } else if (current instanceof CompositeNode) {
                CompositeNode currentComposite = (CompositeNode) current;

                current = currentComposite.getFirstCompositeByName(arg.getNodeType());
                if (current == null) {
                    current = currentComposite.getFirstCompositeByName(arg.getNodeType().withoutRevision());
                }
                if (current == null) {
                    current = currentComposite.getFirstSimpleByName(arg.getNodeType());
                }
                if (current == null) {
                    current = currentComposite.getFirstSimpleByName(arg.getNodeType().withoutRevision());
                }
                if (current == null) {
                    return null;
                }
            }
        }
        return current;
    }

    @Override
    public DataCommitTransaction<InstanceIdentifier, CompositeNode> requestCommit(
            DataModification<InstanceIdentifier, CompositeNode> modification) {
        NetconfDeviceTwoPhaseCommitTransaction twoPhaseCommit = new NetconfDeviceTwoPhaseCommitTransaction(this,
                modification, true, rollbackSupported);
        try {
            twoPhaseCommit.prepare();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while waiting for response", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Read configuration data " + path + " failed", e);
        }
         return twoPhaseCommit;
    }

    Set<QName> getCapabilities(Collection<String> capabilities) {
        return FluentIterable.from(capabilities).filter(new Predicate<String>() {
            @Override
            public boolean apply(final String capability) {
                return capability.contains("?") && capability.contains("module=") && capability.contains("revision=");
            }
        }).transform(new Function<String, QName>() {
            @Override
            public QName apply(final String capability) {
                String[] parts = capability.split("\\?");
                String namespace = parts[0];
                FluentIterable<String> queryParams = FluentIterable.from(Arrays.asList(parts[1].split("&")));

                String revision = getStringAndTransform(queryParams, "revision=", "revision=");

                String moduleName = getStringAndTransform(queryParams, "module=", "module=");

                if (revision == null) {
                    logger.warn("Netconf device was not reporting revision correctly, trying to get amp;revision=");
                    revision = getStringAndTransform(queryParams, "amp;revision==", "revision=");

                    if (revision != null) {
                        logger.warn("Netconf device returned revision incorectly escaped for {}", capability);
                    }
                }
                if (revision == null) {
                    return QName.create(URI.create(namespace), null, moduleName);
                }
                return QName.create(namespace, revision, moduleName);
            }

            private String getStringAndTransform(final Iterable<String> queryParams, final String match,
                    final String substringToRemove) {
                Optional<String> found = Iterables.tryFind(queryParams, new Predicate<String>() {
                    @Override
                    public boolean apply(final String input) {
                        return input.startsWith(match);
                    }
                });

                return found.isPresent() ? found.get().replaceAll(substringToRemove, "") : null;
            }

        }).toSet();
    }

    @Override
    public void close() {
        bringDown();
    }

    public String getName() {
        return name;
    }

    public InetSocketAddress getSocketAddress() {
        return socketAddress;
    }

    public MountProvisionInstance getMountInstance() {
        return mountInstance;
    }

    public void setReconnectStrategy(final ReconnectStrategy reconnectStrategy) {
        this.reconnectStrategy = reconnectStrategy;
    }

    public void setProcessingExecutor(final ExecutorService processingExecutor) {
        this.processingExecutor = processingExecutor;
    }

    public void setSocketAddress(final InetSocketAddress socketAddress) {
        this.socketAddress = socketAddress;
    }

    public void setEventExecutor(final EventExecutor eventExecutor) {
        this.eventExecutor = eventExecutor;
    }

    public void setSchemaSourceProvider(final AbstractCachingSchemaSourceProvider<String, InputStream> schemaSourceProvider) {
        this.schemaSourceProvider = schemaSourceProvider;
    }

    public void setDispatcher(final NetconfClientDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public void setClientConfig(final NetconfReconnectingClientConfiguration clientConfig) {
        this.clientConfig = clientConfig;
    }

    public void setDataProviderService(final DataProviderService dataProviderService) {
        this.dataProviderService = dataProviderService;
    }
}

class NetconfDeviceSchemaContextProvider {

    NetconfDevice device;

    SchemaSourceProvider<InputStream> sourceProvider;

    Optional<SchemaContext> currentContext;

    NetconfDeviceSchemaContextProvider(NetconfDevice device, SchemaSourceProvider<InputStream> sourceProvider) {
        this.device = device;
        this.sourceProvider = sourceProvider;
        this.currentContext = Optional.absent();
    }

    void createContextFromCapabilities(Iterable<QName> capabilities) {
        YangSourceContext sourceContext = YangSourceContext.createFrom(capabilities, sourceProvider);
        if (!sourceContext.getMissingSources().isEmpty()) {
            device.logger.warn("Sources for following models are missing {}", sourceContext.getMissingSources());
        }
        device.logger.debug("Trying to create schema context from {}", sourceContext.getValidSources());
        List<InputStream> modelsToParse = YangSourceContext.getValidInputStreams(sourceContext);
        if (!sourceContext.getValidSources().isEmpty()) {
            SchemaContext schemaContext = tryToCreateContext(modelsToParse);
            currentContext = Optional.fromNullable(schemaContext);
        } else {
            currentContext = Optional.absent();
        }
        if (currentContext.isPresent()) {
            device.logger.debug("Schema context successfully created.");
        }
    }

    SchemaContext tryToCreateContext(List<InputStream> modelsToParse) {
        YangParserImpl parser = new YangParserImpl();
        try {

            Set<Module> models = parser.parseYangModelsFromStreams(modelsToParse);
            return parser.resolveSchemaContext(models);
        } catch (Exception e) {
            device.logger.debug("Error occured during parsing YANG schemas", e);
            return null;
        }
    }
}
