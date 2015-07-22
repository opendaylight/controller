package org.opendaylight.controller.netconf.test.tool;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import org.opendaylight.controller.cluster.datastore.ConcurrentDOMDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStoreFactory;
import org.opendaylight.controller.netconf.api.Capability;
import org.opendaylight.controller.netconf.api.monitoring.CapabilityListener;
import org.opendaylight.controller.netconf.impl.SessionIdProvider;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperation;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationService;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory;
import org.opendaylight.controller.netconf.mdsal.connector.CurrentSchemaContext;
import org.opendaylight.controller.netconf.mdsal.connector.TransactionProvider;
import org.opendaylight.controller.netconf.mdsal.connector.ops.Commit;
import org.opendaylight.controller.netconf.mdsal.connector.ops.DiscardChanges;
import org.opendaylight.controller.netconf.mdsal.connector.ops.EditConfig;
import org.opendaylight.controller.netconf.mdsal.connector.ops.Lock;
import org.opendaylight.controller.netconf.mdsal.connector.ops.Unlock;
import org.opendaylight.controller.netconf.mdsal.connector.ops.get.Get;
import org.opendaylight.controller.netconf.mdsal.connector.ops.get.GetConfig;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.NetconfState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.Yang;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.Schemas;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.schemas.Schema;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MdsalOperationProvider implements NetconfOperationServiceFactory {

    private static final Logger LOG = LoggerFactory
            .getLogger(MdsalOperationProvider.class);

    private final Set<Capability> caps;
    private final MdsalOperationService mdsalOperationService;

    public MdsalOperationProvider(final SessionIdProvider idProvider,
                                  final Set<Capability> caps,
                                  final SchemaContext schemaContext) {
        this.caps = caps;
        mdsalOperationService = new MdsalOperationService(
                idProvider.getCurrentSessionId(), schemaContext, caps);
    }

    @Override
    public Set<Capability> getCapabilities() {
        return caps;
    }

    @Override
    public AutoCloseable registerCapabilityListener(
            CapabilityListener listener) {
        listener.onCapabilitiesAdded(caps);
        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
            }
        };
    }

    @Override
    public NetconfOperationService createService(String netconfSessionIdForReporting) {
        return mdsalOperationService;
    }

    static class MdsalOperationService implements NetconfOperationService {
        private final long currentSessionId;
        private final SchemaContext schemaContext;
        private final Set<Capability> caps;

        public MdsalOperationService(final long currentSessionId,
                                     final SchemaContext schemaContext,
                                     final Set<Capability> caps) {
            this.currentSessionId = currentSessionId;
            this.schemaContext = schemaContext;
            this.caps = caps;
        }

        @Override
        public Set<NetconfOperation> getNetconfOperations() {
            final SchemaService schemaService = createSchemaService();

            final ConcurrentDOMDataBroker cdb = createDataStore(schemaService);
            TransactionProvider transactionProvider = new TransactionProvider(cdb, String.valueOf(currentSessionId));
            CurrentSchemaContext currentSchemaContext = new CurrentSchemaContext(schemaService);

            ContainerNode netconf = createNetconfState();

            YangInstanceIdentifier yangInstanceIdentifier = YangInstanceIdentifier.builder().node(NetconfState.QNAME)
                    .build();

            final DOMDataWriteTransaction tx = cdb.newWriteOnlyTransaction();
            tx.put(LogicalDatastoreType.OPERATIONAL, yangInstanceIdentifier, netconf);

            try {
                tx.submit().checkedGet();
                LOG.debug("Netconf state updated successfully");
            } catch (TransactionCommitFailedException e) {
                LOG.warn("Unable to update netconf state", e);
            }

            final Get get = new Get(String.valueOf(currentSessionId), currentSchemaContext, transactionProvider);
            final EditConfig editConfig = new EditConfig(String.valueOf(currentSessionId), currentSchemaContext,
                    transactionProvider);
            final GetConfig getConfig = new GetConfig(String.valueOf(currentSessionId), currentSchemaContext,
                    transactionProvider);
            final Commit commit = new Commit(String.valueOf(currentSessionId), transactionProvider);
            final Lock lock = new Lock(String.valueOf(currentSessionId));
            final Unlock unLock = new Unlock(String.valueOf(currentSessionId));
            final DiscardChanges discardChanges = new DiscardChanges(String.valueOf(currentSessionId), transactionProvider);

            return Sets.<NetconfOperation>newHashSet(get, getConfig,
                    editConfig, commit, lock, unLock, discardChanges);
        }

        @Override
        public void close() {
        }

        private ContainerNode createNetconfState() {
            DummyMonitoringService monitor = new DummyMonitoringService(
                    caps);

            final QName identifier = QName.create(Schema.QNAME, "identifier");
            final QName version = QName.create(Schema.QNAME, "version");
            final QName format = QName.create(Schema.QNAME, "format");
            final QName location = QName.create(Schema.QNAME, "location");
            final QName namespace = QName.create(Schema.QNAME, "namespace");

            CollectionNodeBuilder<MapEntryNode, MapNode> schemaMapEntryNodeMapNodeCollectionNodeBuilder = Builders
                    .mapBuilder().withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(Schema.QNAME));
            LeafSetEntryNode locationLeafSetEntryNode = Builders.leafSetEntryBuilder().withNodeIdentifier(
                            new YangInstanceIdentifier.NodeWithValue(location, "NETCONF")).withValue("NETCONF").build();

            Map<QName, Object> keyValues = Maps.newHashMap();
            for (final Schema schema : monitor.getSchemas().getSchema()) {
                keyValues.put(identifier, schema.getIdentifier());
                keyValues.put(version, schema.getVersion());
                keyValues.put(format, Yang.QNAME);

                MapEntryNode schemaMapEntryNode = Builders.mapEntryBuilder().withNodeIdentifier(
                                new YangInstanceIdentifier.NodeIdentifierWithPredicates(Schema.QNAME, keyValues))
                        .withChild(Builders.leafBuilder().withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(
                                identifier)).withValue(schema.getIdentifier()).build())
                        .withChild(Builders.leafBuilder().withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(
                                                version)).withValue(schema.getVersion()).build())
                        .withChild(Builders.leafBuilder().withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(
                                                format)).withValue(Yang.QNAME).build())
                        .withChild(Builders.leafBuilder().withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(
                                                namespace)).withValue(schema.getNamespace().getValue()).build())
                        .withChild((DataContainerChild<?, ?>) Builders.leafSetBuilder().withNodeIdentifier(
                                        new YangInstanceIdentifier.NodeIdentifier(location))
                                .withChild(locationLeafSetEntryNode).build())
                        .build();

                schemaMapEntryNodeMapNodeCollectionNodeBuilder.withChild(schemaMapEntryNode);
            }

            DataContainerChild<?, ?> schemaList = schemaMapEntryNodeMapNodeCollectionNodeBuilder.build();

            ContainerNode schemasContainer = Builders.containerBuilder().withNodeIdentifier(
                    new YangInstanceIdentifier.NodeIdentifier(Schemas.QNAME)).withChild(schemaList).build();
            return (ContainerNode) Builders.containerBuilder().withNodeIdentifier(
                    new YangInstanceIdentifier.NodeIdentifier(NetconfState.QNAME)).withChild(schemasContainer).build();
        }

        private ConcurrentDOMDataBroker createDataStore(SchemaService schemaService) {
            final DOMStore operStore = InMemoryDOMDataStoreFactory
                    .create("DOM-OPER", schemaService);
            final DOMStore configStore = InMemoryDOMDataStoreFactory
                    .create("DOM-CFG", schemaService);

            ExecutorService listenableFutureExecutor = SpecialExecutors.newBlockingBoundedCachedThreadPool(
                    16, 16, "CommitFutures");

            final EnumMap<LogicalDatastoreType, DOMStore> datastores = new EnumMap<>(LogicalDatastoreType.class);
            datastores.put(LogicalDatastoreType.CONFIGURATION, configStore);
            datastores.put(LogicalDatastoreType.OPERATIONAL, operStore);

            return new ConcurrentDOMDataBroker(datastores, listenableFutureExecutor);
        }

        private SchemaService createSchemaService() {
            return new SchemaService() {

                @Override
                public void addModule(Module module) {
                }

                @Override
                public void removeModule(Module module) {

                }

                @Override
                public SchemaContext getSessionContext() {
                    return schemaContext;
                }

                @Override
                public SchemaContext getGlobalContext() {
                    return schemaContext;
                }

                @Override
                public ListenerRegistration<SchemaContextListener> registerSchemaContextListener(
                        final SchemaContextListener listener) {
                    listener.onGlobalContextUpdated(getGlobalContext());
                    return new ListenerRegistration<SchemaContextListener>() {
                        @Override
                        public void close() {

                        }

                        @Override
                        public SchemaContextListener getInstance() {
                            return listener;
                        }
                    };
                }
            };
        }
    }

}
