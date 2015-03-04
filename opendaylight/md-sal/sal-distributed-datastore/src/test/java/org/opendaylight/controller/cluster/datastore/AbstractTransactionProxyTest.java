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
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.CheckedFuture;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
import org.opendaylight.controller.cluster.datastore.TransactionProxy.TransactionType;
import org.opendaylight.controller.cluster.datastore.TransactionProxyTest.TestException;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModifications;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModificationsReply;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransaction;
import org.opendaylight.controller.cluster.datastore.messages.DataExists;
import org.opendaylight.controller.cluster.datastore.messages.DataExistsReply;
import org.opendaylight.controller.cluster.datastore.messages.ReadData;
import org.opendaylight.controller.cluster.datastore.messages.ReadDataReply;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransaction;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransactionReply;
import org.opendaylight.controller.cluster.datastore.modification.AbstractModification;
import org.opendaylight.controller.cluster.datastore.modification.Modification;
import org.opendaylight.controller.cluster.datastore.modification.WriteModification;
import org.opendaylight.controller.cluster.datastore.shardstrategy.DefaultShardStrategy;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategyFactory;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.cluster.datastore.utils.DoNothingActor;
import org.opendaylight.controller.cluster.datastore.utils.MockConfiguration;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages.CreateTransactionReply;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

/**
 * Abstract base class for TransactionProxy unit tests.
 *
 * @author Thomas Pantelis
 */
public abstract class AbstractTransactionProxyTest {
    private static ActorSystem system;

    private final Configuration configuration = new MockConfiguration();

    @Mock
    protected ActorContext mockActorContext;

    private SchemaContext schemaContext;

    @Mock
    private ClusterWrapper mockClusterWrapper;

    protected final String memberName = "mock-member";

    protected final Builder dataStoreContextBuilder = DatastoreContext.newBuilder().operationTimeoutInSeconds(2).
            shardBatchedModificationCount(1);

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
        doReturn(schemaContext).when(mockActorContext).getSchemaContext();
        doReturn(mockClusterWrapper).when(mockActorContext).getClusterWrapper();
        doReturn(mockClusterWrapper).when(mockActorContext).getClusterWrapper();
        doReturn(dataStoreContextBuilder.build()).when(mockActorContext).getDatastoreContext();
        doReturn(10).when(mockActorContext).getTransactionOutstandingOperationLimit();

        ShardStrategyFactory.setConfiguration(configuration);
    }

    protected ActorSystem getSystem() {
        return system;
    }

    protected CreateTransaction eqCreateTransaction(final String memberName,
            final TransactionType type) {
        ArgumentMatcher<CreateTransaction> matcher = new ArgumentMatcher<CreateTransaction>() {
            @Override
            public boolean matches(Object argument) {
                if(CreateTransaction.SERIALIZABLE_CLASS.equals(argument.getClass())) {
                    CreateTransaction obj = CreateTransaction.fromSerializable(argument);
                    return obj.getTransactionId().startsWith(memberName) &&
                            obj.getTransactionType() == type.ordinal();
                }

                return false;
            }
        };

        return argThat(matcher);
    }

    protected DataExists eqSerializedDataExists() {
        ArgumentMatcher<DataExists> matcher = new ArgumentMatcher<DataExists>() {
            @Override
            public boolean matches(Object argument) {
                return DataExists.SERIALIZABLE_CLASS.equals(argument.getClass()) &&
                       DataExists.fromSerializable(argument).getPath().equals(TestModel.TEST_PATH);
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

    protected ReadData eqSerializedReadData() {
        return eqSerializedReadData(TestModel.TEST_PATH);
    }

    protected ReadData eqSerializedReadData(final YangInstanceIdentifier path) {
        ArgumentMatcher<ReadData> matcher = new ArgumentMatcher<ReadData>() {
            @Override
            public boolean matches(Object argument) {
                return ReadData.SERIALIZABLE_CLASS.equals(argument.getClass()) &&
                       ReadData.fromSerializable(argument).getPath().equals(path);
            }
        };

        return argThat(matcher);
    }

    protected ReadData eqReadData() {
        ArgumentMatcher<ReadData> matcher = new ArgumentMatcher<ReadData>() {
            @Override
            public boolean matches(Object argument) {
                return (argument instanceof ReadData) &&
                    ((ReadData)argument).getPath().equals(TestModel.TEST_PATH);
            }
        };

        return argThat(matcher);
    }

    protected Future<Object> readySerializedTxReply(String path) {
        return Futures.successful((Object)new ReadyTransactionReply(path).toSerializable());
    }

    protected Future<Object> readyTxReply(String path) {
        return Futures.successful((Object)new ReadyTransactionReply(path));
    }

    protected Future<Object> readSerializedDataReply(NormalizedNode<?, ?> data,
            short transactionVersion) {
        return Futures.successful(new ReadDataReply(data, transactionVersion).toSerializable());
    }

    protected Future<Object> readSerializedDataReply(NormalizedNode<?, ?> data) {
        return readSerializedDataReply(data, DataStoreVersions.CURRENT_VERSION);
    }

    protected Future<ReadDataReply> readDataReply(NormalizedNode<?, ?> data) {
        return Futures.successful(new ReadDataReply(data, DataStoreVersions.CURRENT_VERSION));
    }

    protected Future<Object> dataExistsSerializedReply(boolean exists) {
        return Futures.successful(new DataExistsReply(exists).toSerializable());
    }

    protected Future<DataExistsReply> dataExistsReply(boolean exists) {
        return Futures.successful(new DataExistsReply(exists));
    }

    protected Future<BatchedModificationsReply> batchedModificationsReply(int count) {
        return Futures.successful(new BatchedModificationsReply(count));
    }

    protected Future<Object> incompleteFuture(){
        return mock(Future.class);
    }

    protected ActorSelection actorSelection(ActorRef actorRef) {
        return getSystem().actorSelection(actorRef.path());
    }

    protected void expectBatchedModifications(ActorRef actorRef, int count) {
        doReturn(batchedModificationsReply(count)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), isA(BatchedModifications.class));
    }

    protected void expectBatchedModifications(int count) {
        doReturn(batchedModificationsReply(count)).when(mockActorContext).executeOperationAsync(
                any(ActorSelection.class), isA(BatchedModifications.class));
    }

    protected void expectIncompleteBatchedModifications() {
        doReturn(incompleteFuture()).when(mockActorContext).executeOperationAsync(
                any(ActorSelection.class), isA(BatchedModifications.class));
    }

    protected void expectReadyTransaction(ActorRef actorRef) {
        doReturn(readySerializedTxReply(actorRef.path().toString())).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), isA(ReadyTransaction.SERIALIZABLE_CLASS));
    }

    protected void expectFailedBatchedModifications(ActorRef actorRef) {
        doReturn(Futures.failed(new TestException())).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), isA(BatchedModifications.class));
    }

    protected CreateTransactionReply createTransactionReply(ActorRef actorRef, int transactionVersion){
        return CreateTransactionReply.newBuilder()
            .setTransactionActorPath(actorRef.path().toString())
            .setTransactionId("txn-1")
            .setMessageVersion(transactionVersion)
            .build();
    }

    protected ActorRef setupActorContextWithoutInitialCreateTransaction(ActorSystem actorSystem) {
        ActorRef actorRef = actorSystem.actorOf(Props.create(DoNothingActor.class));
        doReturn(actorSystem.actorSelection(actorRef.path())).
                when(mockActorContext).actorSelection(actorRef.path().toString());

        doReturn(Futures.successful(actorSystem.actorSelection(actorRef.path()))).
                when(mockActorContext).findPrimaryShardAsync(eq(DefaultShardStrategy.DEFAULT_SHARD));

        doReturn(false).when(mockActorContext).isPathLocal(actorRef.path().toString());

        doReturn(10).when(mockActorContext).getTransactionOutstandingOperationLimit();

        return actorRef;
    }

    protected ActorRef setupActorContextWithInitialCreateTransaction(ActorSystem actorSystem,
            TransactionType type, int transactionVersion) {
        ActorRef actorRef = setupActorContextWithoutInitialCreateTransaction(actorSystem);

        doReturn(Futures.successful(createTransactionReply(actorRef, transactionVersion))).when(mockActorContext).
                executeOperationAsync(eq(actorSystem.actorSelection(actorRef.path())),
                        eqCreateTransaction(memberName, type));

        return actorRef;
    }

    protected ActorRef setupActorContextWithInitialCreateTransaction(ActorSystem actorSystem, TransactionType type) {
        return setupActorContextWithInitialCreateTransaction(actorSystem, type, DataStoreVersions.CURRENT_VERSION);
    }


    protected void propagateReadFailedExceptionCause(CheckedFuture<?, ReadFailedException> future)
            throws Throwable {

        try {
            future.checkedGet(5, TimeUnit.SECONDS);
            fail("Expected ReadFailedException");
        } catch(ReadFailedException e) {
            throw e.getCause();
        }
    }

    protected List<BatchedModifications> captureBatchedModifications(ActorRef actorRef) {
        ArgumentCaptor<BatchedModifications> batchedModificationsCaptor =
                ArgumentCaptor.forClass(BatchedModifications.class);
        verify(mockActorContext, Mockito.atLeastOnce()).executeOperationAsync(
                eq(actorSelection(actorRef)), batchedModificationsCaptor.capture());

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

    protected void verifyOneBatchedModification(ActorRef actorRef, Modification expected) {
        List<BatchedModifications> batchedModifications = captureBatchedModifications(actorRef);
        assertEquals("Captured BatchedModifications count", 1, batchedModifications.size());

        verifyBatchedModifications(batchedModifications.get(0), expected);
    }

    protected void verifyBatchedModifications(Object message, Modification... expected) {
        assertEquals("Message type", BatchedModifications.class, message.getClass());
        BatchedModifications batchedModifications = (BatchedModifications)message;
        assertEquals("BatchedModifications size", expected.length, batchedModifications.getModifications().size());
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

    protected void verifyCohortFutures(ThreePhaseCommitCohortProxy proxy,
            Object... expReplies) throws Exception {
            assertEquals("getReadyOperationFutures size", expReplies.length,
                    proxy.getCohortFutures().size());

            int i = 0;
            for( Future<ActorSelection> future: proxy.getCohortFutures()) {
                assertNotNull("Ready operation Future is null", future);

                Object expReply = expReplies[i++];
                if(expReply instanceof ActorSelection) {
                    ActorSelection actual = Await.result(future, Duration.create(5, TimeUnit.SECONDS));
                    assertEquals("Cohort actor path", expReply, actual);
                } else {
                    // Expecting exception.
                    try {
                        Await.result(future, Duration.create(5, TimeUnit.SECONDS));
                        fail("Expected exception from ready operation Future");
                    } catch(Exception e) {
                        // Expected
                    }
                }
            }
        }
}
