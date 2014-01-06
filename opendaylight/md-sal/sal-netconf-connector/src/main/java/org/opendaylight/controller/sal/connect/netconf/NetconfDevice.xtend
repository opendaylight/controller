package org.opendaylight.controller.sal.connect.netconf

import org.opendaylight.controller.sal.core.api.mount.MountProvisionInstance
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier
import org.opendaylight.controller.md.sal.common.api.data.DataReader
import org.opendaylight.yangtools.yang.data.api.CompositeNode
import org.opendaylight.controller.netconf.client.NetconfClient
import org.opendaylight.controller.sal.core.api.RpcImplementation
import static extension org.opendaylight.controller.sal.connect.netconf.NetconfMapping.*
import java.net.InetSocketAddress
import org.opendaylight.yangtools.yang.data.api.Node
import org.opendaylight.yangtools.yang.data.api.SimpleNode
import org.opendaylight.yangtools.yang.common.QName
import java.util.Collections
import org.opendaylight.controller.netconf.client.NetconfClientDispatcher
import org.opendaylight.yangtools.concepts.Registration
import org.opendaylight.controller.sal.core.api.Provider
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession
import org.opendaylight.controller.sal.core.api.mount.MountProvisionService
import static org.opendaylight.controller.sal.connect.netconf.InventoryUtils.*;
import org.opendaylight.controller.sal.core.api.data.DataBrokerService
import org.opendaylight.controller.sal.core.api.data.DataModificationTransaction
import org.opendaylight.yangtools.yang.data.impl.SimpleNodeTOImpl
import org.opendaylight.yangtools.yang.data.impl.CompositeNodeTOImpl
import org.opendaylight.protocol.framework.ReconnectStrategy
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler
import org.opendaylight.controller.md.sal.common.api.data.DataModification
import com.google.common.collect.FluentIterable
import org.opendaylight.yangtools.yang.model.api.SchemaContext
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.NetconfState
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl
import java.io.InputStream
import org.slf4j.LoggerFactory
import org.slf4j.Logger
import org.opendaylight.controller.netconf.client.AbstractNetconfClientNotifySessionListener
import org.opendaylight.controller.netconf.client.NetconfClientSession
import org.opendaylight.controller.netconf.api.NetconfMessage
import io.netty.util.concurrent.EventExecutor

import java.util.Map
import java.util.Set
import com.google.common.collect.ImmutableMap

import org.opendaylight.yangtools.yang.model.util.repo.AbstractCachingSchemaSourceProvider
import org.opendaylight.yangtools.yang.model.util.repo.SchemaSourceProvider
import com.google.common.base.Optional
import com.google.common.collect.ImmutableList
import org.opendaylight.yangtools.yang.model.util.repo.SchemaSourceProviders
import static com.google.common.base.Preconditions.*;
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import org.opendaylight.controller.netconf.client.NetconfClientSessionListener
import io.netty.util.concurrent.Promise
import org.opendaylight.controller.netconf.util.xml.XmlElement
import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants
import java.util.concurrent.ExecutionException
import java.util.concurrent.locks.ReentrantLock

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

    private NetconfDeviceSchemaContextProvider schemaContextProvider

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

        val listener = new NetconfDeviceListener(this,eventExecutor);
        val task = startClientTask(dispatcher, listener)
        if(mountInstance != null) {
            confReaderReg = mountInstance.registerConfigurationReader(ROOT_PATH, this);
            operReaderReg = mountInstance.registerOperationalReader(ROOT_PATH, this);
        }
        return processingExecutor.submit(task) as Future<Void>;

    //commitHandlerReg = mountInstance.registerCommitHandler(path,this);
    }

    def Optional<SchemaContext> getSchemaContext() {
        if (schemaContextProvider == null) {
            return Optional.absent();
        }
        return schemaContextProvider.currentContext;
    }

    private def Runnable startClientTask(NetconfClientDispatcher dispatcher, NetconfDeviceListener listener) {
        return [ |
            logger.info("Starting Netconf Client on: {}", socketAddress);
            client = NetconfClient.clientFor(name, socketAddress, reconnectStrategy, dispatcher, listener);
            logger.debug("Initial capabilities {}", initialCapabilities);
            var SchemaSourceProvider<String> delegate;
            if (initialCapabilities.contains(NetconfMapping.IETF_NETCONF_MONITORING_MODULE)) {
                delegate = new NetconfDeviceSchemaSourceProvider(this);
            } else {
                logger.info("Device does not support IETF Netconf Monitoring.", socketAddress);
                delegate = SchemaSourceProviders.<String>noopProvider();
            }
            val sourceProvider = schemaSourceProvider.createInstanceFor(delegate);
            schemaContextProvider = new NetconfDeviceSchemaContextProvider(this, sourceProvider);
            schemaContextProvider.createContextFromCapabilities(initialCapabilities);
            if (mountInstance != null && schemaContext.isPresent) {
                mountInstance.schemaContext = schemaContext.get();
            }
        ]
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
        addLeaf("stream",streamName);
        invokeRpc(QName,toInstance())
    }

    override invokeRpc(QName rpc, CompositeNode input) {
        val message = rpc.toRpcMessage(input);
        val result = client.sendMessage(message, messegeRetryCount, messageTimeoutCount);
        return result.toRpcResult();
    }

    override getProviderFunctionality() {
        Collections.emptySet
    }

    override onSessionInitiated(ProviderSession session) {
        val dataBroker = session.getService(DataBrokerService);

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

    def Node<?> findNode(CompositeNode node, InstanceIdentifier identifier) {

        var Node<?> current = node;
        for (arg : identifier.path) {
            if (current instanceof SimpleNode<?>) {
                return null;
            } else if (current instanceof CompositeNode) {
                val currentComposite = (current as CompositeNode);

                current = currentComposite.getFirstCompositeByName(arg.nodeType.withoutRevision());
                if (current == null) {
                    current = currentComposite.getFirstSimpleByName(arg.nodeType.withoutRevision());
                }
                if (current == null) {
                    return null;
                }
            }
        }
        return current;
    }

    override requestCommit(DataModification<InstanceIdentifier, CompositeNode> modification) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
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
                val revision = queryParams.findFirst[startsWith("revision=")].replaceAll("revision=", "");
                val moduleName = queryParams.findFirst[startsWith("module=")].replaceAll("module=", "");
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

package class NetconfDeviceListener extends NetconfClientSessionListener {

    val NetconfDevice device
    val EventExecutor eventExecutor

    new(NetconfDevice device,EventExecutor eventExecutor) {
        this.device = device
        this.eventExecutor = eventExecutor
    }

    var Promise<NetconfMessage> messagePromise;
    val promiseLock = new ReentrantLock;
    
    override onMessage(NetconfClientSession session, NetconfMessage message) {
        if (isNotification(message)) {
            onNotification(session, message);
        } else try {
            promiseLock.lock
            if (messagePromise != null) {
                messagePromise.setSuccess(message);
                messagePromise = null;
            }
        } finally {
            promiseLock.unlock
        }
    }

    /**
     * Method intended to customize notification processing.
     * 
     * @param session
     *            {@see
     *            NetconfClientSessionListener#onMessage(NetconfClientSession,
     *            NetconfMessage)}
     * @param message
     *            {@see
     *            NetconfClientSessionListener#onMessage(NetconfClientSession,
     *            NetconfMessage)}
     */
    def void onNotification(NetconfClientSession session, NetconfMessage message) {
        device.logger.debug("Received NETCONF notification.",message);
        val domNotification = message?.toCompositeNode?.notificationBody;
        if(domNotification != null) {
            device?.mountInstance?.publish(domNotification);
        }
    }
    
    private static def CompositeNode getNotificationBody(CompositeNode node) {
        for(child : node.children) {
            if(child instanceof CompositeNode) {
                return child as CompositeNode;
            }
        }
    }

    override getLastMessage(int attempts, int attemptMsDelay) throws InterruptedException {
        val promise = promiseReply();
        val messageAvailable = promise.await(attempts + attemptMsDelay);
        if (messageAvailable) {
            try {
                return promise.get();
            } catch (ExecutionException e) {
                throw new IllegalStateException(e);
            }
        }

        throw new IllegalStateException("Unsuccessful after " + attempts + " attempts.");

    // throw new TimeoutException("Message was not received on time.");
    }

    def Promise<NetconfMessage> promiseReply() {
        promiseLock.lock
        try {
        if (messagePromise == null) {
            messagePromise = eventExecutor.newPromise();
            return messagePromise;
        }
        return messagePromise;
        } finally {
            promiseLock.unlock
        }
    }

    def boolean isNotification(NetconfMessage message) {
        val xmle = XmlElement.fromDomDocument(message.getDocument());
        return XmlNetconfConstants.NOTIFICATION_ELEMENT_NAME.equals(xmle.getName());
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
    }

    def createContextFromCapabilities(Iterable<QName> capabilities) {

        val modelsToParse = ImmutableMap.<QName, InputStream>builder();
        for (cap : capabilities) {
            val source = sourceProvider.getSchemaSource(cap.localName, Optional.fromNullable(cap.formattedRevision));
            if (source.present) {
                modelsToParse.put(cap, source.get());
            }
        }
        val context = tryToCreateContext(modelsToParse.build);
        currentContext = Optional.fromNullable(context);
    }

    def SchemaContext tryToCreateContext(Map<QName, InputStream> modelsToParse) {
        val parser = new YangParserImpl();
        try {
            val models = parser.parseYangModelsFromStreams(ImmutableList.copyOf(modelsToParse.values));
            val result = parser.resolveSchemaContext(models);
            return result;
        } catch (Exception e) {
            device.logger.debug("Error occured during parsing YANG schemas", e);
            return null;
        }
    }
}

package class NetconfDeviceSchemaSourceProvider implements SchemaSourceProvider<String> {

    val NetconfDevice device;

    new(NetconfDevice device) {
        this.device = device;
    }

    override getSchemaSource(String moduleName, Optional<String> revision) {
        val it = ImmutableCompositeNode.builder() //
        setQName(QName::create(NetconfState.QNAME, "get-schema")) //
        addLeaf("format", "yang")
        addLeaf("identifier", moduleName)
        if (revision.present) {
            addLeaf("version", revision.get())
        }

        device.logger.info("Loading YANG schema source for {}:{}", moduleName, revision)
        val schemaReply = device.invokeRpc(getQName(), toInstance());

        if (schemaReply.successful) {
            val schemaBody = schemaReply.result.getFirstSimpleByName(
                QName::create(NetconfState.QNAME.namespace, null, "data"))?.value;
            device.logger.info("YANG Schema successfully received for: {}:{}", moduleName, revision);
            return Optional.of(schemaBody as String);
        }
        return Optional.absent();
    }
}
