/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import com.google.common.util.concurrent.SettableFuture;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.shardmanager.ShardManagerTest.TestShardManager;
import org.opendaylight.controller.cluster.raft.TestActorFactory;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.raft.utils.InMemorySnapshotStore;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;

public class AbstractShardManagerTest extends AbstractClusterRefActorTest {

    protected static final MemberName MEMBER_1 = MemberName.forName("member-1");

    protected static int ID_COUNTER = 1;
    protected static ActorRef mockShardActor;
    protected static ShardIdentifier mockShardName;

    protected final String shardMrgIDSuffix = "config" + ID_COUNTER++;
    protected final TestActorFactory actorFactory = new TestActorFactory(getSystem());
    protected final DatastoreContext.Builder datastoreContextBuilder = DatastoreContext.newBuilder()
            .dataStoreName(shardMrgIDSuffix).shardInitializationTimeout(600, TimeUnit.MILLISECONDS)
            .shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(6);

    protected static SettableFuture<Void> ready;

    protected TestShardManager.Builder newTestShardMgrBuilder() {
        return TestShardManager.builder(datastoreContextBuilder).distributedDataStore(mock(DistributedDataStore.class));
    }

    protected TestShardManager.Builder newTestShardMgrBuilder(final Configuration config) {
        return TestShardManager.builder(datastoreContextBuilder).configuration(config)
                .distributedDataStore(mock(DistributedDataStore.class));
    }

    protected Props newShardMgrProps(final Configuration config) {
        return newTestShardMgrBuilder(config).readinessFuture(ready).props();
    }

    @Before
    public void setUp() {
        initMocks(this);
        ready = SettableFuture.create();

        InMemoryJournal.clear();
        InMemorySnapshotStore.clear();

        if (mockShardActor == null) {
            mockShardName = ShardIdentifier.create(Shard.DEFAULT_NAME, MEMBER_1, "config");
            mockShardActor = getSystem().actorOf(MessageCollectorActor.props(), mockShardName.toString());
        }

        MessageCollectorActor.clearMessages(mockShardActor);
    }

    @After
    public void tearDown() {
        InMemoryJournal.clear();
        InMemorySnapshotStore.clear();

        mockShardActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
        await().atMost(Duration.ofSeconds(10)).until(mockShardActor::isTerminated);
        mockShardActor = null;

        actorFactory.close();
    }
}
