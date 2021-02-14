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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.testkit.javadsl.TestKit;
import akka.util.Timeout;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.FluentFuture;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
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
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.controller.cluster.datastore.utils.MockConfiguration;
import org.opendaylight.controller.cluster.raft.utils.DoNothingActor;
import org.opendaylight.controller.md.cluster.datastore.model.CarsModel;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

/**
 * Abstract base class for TransactionProxy unit tests.
 *
 * @author Thomas Pantelis
 */
public abstract class AbstractTransactionProxyTest extends AbstractTest {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private static ActorSystem system;
    private static SchemaContext SCHEMA_CONTEXT;

    private final Configuration configuration = new MockConfiguration() {
        Map<String, ShardStrategy> strategyMap = ImmutableMap.<String, ShardStrategy>builder().put(
                TestModel.JUNK_QNAME.getLocalName(), new ShardStrategy() {
                    @Override
                    public String findShard(final YangInstanceIdentifier path) {
                        return TestModel.JUNK_QNAME.getLocalName();
                    }

                    @Override
                    public YangInstanceIdentifier getPrefixForPath(final YangInstanceIdentifier path) {
                        return YangInstanceIdentifier.empty();
                    }
                }).put(
                CarsModel.BASE_QNAME.getLocalName(), new ShardStrategy() {
                    @Override
                    public String findShard(final YangInstanceIdentifier path) {
                        return CarsModel.BASE_QNAME.getLocalName();
                    }

                    @Override
                    public YangInstanceIdentifier getPrefixForPath(final YangInstanceIdentifier path) {
                        return YangInstanceIdentifier.empty();
                    }
                }).build();

        @Override
        public ShardStrategy getStrategyForModule(final String moduleName) {
            return strategyMap.get(moduleName);
        }

        @Override
        public String getModuleNameFromNameSpace(final String nameSpace) {
            if (TestModel.JUNK_QNAME.getNamespace().toString().equals(nameSpace)) {
                return TestModel.JUNK_QNAME.getLocalName();
            } else if (CarsModel.BASE_QNAME.getNamespace().toString().equals(nameSpace)) {
                return CarsModel.BASE_QNAME.getLocalName();
            }
            return null;
        }
    };

    @Mock
    protected ActorUtils mockActorContext;

    protected TransactionContextFactory mockComponentFactory;

    @Mock
    private ClusterWrapper mockClusterWrapper;

    protected final String memberName = "mock-member";

    private final int operationTimeoutInSeconds = 2;
    protected final Builder dataStoreContextBuilder = DatastoreContext.newBuilder()
            .operationTimeoutInSeconds(operationTimeoutInSeconds);

    @BeforeClass
    public static void setUpClass() {

        Config config = ConfigFactory.parseMap(ImmutableMap.<String, Object>builder()
                .put("akka.actor.default-dispatcher.type",
                        "akka.testkit.CallingThreadDispatcherConfigurator").build())
                .withFallback(ConfigFactory.load());
        system = ActorSystem.create("test", config);
        SCHEMA_CONTEXT = TestModel.createTestContext();
    }

    @AfterClass
    public static void tearDownClass() {
        TestKit.shutdownActorSystem(system);
        system = null;
        SCHEMA_CONTEXT = null;
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(getSystem()).when(mockActorContext).getActorSystem();
        doReturn(getSystem().dispatchers().defaultGlobalDispatcher()).when(mockActorContext).getClientDispatcher();
        doReturn(MemberName.forName(memberName)).when(mockActorContext).getCurrentMemberName();
        doReturn(new ShardStrategyFactory(configuration,
                LogicalDatastoreType.CONFIGURATION)).when(mockActorContext).getShardStrategyFactory();
        doReturn(SCHEMA_CONTEXT).when(mockActorContext).getSchemaContext();
        doReturn(new Timeout(operationTimeoutInSeconds, TimeUnit.SECONDS)).when(mockActorContext).getOperationTimeout();
        doReturn(mockClusterWrapper).when(mockActorContext).getClusterWrapper();
        doReturn(mockClusterWrapper).when(mockActorContext).getClusterWrapper();
        doReturn(dataStoreContextBuilder.build()).when(mockActorContext).getDatastoreContext();
        doReturn(new Timeout(5, TimeUnit.SECONDS)).when(mockActorContext).getTransactionCommitOperationTimeout();

        final ClientIdentifier mockClientId = MockIdentifiers.clientIdentifier(getClass(), memberName);
        mockComponentFactory = new TransactionContextFactory(mockActorContext, mockClientId);

        Timer timer = new MetricRegistry().timer("test");
        doReturn(timer).when(mockActorContext).getOperationTimer(any(String.class));
    }

    protected ActorSystem getSystem() {
        return system;
    }

    protected CreateTransaction eqCreateTransaction(final String expMemberName,
            final TransactionType type) {
        class CreateTransactionArgumentMatcher implements ArgumentMatcher<CreateTransaction> {
            @Override
            public boolean matches(final CreateTransaction argument) {
                return argument.getTransactionId().getHistoryId().getClientId().getFrontendId().getMemberName()
                        .getName().equals(expMemberName) && argument.getTransactionType() == type.ordinal();
            }
        }

        return argThat(new CreateTransactionArgumentMatcher());
    }

    protected DataExists eqDataExists() {
        class DataExistsArgumentMatcher implements ArgumentMatcher<DataExists> {
            @Override
            public boolean matches(final DataExists argument) {
                return argument.getPath().equals(TestModel.TEST_PATH);
            }
        }

        return argThat(new DataExistsArgumentMatcher());
    }

    protected ReadData eqReadData() {
        return eqReadData(TestModel.TEST_PATH);
    }

    protected ReadData eqReadData(final YangInstanceIdentifier path) {
        class ReadDataArgumentMatcher implements ArgumentMatcher<ReadData> {
            @Override
            public boolean matches(final ReadData argument) {
                return argument.getPath().equals(path);
            }
        }

        return argThat(new ReadDataArgumentMatcher());
    }

    protected Future<Object> readyTxReply(final String path) {
        return Futures.successful((Object)new ReadyTransactionReply(path));
    }


    protected Future<ReadDataReply> readDataReply(final NormalizedNode data) {
        return Futures.successful(new ReadDataReply(data, DataStoreVersions.CURRENT_VERSION));
    }

    protected Future<DataExistsReply> dataExistsReply(final boolean exists) {
        return Futures.successful(new DataExistsReply(exists, DataStoreVersions.CURRENT_VERSION));
    }

    protected Future<BatchedModificationsReply> batchedModificationsReply(final int count) {
        return Futures.successful(new BatchedModificationsReply(count));
    }

    @SuppressWarnings("unchecked")
    protected Future<Object> incompleteFuture() {
        return mock(Future.class);
    }

    protected ActorSelection actorSelection(final ActorRef actorRef) {
        return getSystem().actorSelection(actorRef.path());
    }

    protected void expectBatchedModifications(final ActorRef actorRef, final int count) {
        doReturn(batchedModificationsReply(count)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), isA(BatchedModifications.class), any(Timeout.class));
    }

    protected void expectBatchedModifications(final int count) {
        doReturn(batchedModificationsReply(count)).when(mockActorContext).executeOperationAsync(
                any(ActorSelection.class), isA(BatchedModifications.class), any(Timeout.class));
    }

    protected void expectBatchedModificationsReady(final ActorRef actorRef) {
        expectBatchedModificationsReady(actorRef, false);
    }

    protected void expectBatchedModificationsReady(final ActorRef actorRef, final boolean doCommitOnReady) {
        doReturn(doCommitOnReady ? Futures.successful(new CommitTransactionReply().toSerializable()) :
            readyTxReply(actorRef.path().toString())).when(mockActorContext).executeOperationAsync(
                    eq(actorSelection(actorRef)), isA(BatchedModifications.class), any(Timeout.class));
    }

    protected void expectIncompleteBatchedModifications() {
        doReturn(incompleteFuture()).when(mockActorContext).executeOperationAsync(
                any(ActorSelection.class), isA(BatchedModifications.class), any(Timeout.class));
    }

    protected void expectFailedBatchedModifications(final ActorRef actorRef) {
        doReturn(Futures.failed(new TestException())).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), isA(BatchedModifications.class), any(Timeout.class));
    }

    protected void expectReadyLocalTransaction(final ActorRef actorRef, final boolean doCommitOnReady) {
        doReturn(doCommitOnReady ? Futures.successful(new CommitTransactionReply().toSerializable()) :
            readyTxReply(actorRef.path().toString())).when(mockActorContext).executeOperationAsync(
                    eq(actorSelection(actorRef)), isA(ReadyLocalTransaction.class), any(Timeout.class));
    }

    protected CreateTransactionReply createTransactionReply(final ActorRef actorRef, final short transactionVersion) {
        return new CreateTransactionReply(actorRef.path().toString(), nextTransactionId(), transactionVersion);
    }

    protected ActorRef setupActorContextWithoutInitialCreateTransaction(final ActorSystem actorSystem) {
        return setupActorContextWithoutInitialCreateTransaction(actorSystem, DefaultShardStrategy.DEFAULT_SHARD);
    }

    protected ActorRef setupActorContextWithoutInitialCreateTransaction(final ActorSystem actorSystem,
            final String shardName) {
        return setupActorContextWithoutInitialCreateTransaction(actorSystem, shardName,
                DataStoreVersions.CURRENT_VERSION);
    }

    protected ActorRef setupActorContextWithoutInitialCreateTransaction(final ActorSystem actorSystem,
            final String shardName, final short transactionVersion) {
        ActorRef actorRef = actorSystem.actorOf(Props.create(DoNothingActor.class));
        log.info("Created mock shard actor {}", actorRef);

        doReturn(actorSystem.actorSelection(actorRef.path()))
                .when(mockActorContext).actorSelection(actorRef.path().toString());

        doReturn(primaryShardInfoReply(actorSystem, actorRef, transactionVersion))
                .when(mockActorContext).findPrimaryShardAsync(eq(shardName));

        return actorRef;
    }

    protected Future<PrimaryShardInfo> primaryShardInfoReply(final ActorSystem actorSystem, final ActorRef actorRef) {
        return primaryShardInfoReply(actorSystem, actorRef, DataStoreVersions.CURRENT_VERSION);
    }

    protected Future<PrimaryShardInfo> primaryShardInfoReply(final ActorSystem actorSystem, final ActorRef actorRef,
            final short transactionVersion) {
        return Futures.successful(new PrimaryShardInfo(actorSystem.actorSelection(actorRef.path()),
                transactionVersion));
    }

    protected ActorRef setupActorContextWithInitialCreateTransaction(final ActorSystem actorSystem,
            final TransactionType type, final short transactionVersion, final String shardName) {
        ActorRef shardActorRef = setupActorContextWithoutInitialCreateTransaction(actorSystem, shardName,
                transactionVersion);

        return setupActorContextWithInitialCreateTransaction(actorSystem, type, transactionVersion,
                memberName, shardActorRef);
    }

    protected ActorRef setupActorContextWithInitialCreateTransaction(final ActorSystem actorSystem,
            final TransactionType type, final short transactionVersion, final String prefix,
            final ActorRef shardActorRef) {

        ActorRef txActorRef;
        if (type == TransactionType.WRITE_ONLY
                && dataStoreContextBuilder.build().isWriteOnlyTransactionOptimizationsEnabled()) {
            txActorRef = shardActorRef;
        } else {
            txActorRef = actorSystem.actorOf(Props.create(DoNothingActor.class));
            log.info("Created mock shard Tx actor {}", txActorRef);

            doReturn(actorSystem.actorSelection(txActorRef.path()))
                .when(mockActorContext).actorSelection(txActorRef.path().toString());

            doReturn(Futures.successful(createTransactionReply(txActorRef, transactionVersion))).when(mockActorContext)
                .executeOperationAsync(eq(actorSystem.actorSelection(shardActorRef.path())),
                        eqCreateTransaction(prefix, type), any(Timeout.class));
        }

        return txActorRef;
    }

    protected ActorRef setupActorContextWithInitialCreateTransaction(final ActorSystem actorSystem,
            final TransactionType type) {
        return setupActorContextWithInitialCreateTransaction(actorSystem, type, DataStoreVersions.CURRENT_VERSION,
                DefaultShardStrategy.DEFAULT_SHARD);
    }

    protected ActorRef setupActorContextWithInitialCreateTransaction(final ActorSystem actorSystem,
            final TransactionType type,
            final String shardName) {
        return setupActorContextWithInitialCreateTransaction(actorSystem, type, DataStoreVersions.CURRENT_VERSION,
                shardName);
    }

    @SuppressWarnings({"checkstyle:avoidHidingCauseException", "checkstyle:IllegalThrows"})
    protected void propagateReadFailedExceptionCause(final FluentFuture<?> future) throws Throwable {
        try {
            future.get(5, TimeUnit.SECONDS);
            fail("Expected ReadFailedException");
        } catch (ExecutionException e) {
            final Throwable cause = e.getCause();
            assertTrue("Unexpected cause: " + cause.getClass(), cause instanceof ReadFailedException);
            throw Throwables.getRootCause(cause);
        }
    }

    protected List<BatchedModifications> captureBatchedModifications(final ActorRef actorRef) {
        ArgumentCaptor<BatchedModifications> batchedModificationsCaptor =
                ArgumentCaptor.forClass(BatchedModifications.class);
        verify(mockActorContext, Mockito.atLeastOnce()).executeOperationAsync(
                eq(actorSelection(actorRef)), batchedModificationsCaptor.capture(), any(Timeout.class));

        List<BatchedModifications> batchedModifications = filterCaptured(
                batchedModificationsCaptor, BatchedModifications.class);
        return batchedModifications;
    }

    protected <T> List<T> filterCaptured(final ArgumentCaptor<T> captor, final Class<T> type) {
        List<T> captured = new ArrayList<>();
        for (T c: captor.getAllValues()) {
            if (type.isInstance(c)) {
                captured.add(c);
            }
        }

        return captured;
    }

    protected void verifyOneBatchedModification(final ActorRef actorRef, final Modification expected,
            final boolean expIsReady) {
        List<BatchedModifications> batchedModifications = captureBatchedModifications(actorRef);
        assertEquals("Captured BatchedModifications count", 1, batchedModifications.size());

        verifyBatchedModifications(batchedModifications.get(0), expIsReady, expIsReady, expected);
    }

    protected void verifyBatchedModifications(final Object message, final boolean expIsReady,
            final Modification... expected) {
        verifyBatchedModifications(message, expIsReady, false, expected);
    }

    protected void verifyBatchedModifications(final Object message, final boolean expIsReady,
            final boolean expIsDoCommitOnReady, final Modification... expected) {
        assertEquals("Message type", BatchedModifications.class, message.getClass());
        BatchedModifications batchedModifications = (BatchedModifications)message;
        assertEquals("BatchedModifications size", expected.length, batchedModifications.getModifications().size());
        assertEquals("isReady", expIsReady, batchedModifications.isReady());
        assertEquals("isDoCommitOnReady", expIsDoCommitOnReady, batchedModifications.isDoCommitOnReady());
        for (int i = 0; i < batchedModifications.getModifications().size(); i++) {
            Modification actual = batchedModifications.getModifications().get(i);
            assertEquals("Modification type", expected[i].getClass(), actual.getClass());
            assertEquals("getPath", ((AbstractModification)expected[i]).getPath(),
                    ((AbstractModification)actual).getPath());
            if (actual instanceof WriteModification) {
                assertEquals("getData", ((WriteModification)expected[i]).getData(),
                        ((WriteModification)actual).getData());
            }
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    protected void verifyCohortFutures(final AbstractThreePhaseCommitCohort<?> proxy,
            final Object... expReplies) {
        assertEquals("getReadyOperationFutures size", expReplies.length,
                proxy.getCohortFutures().size());

        List<Object> futureResults = new ArrayList<>();
        for (Future<?> future : proxy.getCohortFutures()) {
            assertNotNull("Ready operation Future is null", future);
            try {
                futureResults.add(Await.result(future, FiniteDuration.create(5, TimeUnit.SECONDS)));
            } catch (Exception e) {
                futureResults.add(e);
            }
        }

        for (Object expReply : expReplies) {
            boolean found = false;
            Iterator<?> iter = futureResults.iterator();
            while (iter.hasNext()) {
                Object actual = iter.next();
                if (CommitTransactionReply.isSerializedType(expReply)
                        && CommitTransactionReply.isSerializedType(actual)
                        || expReply instanceof ActorSelection && Objects.equals(expReply, actual)) {
                    found = true;
                } else if (expReply instanceof Class && ((Class<?>) expReply).isInstance(actual)) {
                    found = true;
                }

                if (found) {
                    iter.remove();
                    break;
                }
            }

            if (!found) {
                fail(String.format("No cohort Future response found for %s. Actual: %s", expReply, futureResults));
            }
        }
    }
}
