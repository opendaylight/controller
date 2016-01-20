/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.compat;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.opendaylight.controller.cluster.datastore.TransactionType.READ_WRITE;
import akka.actor.ActorRef;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.AbstractTransactionProxyTest;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.cluster.datastore.TransactionProxy;
import org.opendaylight.controller.cluster.datastore.shardstrategy.DefaultShardStrategy;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages;

/**
 * TransactionProxy unit tests for backwards compatibility with pre-Boron versions.
 *
 * @author Thomas Pantelis
 */
public class PreBoronTransactionProxyTest extends AbstractTransactionProxyTest {

    @Test
    public void testClose() throws Exception{
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), READ_WRITE,
                DataStoreVersions.LITHIUM_VERSION, DefaultShardStrategy.DEFAULT_SHARD);

        doReturn(readDataReply(null)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqSerializedReadData());

        TransactionProxy transactionProxy = new TransactionProxy(mockComponentFactory, READ_WRITE);

        transactionProxy.read(TestModel.TEST_PATH);

        transactionProxy.close();

        verify(mockActorContext).sendOperationAsync(
                eq(actorSelection(actorRef)), isA(ShardTransactionMessages.CloseTransaction.class));
    }
}
