/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.compat;

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
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.opendaylight.controller.cluster.datastore.AbstractTransactionProxyTest;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.cluster.datastore.TransactionProxy;
import org.opendaylight.controller.cluster.datastore.TransactionType;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransaction;
import org.opendaylight.controller.cluster.datastore.shardstrategy.DefaultShardStrategy;
import org.opendaylight.controller.cluster.raft.utils.DoNothingActor;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages;
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages.CreateTransactionReply;

/**
 * TransactionProxy unit tests for backwards compatibility with pre-Boron versions.
 *
 * @author Thomas Pantelis
 */
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

    private CreateTransactionReply legacyCreateTransactionReply(ActorRef actorRef, int transactionVersion){
        return CreateTransactionReply.newBuilder()
            .setTransactionActorPath(actorRef.path().toString())
            .setTransactionId("txn-1")
            .setMessageVersion(transactionVersion)
            .build();
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

        doReturn(readDataReply(null)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqSerializedReadData());

        TransactionProxy transactionProxy = new TransactionProxy(mockComponentFactory, READ_WRITE);

        transactionProxy.read(TestModel.TEST_PATH);

        transactionProxy.close();

        verify(mockActorContext).sendOperationAsync(
                eq(actorSelection(actorRef)), isA(ShardTransactionMessages.CloseTransaction.class));
    }
}
