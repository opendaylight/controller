/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.testkit.JavaTestKit;
import akka.util.Timeout;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.CheckedFuture;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.datastore.DatastoreContext.Builder;
import org.opendaylight.controller.cluster.datastore.TransactionProxyTest.TestException;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModifications;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModificationsReply;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.DataExists;
import org.opendaylight.controller.cluster.datastore.messages.DataExistsReply;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;
import org.opendaylight.controller.cluster.datastore.messages.ReadData;
import org.opendaylight.controller.cluster.datastore.messages.ReadDataReply;
import org.opendaylight.controller.cluster.datastore.messages.ReadyLocalTransaction;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransactionReply;
import org.opendaylight.controller.cluster.datastore.modification.AbstractModification;
import org.opendaylight.controller.cluster.datastore.modification.Modification;
import org.opendaylight.controller.cluster.datastore.modification.WriteModification;
import org.opendaylight.controller.cluster.datastore.shardstrategy.DefaultShardStrategy;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategy;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategyFactory;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.cluster.datastore.utils.MockConfiguration;
import org.opendaylight.controller.cluster.raft.utils.DoNothingActor;
import org.opendaylight.controller.md.cluster.datastore.model.CarsModel;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

/**
 * Abstract base class for TransactionProxy unit tests.
 *
 * @author Thomas Pantelis
 */
public abstract class AbstractTransactionProxyTest {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private static ActorSystem system;

    private final Configuration configuration = new MockConfiguration() {
        Map<String, ShardStrategy> strategyMap = ImmutableMap.<String, ShardStrategy>builder().put(
                "junk", new ShardStrategy() {
                    @Override
                    public String findShard(YangInstanceIdentifier path) {
                        return "junk";
                    }
                }).put(
                "cars", new ShardStrategy() {
                    @Override
                    public String findShard(YangInstanceIdentifier path) {
                        return "cars";
                    }
                }).build();

        @Override
        public ShardStrategy getStrategyForModule(String moduleName) {
            return strategyMap.get(moduleName);
        }

        @Override
        public String getModuleNameFromNameSpace(String nameSpace) {
            if(TestModel.JUNK_QNAME.getNamespace().toASCIIString().equals(nameSpace)) {
                return "junk";
            } else if(CarsModel.BASE_QNAME.getNamespace().toASCIIString().equals(nameSpace)){
                return "cars";
            }
            return null;
        }
    };

    @Mock
    protected ActorContext mockActorContext;

    protected TransactionContextFactory mockComponentFactory;

    private SchemaContext schemaContext;

    @Mock
    private ClusterWrapper mockClusterWrapper;

    protected final String memberName = "mock-member";

    private final int operationTimeoutInSeconds = 2;
    protected final Builder dataStoreContextBuilder = DatastoreContext.newBuilder()
            .operationTimeoutInSeconds(operationTimeoutInSeconds);

    @BeforeClass
    public static void setUpClass() throws IOException {

        Config config = ConfigFactory.parseMap(ImmutableMap.<String, Object>builder().
                put("akka.actor.default-dispatcher.type",
                        "akka.testkit.CallingThreadDispatcherConfigurator").build()).
                withFallback(ConfigFactory.load());
        system = ActorSystem.create("test", config);
    }

    @AfterClass
    public static void tearDownClass() throws IOException {
        JavaTestKit.shutdownActorSystem(system);
        system = null;
    }

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);

        schemaContext = TestModel.createTestContext();

        doReturn(getSystem()).when(mockActorContext).getActorSystem();
        doReturn(getSystem().dispatchers().defaultGlobalDispatcher()).when(mockActorContext).getClientDispatcher();
        doReturn(memberName).when(mockActorContext).getCurrentMemberName();
        doReturn(new ShardStrategyFactory(configuration)).when(mockActorContext).getShardStrategyFactory();
        doReturn(schemaContext).when(mockActorContext).getSchemaContext();
        doReturn(new Timeout(operationTimeoutInSeconds, TimeUnit.SECONDS)).when(mockActorContext).getOperationTimeout();
        doReturn(mockClusterWrapper).when(mockActorContext).getClusterWrapper();
        doReturn(mockClusterWrapper).when(mockActorContext).getClusterWrapper();
        doReturn(dataStoreContextBuilder.build()).when(mockActorContext).getDatastoreContext();

        mockComponentFactory = TransactionContextFactory.create(mockActorContext);

        Timer timer = new MetricRegistry().timer("test");
        doReturn(timer).when(mockActorContext).getOperationTimer(any(String.class));
    }

    protected ActorSystem getSystem() {
        return system;
    }

    protected CreateTransaction eqCreateTransaction(final String memberName,
            final TransactionType type) {
        ArgumentMatcher<CreateTransaction> matcher = new ArgumentMatcher<CreateTransaction>() {
            @Override
            public boolean matches(Object argument) {
                if(CreateTransaction.class.equals(argument.getClass())) {
                    CreateTransaction obj = CreateTransaction.fromSerializable(argument);
                    return obj.getTransactionId().startsWith(memberName) &&
                            obj.getTransactionType() == type.ordinal();
                }

                return false;
            }
        };

        return argThat(matcher);
    }

    protected DataExists eqDataExists() {
        ArgumentMatcher<DataExists> matcher = new ArgumentMatcher<DataExists>() {
            @Override
            public boolean matches(Object argument) {
                return (argument instanceof DataExists) &&
                    ((DataExists)argument).getPath().equals(TestModel.TEST_PATH);
            }
        };

        return argThat(matcher);
    }

    protected ReadData eqReadData() {
        return eqReadData(TestModel.TEST_PATH);
    }

    protected ReadData eqReadData(final YangInstanceIdentifier path) {
        ArgumentMatcher<ReadData> matcher = new ArgumentMatcher<ReadData>() {
            @Override
            public boolean matches(Object argument) {
                return (argument instanceof ReadData) && ((ReadData)argument).getPath().equals(path);
            }
        };

        return argThat(matcher);
    }

    protected Future<Object> readyTxReply(String path) {
        return Futures.successful((Object)new ReadyTransactionReply(path));
    }


    protected Future<ReadDataReply> readDataReply(NormalizedNode<?, ?> data) {
        return Futures.successful(new ReadDataReply(data, DataStoreVersions.CURRENT_VERSION));
    }

    protected Future<DataExistsReply> dataExistsReply(boolean exists) {
        return Futures.successful(new DataExistsReply(exists, DataStoreVersions.CURRENT_VERSION));
    }

    protected Future<BatchedModificationsReply> batchedModificationsReply(int count) {
        return Futures.successful(new BatchedModificationsReply(count));
    }

    @SuppressWarnings("unchecked")
    protected Future<Object> incompleteFuture() {
        return mock(Future.class);
    }

    protected ActorSelection actorSelection(ActorRef actorRef) {
        return getSystem().actorSelection(actorRef.path());
    }

    protected void expectBatchedModifications(ActorRef actorRef, int count) {
        doReturn(batchedModificationsReply(count)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), isA(BatchedModifications.class), any(Timeout.class));
    }

    protected void expectBatchedModificationsReady(ActorRef actorRef) {
        expectBatchedModificationsReady(actorRef, false);
    }

    protected void expectBatchedModificationsReady(ActorRef actorRef, boolean doCommitOnReady) {
        doReturn(doCommitOnReady ? Futures.successful(new CommitTransactionReply().toSerializable()) :
            readyTxReply(actorRef.path().toString())).when(mockActorContext).executeOperationAsync(
                    eq(actorSelection(actorRef)), isA(BatchedModifications.class), any(Timeout.class));
    }

    protected void expectBatchedModifications(int count) {
        doReturn(batchedModificationsReply(count)).when(mockActorContext).executeOperationAsync(
                any(ActorSelection.class), isA(BatchedModifications.class), any(Timeout.class));
    }

    protected void expectIncompleteBatchedModifications() {
        doReturn(incompleteFuture()).when(mockActorContext).executeOperationAsync(
                any(ActorSelection.class), isA(BatchedModifications.class), any(Timeout.class));
    }

    protected void expectFailedBatchedModifications(ActorRef actorRef) {
        doReturn(Futures.failed(new TestException())).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), isA(BatchedModifications.class), any(Timeout.class));
    }

    protected void expectReadyLocalTransaction(ActorRef actorRef, boolean doCommitOnReady) {
        doReturn(doCommitOnReady ? Futures.successful(new CommitTransactionReply().toSerializable()) :
            readyTxReply(actorRef.path().toString())).when(mockActorContext).executeOperationAsync(
                    eq(actorSelection(actorRef)), isA(ReadyLocalTransaction.class), any(Timeout.class));
    }

    protected CreateTransactionReply createTransactionReply(ActorRef actorRef, short transactionVersion){
        return new CreateTransactionReply(actorRef.path().toString(), "txn-1", transactionVersion);
    }

    protected ActorRef setupActorContextWithoutInitialCreateTransaction(ActorSystem actorSystem) {
        return setupActorContextWithoutInitialCreateTransaction(actorSystem, DefaultShardStrategy.DEFAULT_SHARD);
    }

    protected Future<PrimaryShardInfo> primaryShardInfoReply(ActorSystem actorSystem, ActorRef actorRef) {
        return primaryShardInfoReply(actorSystem, actorRef, DataStoreVersions.CURRENT_VERSION);
    }

    protected Future<PrimaryShardInfo> primaryShardInfoReply(ActorSystem actorSystem, ActorRef actorRef,
            short transactionVersion) {
        return Futures.successful(new PrimaryShardInfo(actorSystem.actorSelection(actorRef.path()),
                transactionVersion, Optional.<DataTree>absent()));
    }

    protected ActorRef setupActorContextWithoutInitialCreateTransaction(ActorSystem actorSystem, String shardName) {
        return setupActorContextWithoutInitialCreateTransaction(actorSystem, shardName, DataStoreVersions.CURRENT_VERSION);
    }

    protected ActorRef setupActorContextWithoutInitialCreateTransaction(ActorSystem actorSystem, String shardName,
            short transactionVersion) {
        ActorRef actorRef = actorSystem.actorOf(Props.create(DoNothingActor.class));
        log.info("Created mock shard actor {}", actorRef);

        doReturn(actorSystem.actorSelection(actorRef.path())).
                when(mockActorContext).actorSelection(actorRef.path().toString());

        doReturn(primaryShardInfoReply(actorSystem, actorRef, transactionVersion)).
                when(mockActorContext).findPrimaryShardAsync(eq(shardName));

        return actorRef;
    }

    protected ActorRef setupActorContextWithInitialCreateTransaction(ActorSystem actorSystem,
            TransactionType type, short transactionVersion, String shardName) {
        ActorRef shardActorRef = setupActorContextWithoutInitialCreateTransaction(actorSystem, shardName,
                transactionVersion);

        return setupActorContextWithInitialCreateTransaction(actorSystem, type, transactionVersion,
                memberName, shardActorRef);
    }

    protected ActorRef setupActorContextWithInitialCreateTransaction(ActorSystem actorSystem,
            TransactionType type, short transactionVersion, String prefix, ActorRef shardActorRef) {

        ActorRef txActorRef;
        if(type == TransactionType.WRITE_ONLY &&
                dataStoreContextBuilder.build().isWriteOnlyTransactionOptimizationsEnabled()) {
            txActorRef = shardActorRef;
        } else {
            txActorRef = actorSystem.actorOf(Props.create(DoNothingActor.class));
            log.info("Created mock shard Tx actor {}", txActorRef);

            doReturn(actorSystem.actorSelection(txActorRef.path())).
                when(mockActorContext).actorSelection(txActorRef.path().toString());

            doReturn(Futures.successful(createTransactionReply(txActorRef, transactionVersion))).when(mockActorContext).
                executeOperationAsync(eq(actorSystem.actorSelection(shardActorRef.path())),
                        eqCreateTransaction(prefix, type), any(Timeout.class));
        }

        return txActorRef;
    }

    protected ActorRef setupActorContextWithInitialCreateTransaction(ActorSystem actorSystem, TransactionType type) {
        return setupActorContextWithInitialCreateTransaction(actorSystem, type, DataStoreVersions.CURRENT_VERSION,
                DefaultShardStrategy.DEFAULT_SHARD);
    }

    protected ActorRef setupActorContextWithInitialCreateTransaction(ActorSystem actorSystem, TransactionType type,
            String shardName) {
        return setupActorContextWithInitialCreateTransaction(actorSystem, type, DataStoreVersions.CURRENT_VERSION,
                shardName);
    }

    protected void propagateReadFailedExceptionCause(CheckedFuture<?, ReadFailedException> future)
            throws Throwable {

        try {
            future.checkedGet(5, TimeUnit.SECONDS);
            fail("Expected ReadFailedException");
        } catch(ReadFailedException e) {
            assertNotNull("Expected a cause", e.getCause());
            if(e.getCause().getCause() != null) {
                throw e.getCause().getCause();
            } else {
                throw e.getCause();
            }
        }
    }

    protected List<BatchedModifications> captureBatchedModifications(ActorRef actorRef) {
        ArgumentCaptor<BatchedModifications> batchedModificationsCaptor =
                ArgumentCaptor.forClass(BatchedModifications.class);
        verify(mockActorContext, Mockito.atLeastOnce()).executeOperationAsync(
                eq(actorSelection(actorRef)), batchedModificationsCaptor.capture(), any(Timeout.class));

        List<BatchedModifications> batchedModifications = filterCaptured(
                batchedModificationsCaptor, BatchedModifications.class);
        return batchedModifications;
    }

    protected <T> List<T> filterCaptured(ArgumentCaptor<T> captor, Class<T> type) {
        List<T> captured = new ArrayList<>();
        for(T c: captor.getAllValues()) {
            if(type.isInstance(c)) {
                captured.add(c);
            }
        }

        return captured;
    }

    protected void verifyOneBatchedModification(ActorRef actorRef, Modification expected, boolean expIsReady) {
        List<BatchedModifications> batchedModifications = captureBatchedModifications(actorRef);
        assertEquals("Captured BatchedModifications count", 1, batchedModifications.size());

        verifyBatchedModifications(batchedModifications.get(0), expIsReady, expIsReady, expected);
    }

    protected void verifyBatchedModifications(Object message, boolean expIsReady, Modification... expected) {
        verifyBatchedModifications(message, expIsReady, false, expected);
    }

    protected void verifyBatchedModifications(Object message, boolean expIsReady, boolean expIsDoCommitOnReady,
            Modification... expected) {
        assertEquals("Message type", BatchedModifications.class, message.getClass());
        BatchedModifications batchedModifications = (BatchedModifications)message;
        assertEquals("BatchedModifications size", expected.length, batchedModifications.getModifications().size());
        assertEquals("isReady", expIsReady, batchedModifications.isReady());
        assertEquals("isDoCommitOnReady", expIsDoCommitOnReady, batchedModifications.isDoCommitOnReady());
        for(int i = 0; i < batchedModifications.getModifications().size(); i++) {
            Modification actual = batchedModifications.getModifications().get(i);
            assertEquals("Modification type", expected[i].getClass(), actual.getClass());
            assertEquals("getPath", ((AbstractModification)expected[i]).getPath(),
                    ((AbstractModification)actual).getPath());
            if(actual instanceof WriteModification) {
                assertEquals("getData", ((WriteModification)expected[i]).getData(),
                        ((WriteModification)actual).getData());
            }
        }
    }

    protected void verifyCohortFutures(AbstractThreePhaseCommitCohort<?> proxy,
            Object... expReplies) throws Exception {
            assertEquals("getReadyOperationFutures size", expReplies.length,
                    proxy.getCohortFutures().size());

            List<Object> futureResults = new ArrayList<>();
            for( Future<?> future: proxy.getCohortFutures()) {
                assertNotNull("Ready operation Future is null", future);
                try {
                    futureResults.add(Await.result(future, Duration.create(5, TimeUnit.SECONDS)));
                } catch(Exception e) {
                    futureResults.add(e);
                }
            }

            for(int i = 0; i < expReplies.length; i++) {
                Object expReply = expReplies[i];
                boolean found = false;
                Iterator<?> iter = futureResults.iterator();
                while(iter.hasNext()) {
                    Object actual = iter.next();
                    if(CommitTransactionReply.isSerializedType(expReply) &&
                       CommitTransactionReply.isSerializedType(actual)) {
                        found = true;
                    } else if(expReply instanceof ActorSelection && Objects.equal(expReply, actual)) {
                        found = true;
                    } else if(expReply instanceof Class && ((Class<?>)expReply).isInstance(actual)) {
                        found = true;
                    }

                    if(found) {
                        iter.remove();
                        break;
                    }
                }

                if(!found) {
                    fail(String.format("No cohort Future response found for %s. Actual: %s", expReply, futureResults));
                }
            }
        }
}
