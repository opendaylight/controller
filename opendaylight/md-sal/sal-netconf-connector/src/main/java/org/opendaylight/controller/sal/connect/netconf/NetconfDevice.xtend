/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf

import com.google.common.base.Optional
import com.google.common.collect.FluentIterable
import io.netty.util.concurrent.EventExecutor
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.URI
import java.util.Collections
import java.util.List
import java.util.Set
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler
import org.opendaylight.controller.md.sal.common.api.data.DataModification
import org.opendaylight.controller.md.sal.common.api.data.DataReader
import org.opendaylight.controller.netconf.api.NetconfMessage
import org.opendaylight.controller.netconf.client.NetconfClient
import org.opendaylight.controller.netconf.client.NetconfClientDispatcher
import org.opendaylight.controller.netconf.util.xml.XmlUtil
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession
import org.opendaylight.controller.sal.core.api.Provider
import org.opendaylight.controller.sal.core.api.RpcImplementation
import org.opendaylight.controller.sal.core.api.data.DataBrokerService
import org.opendaylight.controller.sal.core.api.data.DataModificationTransaction
import org.opendaylight.controller.sal.core.api.mount.MountProvisionInstance
import org.opendaylight.controller.sal.core.api.mount.MountProvisionService
import org.opendaylight.protocol.framework.ReconnectStrategy
import org.opendaylight.yangtools.concepts.Registration
import org.opendaylight.yangtools.yang.common.QName
import org.opendaylight.yangtools.yang.data.api.CompositeNode
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier
import org.opendaylight.yangtools.yang.data.api.Node
import org.opendaylight.yangtools.yang.data.api.SimpleNode
import org.opendaylight.yangtools.yang.data.impl.CompositeNodeTOImpl
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode
import org.opendaylight.yangtools.yang.data.impl.SimpleNodeTOImpl
import org.opendaylight.yangtools.yang.model.api.SchemaContext
import org.opendaylight.yangtools.yang.model.util.repo.AbstractCachingSchemaSourceProvider
import org.opendaylight.yangtools.yang.model.util.repo.SchemaSourceProvider
import org.opendaylight.yangtools.yang.model.util.repo.SchemaSourceProviders
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl
import org.opendaylight.yangtools.yang.parser.impl.util.YangSourceContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static com.google.common.base.Preconditions.*
import static org.opendaylight.controller.sal.connect.netconf.InventoryUtils.*

import static extension org.opendaylight.controller.sal.connect.netconf.NetconfMapping.*

class NetconfDevice implements Provider, // 
DataReader<InstanceIdentifier, CompositeNode>, //
DataCommitHandler<InstanceIdentifier, CompositeNode>, //
RpcImplementation, //
AutoCloseable {

    var NetconfClient client;

    @Property
    var InetSocketAddress socketAddress;

    @Property
    var MountProvisionInstance mountInstance;

    @Property
    var EventExecutor eventExecutor;

    @Property
    var ExecutorService processingExecutor;

    @Property
    var InstanceIdentifier path;

    @Property
    var ReconnectStrategy reconnectStrategy;

    @Property
    var AbstractCachingSchemaSourceProvider<String, InputStream> schemaSourceProvider;

    @Property
    private NetconfDeviceSchemaContextProvider deviceContextProvider

    protected val Logger logger

    Registration<DataReader<InstanceIdentifier, CompositeNode>> operReaderReg
    Registration<DataReader<InstanceIdentifier, CompositeNode>> confReaderReg
    Registration<DataCommitHandler<InstanceIdentifier, CompositeNode>> commitHandlerReg

    val String name
    MountProvisionService mountService

    int messegeRetryCount = 5;

    int messageTimeoutCount = 5 * 1000;

    Set<QName> cachedCapabilities

    @Property
    var NetconfClientDispatcher dispatcher

    static val InstanceIdentifier ROOT_PATH = InstanceIdentifier.builder().toInstance();

    @Property
    var SchemaSourceProvider<InputStream> remoteSourceProvider
    
    DataBrokerService dataBroker

    public new(String name) {
        this.name = name;
        this.logger = LoggerFactory.getLogger(NetconfDevice.name + "#" + name);
        this.path = InstanceIdentifier.builder(INVENTORY_PATH).nodeWithKey(INVENTORY_NODE,
            Collections.singletonMap(INVENTORY_ID, name)).toInstance;
    }

    def start() {
        checkState(dispatcher != null, "Dispatcher must be set.");
        checkState(schemaSourceProvider != null, "Schema Source Provider must be set.")
        checkState(eventExecutor != null, "Event executor must be set.");

        val listener = new NetconfDeviceListener(this);
        val task = startClientTask(dispatcher, listener)
        return processingExecutor.submit(task) as Future<Void>;

    }

    def Optional<SchemaContext> getSchemaContext() {
        if (deviceContextProvider == null) {
            return Optional.absent();
        }
        return deviceContextProvider.currentContext;
    }

    private def Runnable startClientTask(NetconfClientDispatcher dispatcher, NetconfDeviceListener listener) {
        return [ |
            try {
                logger.info("Starting Netconf Client on: {}", socketAddress);
                client = NetconfClient.clientFor(name, socketAddress, reconnectStrategy, dispatcher, listener);
                logger.debug("Initial capabilities {}", initialCapabilities);
                var SchemaSourceProvider<String> delegate;
                if (NetconfRemoteSchemaSourceProvider.isSupportedFor(initialCapabilities)) {
                    delegate = new NetconfRemoteSchemaSourceProvider(this);
                }  else if(client.capabilities.contains(NetconfRemoteSchemaSourceProvider.IETF_NETCONF_MONITORING.namespace.toString)) {
                    delegate = new NetconfRemoteSchemaSourceProvider(this);
                } else {
                    logger.info("Netconf server {} does not support IETF Netconf Monitoring", socketAddress);
                    delegate = SchemaSourceProviders.<String>noopProvider();
                }
                remoteSourceProvider = schemaSourceProvider.createInstanceFor(delegate);
                deviceContextProvider = new NetconfDeviceSchemaContextProvider(this, remoteSourceProvider);
                deviceContextProvider.createContextFromCapabilities(initialCapabilities);
                if (mountInstance != null && schemaContext.isPresent) {
                    mountInstance.schemaContext = schemaContext.get();
                    val operations = schemaContext.get().operations;
                    for (rpc : operations) {
                        mountInstance.addRpcImplementation(rpc.QName, this);
                    }
                }
                updateDeviceState()
                if (mountInstance != null && confReaderReg == null && operReaderReg == null) {
                    confReaderReg = mountInstance.registerConfigurationReader(ROOT_PATH, this);
                    operReaderReg = mountInstance.registerOperationalReader(ROOT_PATH, this);
                    commitHandlerReg = mountInstance.registerCommitHandler(ROOT_PATH, this);
                }
            } catch (Exception e) {
                logger.error("Netconf client NOT started. ", e)
            }
        ]
    }

    private def updateDeviceState() {
        val transaction = dataBroker.beginTransaction

        val it = ImmutableCompositeNode.builder
        setQName(INVENTORY_NODE)
        addLeaf(INVENTORY_ID, name)
        addLeaf(INVENTORY_CONNECTED, client.clientSession.up)

        logger.debug("Client capabilities {}", client.capabilities)
        for (capability : client.capabilities) {
            addLeaf(NETCONF_INVENTORY_INITIAL_CAPABILITY, capability)
        }

        logger.debug("Update device state transaction " + transaction.identifier + " putting operational data started.")
        transaction.putOperationalData(path, it.toInstance)
        logger.debug("Update device state transaction " + transaction.identifier + " putting operational data ended.")
        val transactionStatus = transaction.commit.get;

        if (transactionStatus.successful) {
            logger.debug("Update device state transaction " + transaction.identifier + " SUCCESSFUL.")
        } else {
            logger.debug("Update device state transaction " + transaction.identifier + " FAILED!")
            logger.debug("Update device state transaction status " + transaction.status)
        }
    }

    override readConfigurationData(InstanceIdentifier path) {
        val result = invokeRpc(NETCONF_GET_CONFIG_QNAME,
            wrap(NETCONF_GET_CONFIG_QNAME, CONFIG_SOURCE_RUNNING, path.toFilterStructure()));
        val data = result.result.getFirstCompositeByName(NETCONF_DATA_QNAME);
        return data?.findNode(path) as CompositeNode;
    }

    override readOperationalData(InstanceIdentifier path) {
        val result = invokeRpc(NETCONF_GET_QNAME, wrap(NETCONF_GET_QNAME, path.toFilterStructure()));
        val data = result.result.getFirstCompositeByName(NETCONF_DATA_QNAME);
        return data?.findNode(path) as CompositeNode;
    }

    override getSupportedRpcs() {
        Collections.emptySet;
    }

    def createSubscription(String streamName) {
        val it = ImmutableCompositeNode.builder()
        QName = NETCONF_CREATE_SUBSCRIPTION_QNAME
        addLeaf("stream", streamName);
        invokeRpc(QName, toInstance())
    }

    override invokeRpc(QName rpc, CompositeNode input) {
        try {
            val message = rpc.toRpcMessage(input,schemaContext);
            val result = sendMessageImpl(message, messegeRetryCount, messageTimeoutCount);
            return result.toRpcResult(rpc, schemaContext);

        } catch (Exception e) {
            logger.error("Rpc was not processed correctly.", e)
            throw e;
        }
    }

    def NetconfMessage sendMessageImpl(NetconfMessage message, int retryCount, int timeout) {
        logger.debug("Send message {}",XmlUtil.toString(message.document))
        val result = client.sendMessage(message, retryCount, timeout);
        NetconfMapping.checkValidReply(message, result)
        return result;
    }

    override getProviderFunctionality() {
        Collections.emptySet
    }

    override onSessionInitiated(ProviderSession session) {
        dataBroker = session.getService(DataBrokerService);

        val transaction = dataBroker.beginTransaction
        if (transaction.operationalNodeNotExisting) {
            transaction.putOperationalData(path, nodeWithId)
        }
        if (transaction.configurationNodeNotExisting) {
            transaction.putConfigurationData(path, nodeWithId)
        }
        transaction.commit().get();
        mountService = session.getService(MountProvisionService);
        mountInstance = mountService?.createOrGetMountPoint(path);
    }

    def getNodeWithId() {
        val id = new SimpleNodeTOImpl(INVENTORY_ID, null, name);
        return new CompositeNodeTOImpl(INVENTORY_NODE, null, Collections.singletonList(id));
    }

    def boolean configurationNodeNotExisting(DataModificationTransaction transaction) {
        return null === transaction.readConfigurationData(path);
    }

    def boolean operationalNodeNotExisting(DataModificationTransaction transaction) {
        return null === transaction.readOperationalData(path);
    }

    static def Node<?> findNode(CompositeNode node, InstanceIdentifier identifier) {

        var Node<?> current = node;
        for (arg : identifier.path) {
            if (current instanceof SimpleNode<?>) {
                return null;
            } else if (current instanceof CompositeNode) {
                val currentComposite = (current as CompositeNode);
                
                current = currentComposite.getFirstCompositeByName(arg.nodeType);
                if(current == null) {
                    current = currentComposite.getFirstCompositeByName(arg.nodeType.withoutRevision());
                }
                if(current == null) {
                    current = currentComposite.getFirstSimpleByName(arg.nodeType);
                }
                if (current == null) {
                    current = currentComposite.getFirstSimpleByName(arg.nodeType.withoutRevision());
                } if (current == null) {
                    return null;
                }
            }
        }
        return current;
    }

    override requestCommit(DataModification<InstanceIdentifier, CompositeNode> modification) {
        val twoPhaseCommit = new NetconfDeviceTwoPhaseCommitTransaction(this, modification);
        twoPhaseCommit.prepare()
        return twoPhaseCommit;
    }

    def getInitialCapabilities() {
        val capabilities = client?.capabilities;
        if (capabilities == null) {
            return null;
        }
        if (cachedCapabilities == null) {
            cachedCapabilities = FluentIterable.from(capabilities).filter[
                contains("?") && contains("module=") && contains("revision=")].transform [
                val parts = split("\\?");
                val namespace = parts.get(0);
                val queryParams = FluentIterable.from(parts.get(1).split("&"));
                var revision = queryParams.findFirst[startsWith("revision=")]?.replaceAll("revision=", "");
                val moduleName = queryParams.findFirst[startsWith("module=")]?.replaceAll("module=", "");
                if (revision === null) {
                    logger.warn("Netconf device was not reporting revision correctly, trying to get amp;revision=");
                    revision = queryParams.findFirst[startsWith("&amp;revision=")]?.replaceAll("revision=", "");
                    if (revision != null) {
                        logger.warn("Netconf device returned revision incorectly escaped for {}", it)
                    }
                }
                if (revision == null) {
                    return QName.create(URI.create(namespace), null, moduleName);
                }
                return QName.create(namespace, revision, moduleName);
            ].toSet();
        }
        return cachedCapabilities;
    }

    override close() {
        confReaderReg?.close()
        operReaderReg?.close()
        client?.close()
    }

}

package class NetconfDeviceSchemaContextProvider {

    @Property
    val NetconfDevice device;

    @Property
    val SchemaSourceProvider<InputStream> sourceProvider;

    @Property
    var Optional<SchemaContext> currentContext;

    new(NetconfDevice device, SchemaSourceProvider<InputStream> sourceProvider) {
        _device = device
        _sourceProvider = sourceProvider
        _currentContext = Optional.absent();
    }

    def createContextFromCapabilities(Iterable<QName> capabilities) {
        val sourceContext = YangSourceContext.createFrom(capabilities, sourceProvider)
        if (!sourceContext.missingSources.empty) {
            device.logger.warn("Sources for following models are missing {}", sourceContext.missingSources);
        }
        device.logger.debug("Trying to create schema context from {}", sourceContext.validSources)
        val modelsToParse = YangSourceContext.getValidInputStreams(sourceContext);
        if (!sourceContext.validSources.empty) {
            val schemaContext = tryToCreateContext(modelsToParse);
            currentContext = Optional.fromNullable(schemaContext);
        } else {
            currentContext = Optional.absent();
        }
        if (currentContext.present) {
            device.logger.debug("Schema context successfully created.");
        }

    }

    def SchemaContext tryToCreateContext(List<InputStream> modelsToParse) {
        val parser = new YangParserImpl();
        try {

            val models = parser.parseYangModelsFromStreams(modelsToParse);
            val result = parser.resolveSchemaContext(models);
            return result;
        } catch (Exception e) {
            device.logger.debug("Error occured during parsing YANG schemas", e);
            return null;
        }
    }
}
