package org.opendaylight.controller.cluster.datastore;

import static org.mockito.MockitoAnnotations.initMocks;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.AddressFromURIString;
import akka.actor.Props;
import akka.actor.Status.Success;
import akka.cluster.Cluster;
import akka.dispatch.Dispatchers;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import com.google.common.collect.Lists;
import com.typesafe.config.ConfigFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.Shard.Builder;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.config.ConfigurationImpl;
import org.opendaylight.controller.cluster.datastore.config.EmptyModuleShardConfigProvider;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardManagerIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.AddPrefixShardReplica;
import org.opendaylight.controller.cluster.datastore.messages.CreatePrefixedShard;
import org.opendaylight.controller.cluster.datastore.messages.FindLocalShard;
import org.opendaylight.controller.cluster.datastore.messages.FindPrimary;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardFound;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardNotFound;
import org.opendaylight.controller.cluster.datastore.messages.RemotePrimaryShardFound;
import org.opendaylight.controller.cluster.datastore.messages.UpdateSchemaContext;
import org.opendaylight.controller.cluster.datastore.shardmanager.ShardManagerTest.TestShardManager;
import org.opendaylight.controller.cluster.datastore.utils.ClusterUtils;
import org.opendaylight.controller.cluster.datastore.utils.MockConfiguration;
import org.opendaylight.controller.cluster.raft.TestActorFactory;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.raft.utils.InMemorySnapshotStore;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrefixShardCreationTest extends AbstractActorTest {

    private static final Logger LOG = LoggerFactory.getLogger(PrefixShardCreationTest.class);

    private static final DOMDataTreeIdentifier TEST_ID = new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, TestModel.TEST_PATH);

    private static final MemberName MEMBER_1 = MemberName.forName("member-1");
    private static final MemberName MEMBER_2 = MemberName.forName("member-2");
    private static final MemberName MEMBER_3 = MemberName.forName("member-3");

    private static int ID_COUNTER = 1;

    @Mock
    CountDownLatch ready;


    private final String shardMrgIDSuffix = "config" + ID_COUNTER++;
    private final String shardMgrID = ShardManagerIdentifier.builder().type(shardMrgIDSuffix).build().toString();

    private static TestActorRef<MessageCollectorActor> mockShardActor;

    private static ShardIdentifier mockShardName;

    private final TestActorFactory actorFactory = new TestActorFactory(getSystem());

    private final Collection<ActorSystem> actorSystems = new ArrayList<>();



    private final DatastoreContext.Builder datastoreContextBuilder = DatastoreContext.newBuilder().
            dataStoreName(shardMrgIDSuffix).shardInitializationTimeout(600, TimeUnit.MILLISECONDS)
            .shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(6);

    private TestShardManager.Builder newTestShardMgrBuilder() {
        return TestShardManager.builder(datastoreContextBuilder);
    }

    private TestShardManager.Builder newTestShardMgrBuilder(final Configuration config) {
        return TestShardManager.builder(datastoreContextBuilder).configuration(config);
    }

    private Props newShardMgrProps(final Configuration config) {
        return newTestShardMgrBuilder(config).waitTillReadyCountdownLatch(ready).props();
    }

    private Props newShardMgrProps() {
        return newShardMgrProps(new MockConfiguration());
    }

    private TestShardManager newTestShardManager() {
        return newTestShardManager(newShardMgrProps());
    }

    private TestShardManager newTestShardManager(final Props props) {
        final TestActorRef<TestShardManager> shardManagerActor = actorFactory.createTestActor(props);
        final TestShardManager shardManager = shardManagerActor.underlyingActor();
        shardManager.waitForRecoveryComplete();
        return shardManager;
    }


    private TestShardManager.Builder newTestShardMgrBuilderWithMockShardActor() {
        return newTestShardMgrBuilderWithMockShardActor(mockShardActor);
    }

    private TestShardManager.Builder newTestShardMgrBuilderWithMockShardActor(final ActorRef shardActor) {
        return TestShardManager.builder(datastoreContextBuilder).waitTillReadyCountdownLatch(ready).shardActor(shardActor);
    }


    @Before
    public void setUp() throws Exception {
        initMocks(this);

        InMemoryJournal.clear();
        InMemorySnapshotStore.clear();

        if(mockShardActor == null) {
            mockShardName = ShardIdentifier.create(Shard.DEFAULT_NAME, MEMBER_1, "config");
            mockShardActor = TestActorRef.create(getSystem(), Props.create(MessageCollectorActor.class),
                    mockShardName.toString());
        }

        mockShardActor.underlyingActor().clear();
    }

    @After
    public void tearDown() {
        InMemoryJournal.clear();
        InMemorySnapshotStore.clear();

        for(final ActorSystem system: actorSystems) {
            JavaTestKit.shutdownActorSystem(system, null, Boolean.TRUE);
        }

        actorFactory.close();
    }

    @Test
    public void testPrefixShardCreation() throws Exception {

        LOG.info("testOnCreateShard starting");
        new JavaTestKit(getSystem()) {{
            datastoreContextBuilder.shardInitializationTimeout(1, TimeUnit.MINUTES).persistent(true);

            final ActorRef shardManager = actorFactory.createActor(newShardMgrProps(
                    new ConfigurationImpl(new EmptyModuleShardConfigProvider())));

            final SchemaContext schemaContext = TestModel.createTestContext();
            shardManager.tell(new UpdateSchemaContext(schemaContext), ActorRef.noSender());

            shardManager.tell(new FindLocalShard(ClusterUtils.getCleanShardName(TestModel.TEST_PATH), true), getRef());
            expectMsgClass(duration("5 seconds"), LocalShardNotFound.class);

            final Builder builder = Shard.builder();

            final CreatePrefixedShard createPrefixedShard = new CreatePrefixedShard(new PrefixShardConfiguration(TEST_ID, PrefixShardStrategy.NAME, Collections.singletonList(MEMBER_1)),
                    datastoreContextBuilder.build(), builder);

            shardManager.tell(createPrefixedShard, getRef());
            expectMsgClass(duration("5 seconds"), Success.class);

            shardManager.tell(new FindLocalShard(ClusterUtils.getCleanShardName(TestModel.TEST_PATH), true), getRef());
            expectMsgClass(duration("5 seconds"), LocalShardFound.class);

        }};

    }

    @Test
    public void testPrefixShardReplicas() throws Exception {
        LOG.info("testPrefixShardReplicas starting");
        final String shardManagerID = ShardManagerIdentifier.builder().type(shardMrgIDSuffix).build().toString();

        // Create ACtorSystem for member-1
        final ActorSystem system1 = newActorSystem("Member1");
        Cluster.get(system1).join(AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558"));

        final TestActorRef<TestShardManager> shardManager1 = TestActorRef.create(system1,
                newTestShardMgrBuilder(new ConfigurationImpl(new EmptyModuleShardConfigProvider()))
                        .waitTillReadyCountdownLatch(ready)
                        .cluster(new ClusterWrapperImpl(system1))
                        .props().withDispatcher(Dispatchers.DefaultDispatcherId()),
                shardManagerID);

        // Create an ActorSystem ShardManager actor for member-2.

        final ActorSystem system2 = newActorSystem("Member2");

        Cluster.get(system2).join(AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558"));

        final TestActorRef<TestShardManager> shardManager2 = TestActorRef.create(system2,
                newTestShardMgrBuilder()
                        .configuration(new ConfigurationImpl(new EmptyModuleShardConfigProvider()))
                        .waitTillReadyCountdownLatch(ready)
                        .cluster(new ClusterWrapperImpl(system2)).props().withDispatcher(
                        Dispatchers.DefaultDispatcherId()),
                shardManagerID);

        final JavaTestKit kit2 = new JavaTestKit(system2);

        new JavaTestKit(system1) {{

            shardManager1.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
            shardManager2.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

            // check shard does not exist
            shardManager1.tell(new FindLocalShard(ClusterUtils.getCleanShardName(TestModel.TEST_PATH), true), getRef());
            expectMsgClass(duration("5 seconds"), LocalShardNotFound.class);

            shardManager2.tell(new FindLocalShard(ClusterUtils.getCleanShardName(TestModel.TEST_PATH), true), kit2.getRef());
            kit2.expectMsgClass(duration("5 seconds"), LocalShardNotFound.class);

            // create shard on node1
            final Builder builder = Shard.builder();

            final CreatePrefixedShard createPrefixedShard = new CreatePrefixedShard(new PrefixShardConfiguration(TEST_ID, PrefixShardStrategy.NAME, Lists.newArrayList(MEMBER_1, MEMBER_2)),
                    datastoreContextBuilder.build(), builder);

            shardManager1.tell(createPrefixedShard, getRef());
            expectMsgClass(duration("5 seconds"), Success.class);

            shardManager1.underlyingActor().waitForMemberUp();

            LOG.info("changed leader state");

            // check node2 cannot find it locally
            shardManager1.tell(new FindLocalShard(ClusterUtils.getCleanShardName(TestModel.TEST_PATH), true), getRef());
            expectMsgClass(duration("5 seconds"), LocalShardFound.class);

            shardManager2.tell(new FindLocalShard(ClusterUtils.getCleanShardName(TestModel.TEST_PATH), true), kit2.getRef());
            kit2.expectMsgClass(duration("5 seconds"), LocalShardNotFound.class);

            // but can remotely
            shardManager2.tell(new FindPrimary(ClusterUtils.getCleanShardName(TestModel.TEST_PATH), true), kit2.getRef());
            kit2.expectMsgClass(duration("5 seconds"), RemotePrimaryShardFound.class);

            // add replica and verify if succesful
            shardManager2.tell(new AddPrefixShardReplica(TEST_ID.getRootIdentifier()), kit2.getRef());
            kit2.expectMsgClass(duration("5 seconds"), Success.class);

        }};

    }

    private ActorSystem newActorSystem(final String config) {
        final ActorSystem system = ActorSystem.create("cluster-test", ConfigFactory.load().getConfig(config));
        actorSystems.add(system);
        return system;
    }


    private ActorRef newMockShardActor(final ActorSystem system, final String shardName, final String memberName) {
        final String name = ShardIdentifier.create(shardName, MemberName.forName(memberName), "config").toString();
        if(system == getSystem()) {
            return actorFactory.createTestActor(Props.create(MessageCollectorActor.class), name);
        }

        return TestActorRef.create(system, Props.create(MessageCollectorActor.class), name);
    }

}