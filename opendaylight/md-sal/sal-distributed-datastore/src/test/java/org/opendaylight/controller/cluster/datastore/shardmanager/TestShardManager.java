/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.shardmanager;

import java.nio.file.Path;
import java.util.Map;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Props;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.TestShard;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshot;

public class TestShardManager extends ShardManager {
    TestShardManager(final Path stateDir, final AbstractShardManagerCreator<?> builder) {
        super(stateDir, builder);
    }

    @Override
    public void handleCommand(final Object message) throws Exception {
        if (GetLocalShards.INSTANCE.equals(message)) {
            getSender().tell(new GetLocalShardsReply(localShards), null);
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
    protected ActorRef newShardActor(final ShardInformation info) {
        final var shardName = info.getShardName();
        final var shardId = info.getShardId();
        final var newInfo = new ShardInformation(stateDir, shardName, shardId, getPeerAddresses(info.getShardName()),
            info.getDatastoreContext(),
            TestShard.builder().restoreFromSnapshot(info.getBuilder().getRestoreFromSnapshot()), peerAddressResolver);
        newInfo.setSchemaContext(info.getSchemaContext());
        newInfo.setActiveMember(info.isActiveMember());

        localShards.put(shardName, info);
        return getContext().actorOf(newInfo.newProps().withDispatcher(shardDispatcherPath), shardId.toString());
    }

    @Override
    ShardInformation createShardInfoFor(final String shardName, final ShardIdentifier shardId,
                                        final Map<String, String> peerAddresses,
                                        final DatastoreContext datastoreContext,
                                        final Map<String, DatastoreSnapshot.ShardSnapshot> shardSnapshots) {
        return new ShardInformation(stateDir, shardName, shardId, peerAddresses,
                datastoreContext, TestShard.builder().restoreFromSnapshot(shardSnapshots.get(shardName)),
                peerAddressResolver);
    }

    public static class TestShardManagerCreator extends AbstractShardManagerCreator<TestShardManagerCreator> {
        @Override
        public Props props(final Path stateDir) {
            verify();
            return Props.create(TestShardManager.class, stateDir, this);
        }
    }

    public static final class GetLocalShards {
        public static final GetLocalShards INSTANCE = new GetLocalShards();

        private GetLocalShards() {

        }
    }

    public static class GetLocalShardsReply {

        private final Map<String, ShardInformation> localShards;

        public GetLocalShardsReply(final Map<String, ShardInformation> localShards) {
            this.localShards = localShards;
        }

        public Map<String, ShardInformation> getLocalShards() {
            return localShards;
        }
    }
}
