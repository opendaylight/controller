/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.benchmark.sharding.impl;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.util.ListenerRegistry;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;
import org.opendaylight.yangtools.yang.parser.spi.meta.ReactorException;
import org.opendaylight.yangtools.yang.parser.spi.source.SourceException;
import org.opendaylight.yangtools.yang.parser.stmt.reactor.CrossSourceStatementReactor;
import org.opendaylight.yangtools.yang.parser.stmt.rfc6020.YangInferencePipeline;

public class ShardingSimpleTest {

    // TODO add models to test resources, change the path
    private static final String DATASTORE_TEST_YANG = "/shardingsimple.yang";

    MockSchemaService schemaService = new MockSchemaService();

    @Before
    public void setUp() throws Exception {
        schemaService.changeSchema(createTestContext());
    }

//    @Test
//    public void test() {
//        final ShardedDOMDataTree dataTreeShardingService = new ShardedDOMDataTree();
//
//        final RpcProviderRegistry rpcRegistry = new RpcProviderRegistryMock();
//
//        DistributedShardFactory distributedShardFactory = new DistributedShardFactory() {
//            @Override
//            public DistributedShardRegistration createDistributedShard(DOMDataTreeIdentifier prefix,
//                                                                       Collection<MemberName> replicaMembers)
//                    throws DOMDataTreeShardingConflictException,
//                    DOMDataTreeProducerException, DOMDataTreeShardCreationFailedException {
//                return null;
//            }
//        };
//
//        final ShardingSimpleProvider shardingSimpleProvider =
//                new ShardingSimpleProvider(rpcRegistry, dataTreeShardingService, dataTreeShardingService,
//                        distributedShardFactory, schemaService);
//        shardingSimpleProvider.init();
//    }

    public static SchemaContext createTestContext() throws ReactorException {
        return parseYangStreams(Collections.singletonList(getInputStream()));
    }

    private static InputStream getInputStream() {
        return ShardingSimpleTest.class.getResourceAsStream(DATASTORE_TEST_YANG);
    }

    private static SchemaContext parseYangStreams(final List<InputStream> streams)
            throws SourceException, ReactorException {

        final CrossSourceStatementReactor.BuildAction reactor = YangInferencePipeline.RFC6020_REACTOR
                .newBuild();
        return reactor.buildEffective(streams);
    }

    public static final class MockSchemaService implements SchemaService, SchemaContextProvider {

        private SchemaContext schemaContext;

        ListenerRegistry<SchemaContextListener> listeners = ListenerRegistry.create();

        @Override
        public void addModule(final Module module) {
            throw new UnsupportedOperationException();
        }

        @Override
        public synchronized SchemaContext getGlobalContext() {
            return schemaContext;
        }

        @Override
        public synchronized SchemaContext getSessionContext() {
            return schemaContext;
        }

        @Override
        public ListenerRegistration<SchemaContextListener> registerSchemaContextListener(
                final SchemaContextListener listener) {
            return listeners.register(listener);
        }

        @Override
        public void removeModule(final Module module) {
            throw new UnsupportedOperationException();
        }

        @Override
        public synchronized SchemaContext getSchemaContext() {
            return schemaContext;
        }

        public synchronized void changeSchema(final SchemaContext newContext) {
            schemaContext = newContext;
            for (final ListenerRegistration<SchemaContextListener> listener : listeners) {
                listener.getInstance().onGlobalContextUpdated(schemaContext);
            }
        }
    }

}
