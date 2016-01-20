/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.compat;

import static org.junit.Assert.assertTrue;
import akka.actor.ActorRef;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.AbstractShardTest;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.cluster.datastore.ShardTestKit;
import org.opendaylight.controller.cluster.datastore.TransactionType;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.UpdateSchemaContext;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages;

/**
 * Shard unit tests for backwards compatibility with pre-Boron versions.
 *
 * @author Thomas Pantelis
 */
public class PreBoronShardTest extends AbstractShardTest {

    @Test
    public void testCreateTransaction(){
        new ShardTestKit(getSystem()) {{
            final ActorRef shard = actorFactory.createActor(newShardProps(), "testCreateTransaction");

            waitUntilLeader(shard);

            shard.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

            shard.tell(new CreateTransaction("txn-1", TransactionType.READ_ONLY.ordinal(), null,
                    DataStoreVersions.LITHIUM_VERSION).toSerializable(), getRef());

            ShardTransactionMessages.CreateTransactionReply reply =
                    expectMsgClass(ShardTransactionMessages.CreateTransactionReply.class);

            final String path = CreateTransactionReply.fromSerializable(reply).getTransactionPath().toString();
            assertTrue("Unexpected transaction path " + path,
                    path.contains("akka://test/user/testCreateTransaction/shard-txn-1"));
        }};
    }
}
