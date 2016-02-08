/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.compat;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.opendaylight.controller.cluster.datastore.TransactionType.READ_WRITE;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.util.Timeout;
import com.google.common.base.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.opendaylight.controller.cluster.datastore.AbstractTransactionProxyTest;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.cluster.datastore.TransactionProxy;
import org.opendaylight.controller.cluster.datastore.TransactionType;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransaction;
import org.opendaylight.controller.cluster.datastore.messages.DataExists;
import org.opendaylight.controller.cluster.datastore.messages.ReadData;
import org.opendaylight.controller.cluster.datastore.shardstrategy.DefaultShardStrategy;
import org.opendaylight.controller.cluster.raft.utils.DoNothingActor;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

/**
 * TransactionProxy unit tests for backwards compatibility with pre-Boron versions.
 *
 * @author Thomas Pantelis
 */
@SuppressWarnings("resource")
public class PreBoronTransactionProxyTest extends AbstractTransactionProxyTest {

    private CreateTransaction eqLegacyCreateTransaction(final TransactionType type) {
        ArgumentMatcher<CreateTransaction> matcher = new ArgumentMatcher<CreateTransaction>() {
            @Override
            public boolean matches(Object argument) {
                if(ShardTransactionMessages.CreateTransaction.class.equals(argument.getClass())) {
                    CreateTransaction obj = CreateTransaction.fromSerializable(argument);
                    return obj.getTransactionId().startsWith(memberName) &&
                            obj.getTransactionType() == type.ordinal();
                }

                return false;
            }
        };

        return argThat(matcher);
    }

    private static ShardTransactionMessages.CreateTransactionReply legacyCreateTransactionReply(ActorRef actorRef,
            int transactionVersion){
        return ShardTransactionMessages.CreateTransactionReply.newBuilder()
            .setTransactionActorPath(actorRef.path().toString())
            .setTransactionId("txn-1")
            .setMessageVersion(transactionVersion)
            .build();
    }

    private static ReadData eqLegacySerializedReadData(final YangInstanceIdentifier path) {
        ArgumentMatcher<ReadData> matcher = new ArgumentMatcher<ReadData>() {
            @Override
            public boolean matches(Object argument) {
                return ShardTransactionMessages.ReadData.class.equals(argument.getClass()) &&
                       ReadData.fromSerializable(argument).getPath().equals(path);
            }
        };

        return argThat(matcher);
    }

    private static DataExists eqLegacySerializedDataExists() {
        ArgumentMatcher<DataExists> matcher = new ArgumentMatcher<DataExists>() {
            @Override
            public boolean matches(Object argument) {
                return ShardTransactionMessages.DataExists.class.equals(argument.getClass()) &&
                       DataExists.fromSerializable(argument).getPath().equals(TestModel.TEST_PATH);
            }
        };

        return argThat(matcher);
    }

    private ActorRef setupPreBoronActorContextWithInitialCreateTransaction(ActorSystem actorSystem,
            TransactionType type) {
        ActorRef shardActorRef = setupActorContextWithoutInitialCreateTransaction(actorSystem,
                DefaultShardStrategy.DEFAULT_SHARD, DataStoreVersions.LITHIUM_VERSION);

        ActorRef txActorRef;
        if(type == TransactionType.WRITE_ONLY) {
            txActorRef = shardActorRef;
        } else {
            txActorRef = actorSystem.actorOf(Props.create(DoNothingActor.class));
            doReturn(actorSystem.actorSelection(txActorRef.path())).
                when(mockActorContext).actorSelection(txActorRef.path().toString());

            doReturn(Futures.successful(legacyCreateTransactionReply(txActorRef, DataStoreVersions.LITHIUM_VERSION)))
                .when(mockActorContext).executeOperationAsync(eq(actorSystem.actorSelection(shardActorRef.path())),
                        eqLegacyCreateTransaction(type), any(Timeout.class));
        }

        return txActorRef;
    }

    @Test
    public void testClose() throws Exception{
        ActorRef actorRef = setupPreBoronActorContextWithInitialCreateTransaction(getSystem(), READ_WRITE);

        expectBatchedModifications(actorRef, 1);

        TransactionProxy transactionProxy = new TransactionProxy(mockComponentFactory, READ_WRITE);

        transactionProxy.write(TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));

        transactionProxy.close();

        verify(mockActorContext).sendOperationAsync(
                eq(actorSelection(actorRef)), isA(ShardTransactionMessages.CloseTransaction.class));
    }

    @Test
    public void testRead() throws Exception{
        ActorRef actorRef = setupPreBoronActorContextWithInitialCreateTransaction(getSystem(), READ_WRITE);

        NormalizedNode<?, ?> expectedNode = ImmutableNodes.containerNode(TestModel.TEST_QNAME);
        doReturn(readDataReply(expectedNode)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqLegacySerializedReadData(TestModel.TEST_PATH), any(Timeout.class));

        TransactionProxy transactionProxy = new TransactionProxy(mockComponentFactory, READ_WRITE);

        Optional<NormalizedNode<?, ?>> readOptional = transactionProxy.read(
                TestModel.TEST_PATH).get(5, TimeUnit.SECONDS);

        assertEquals("NormalizedNode isPresent", true, readOptional.isPresent());
        assertEquals("Response NormalizedNode", expectedNode, readOptional.get());
    }

    @Test
    public void testExists() throws Exception{
        ActorRef actorRef = setupPreBoronActorContextWithInitialCreateTransaction(getSystem(), READ_WRITE);

        doReturn(dataExistsReply(true)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqLegacySerializedDataExists(), any(Timeout.class));

        TransactionProxy transactionProxy = new TransactionProxy(mockComponentFactory, READ_WRITE);

        Boolean exists = transactionProxy.exists(TestModel.TEST_PATH).checkedGet(5, TimeUnit.SECONDS);
        assertEquals("Exists response", true, exists);
    }
}
