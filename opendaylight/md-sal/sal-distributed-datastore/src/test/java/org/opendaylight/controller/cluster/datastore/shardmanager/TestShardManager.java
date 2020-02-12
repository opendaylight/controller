/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.shardmanager;

import akka.actor.ActorRef;
import akka.actor.Props;
import java.util.Map;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.TestShard;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshot;

public class TestShardManager extends ShardManager {
    TestShardManager(AbstractShardManagerCreator<?> builder) {
        super(builder);
    }

    @Override
    public void handleCommand(Object message) throws Exception {
        if (GetLocalShards.INSTANCE.equals(message)) {
            sender().tell(new GetLocalShardsReply(localShards), null);
        } else {
            super.handleCommand(message);
        }
    }

    /**
     * Plug into shard actor creation to replace info with our testing one.
     * @param info shard info.
     * @return actor for replaced shard info.
     */
    @Override
    protected ActorRef newShardActor(ShardInformation info) {
        Map<String, String> peerAddresses = getPeerAddresses(info.getShardName());
        ShardInformation newInfo = new ShardInformation(info.getShardName(),
                info.getShardId(), peerAddresses,
                info.getDatastoreContext(),
                TestShard.builder()
                        .restoreFromSnapshot(info.getBuilder().getRestoreFromSnapshot()),
                peerAddressResolver);
        newInfo.setSchemaContext(info.getSchemaContext());
        newInfo.setActiveMember(info.isActiveMember());


        localShards.put(info.getShardName(), info);
        return getContext().actorOf(newInfo.newProps().withDispatcher(shardDispatcherPath),
                info.getShardId().toString());
    }

    @Override
    ShardInformation createShardInfoFor(String shardName, ShardIdentifier shardId,
                                        Map<String, String> peerAddresses,
                                        DatastoreContext datastoreContext,
                                        Map<String, DatastoreSnapshot.ShardSnapshot> shardSnapshots) {
        return new ShardInformation(shardName, shardId, peerAddresses,
                datastoreContext, TestShard.builder().restoreFromSnapshot(shardSnapshots.get(shardName)),
                peerAddressResolver);
    }

    public static class TestShardManagerCreator extends AbstractShardManagerCreator<TestShardManagerCreator> {
        @Override
        public Props props() {
            verify();
            return Props.create(TestShardManager.class, this);
        }
    }

    public static final class GetLocalShards {
        public static final GetLocalShards INSTANCE = new GetLocalShards();

        private GetLocalShards() {

        }
    }

    public static class GetLocalShardsReply {

        private final Map<String, ShardInformation> localShards;

        public GetLocalShardsReply(Map<String, ShardInformation> localShards) {
            this.localShards = localShards;
        }

        public Map<String, ShardInformation> getLocalShards() {
            return localShards;
        }
    }
}
