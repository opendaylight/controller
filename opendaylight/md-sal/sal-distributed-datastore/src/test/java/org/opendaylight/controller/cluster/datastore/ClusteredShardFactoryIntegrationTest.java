package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.actor.AddressFromURIString;
import akka.cluster.Cluster;
import akka.testkit.JavaTestKit;
import com.typesafe.config.ConfigFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.cluster.raft.policy.DisableElectionsRaftPolicy;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.mdsal.dom.broker.ShardedDOMDataTree;

public class ClusteredShardFactoryIntegrationTest extends AbstractTest {

    private static final Address MEMBER_1_ADDRESS = AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558");
    private static final Address MEMBER_2_ADDRESS = AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2559");

    private ActorSystem leaderSystem;
    private ActorSystem followerSystem;
    private ActorSystem follower2System;

    private final DatastoreContext.Builder leaderDatastoreContextBuilder =
            DatastoreContext.newBuilder().shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(2);

    private final DatastoreContext.Builder followerDatastoreContextBuilder =
            DatastoreContext.newBuilder().shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(5).
                    customRaftPolicyImplementation(DisableElectionsRaftPolicy.class.getName());

    private DistributedDataStore followerDistributedDataStore;
    private DistributedDataStore leaderDistributedDataStore;
    private IntegrationTestKit followerTestKit;
    private IntegrationTestKit leaderTestKit;

    @Before
    public void setUp() {
        leaderSystem = ActorSystem.create("cluster-test", ConfigFactory.load().getConfig("Member1"));
        Cluster.get(leaderSystem).join(MEMBER_1_ADDRESS);

        followerSystem = ActorSystem.create("cluster-test", ConfigFactory.load().getConfig("Member2"));
        Cluster.get(followerSystem).join(MEMBER_1_ADDRESS);

        follower2System = ActorSystem.create("cluster-test", ConfigFactory.load().getConfig("Member3"));
        Cluster.get(follower2System).join(MEMBER_1_ADDRESS);
    }

    @After
    public void tearDown() {
        if (followerDistributedDataStore != null) {
            leaderDistributedDataStore.close();
        }
        if (leaderDistributedDataStore != null) {
            leaderDistributedDataStore.close();
        }

        JavaTestKit.shutdownActorSystem(leaderSystem);
        JavaTestKit.shutdownActorSystem(followerSystem);
        JavaTestKit.shutdownActorSystem(follower2System);
    }


    private void initDatastores(final String type, final String moduleShardsConfig, final String[] shards) {
        leaderTestKit = new IntegrationTestKit(leaderSystem, leaderDatastoreContextBuilder);

        leaderDistributedDataStore = leaderTestKit.setupDistributedDataStoreWithoutConfig(type, SchemaContextHelper.full());

        followerTestKit = new IntegrationTestKit(followerSystem, followerDatastoreContextBuilder);
        followerDistributedDataStore = followerTestKit.setupDistributedDataStoreWithoutConfig(type, SchemaContextHelper.full());

        leaderTestKit.waitUntilLeader(leaderDistributedDataStore.getActorContext(), shards);
    }


    @Test
    public void testWriteIntoMultipleShards() throws Exception {

        final ClusteredShardFactory clusteredShardFactory = new ClusteredShardFactory(new ShardedDOMDataTree(), Mockito.mock(DistributedDataStore.class), leaderDistributedDataStore, leaderSystem, MEMBER_NAME);


    }
}
