package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.AddressFromURIString;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.dispatch.Dispatchers;
import akka.japi.Creator;
import akka.pattern.Patterns;
import akka.persistence.RecoveryCompleted;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import akka.util.Timeout;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Uninterruptibles;
import com.typesafe.config.ConfigFactory;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.datastore.exceptions.NoShardLeaderException;
import org.opendaylight.controller.cluster.datastore.exceptions.NotInitializedException;
import org.opendaylight.controller.cluster.datastore.exceptions.PrimaryNotFoundException;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardManagerIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.ActorInitialized;
import org.opendaylight.controller.cluster.datastore.messages.FindLocalShard;
import org.opendaylight.controller.cluster.datastore.messages.FindPrimary;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardFound;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardNotFound;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryFound;
import org.opendaylight.controller.cluster.datastore.messages.UpdateSchemaContext;
import org.opendaylight.controller.cluster.datastore.utils.DoNothingActor;
import org.opendaylight.controller.cluster.datastore.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.datastore.utils.MockClusterWrapper;
import org.opendaylight.controller.cluster.datastore.utils.MockConfiguration;
import org.opendaylight.controller.cluster.notifications.RoleChangeNotification;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.base.messages.FollowerInitialSyncUpStatus;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.yang.model.api.ModuleIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

public class ShardManagerTest extends AbstractActorTest {
    private static int ID_COUNTER = 1;

    private final String shardMrgIDSuffix = "config" + ID_COUNTER++;
    private final String shardMgrID = "shard-manager-" + shardMrgIDSuffix;

    @Mock
    private static CountDownLatch ready;

    private static ActorRef mockShardActor;

    private final DatastoreContext.Builder datastoreContextBuilder = DatastoreContext.newBuilder().
            dataStoreType(shardMrgIDSuffix).shardInitializationTimeout(600, TimeUnit.MILLISECONDS);

    private static ActorRef newMockShardActor(ActorSystem system, String shardName, String memberName) {
        String name = new ShardIdentifier(shardName, memberName,"config").toString();
        return system.actorOf(Props.create(DoNothingActor.class), name);
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        InMemoryJournal.clear();

        if(mockShardActor == null) {
            mockShardActor = newMockShardActor(getSystem(), Shard.DEFAULT_NAME, "member-1");
        }
    }

    @After
    public void tearDown() {
        InMemoryJournal.clear();
    }

    private Props newShardMgrProps() {
        return ShardManager.props(new MockClusterWrapper(), new MockConfiguration(),
                datastoreContextBuilder.build(), ready);
    }

    private Props newPropsShardMgrWithMockShardActor() {
        return newPropsShardMgrWithMockShardActor("shardManager", mockShardActor, new MockClusterWrapper(),
                new MockConfiguration());
    }

    private Props newPropsShardMgrWithMockShardActor(final String name, final ActorRef shardActor,
            final ClusterWrapper clusterWrapper, final Configuration config) {
        Creator<ShardManager> creator = new Creator<ShardManager>() {
            private static final long serialVersionUID = 1L;
            @Override
            public ShardManager create() throws Exception {
                return new ForwardingShardManager(clusterWrapper, config, datastoreContextBuilder.build(),
                        ready, name, shardActor);
            }
        };

        return Props.create(new DelegatingShardManagerCreator(creator)).withDispatcher(Dispatchers.DefaultDispatcherId());
    }

    @Test
    public void testOnReceiveFindPrimaryForNonExistentShard() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = getSystem().actorOf(newPropsShardMgrWithMockShardActor());

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

            shardManager.tell(new FindPrimary("non-existent", false), getRef());

            expectMsgClass(duration("5 seconds"), PrimaryNotFoundException.class);
        }};
    }

    @Test
    public void testOnReceiveFindPrimaryForReadyShard() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = getSystem().actorOf(newPropsShardMgrWithMockShardActor());

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
            shardManager.tell(new ActorInitialized(), mockShardActor);
            shardManager.tell(new RoleChangeNotification("member-1-shard-default-" + shardMrgIDSuffix,
                    RaftState.Candidate.name(), RaftState.Leader.name()), mockShardActor);

            shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, false), getRef());

            expectMsgClass(duration("5 seconds"), PrimaryFound.class);
        }};
    }

    @Test
    public void testOnReceiveFindPrimaryForUninitializedShard() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = getSystem().actorOf(newPropsShardMgrWithMockShardActor());

            shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, false), getRef());

            expectMsgClass(duration("5 seconds"), NotInitializedException.class);
        }};
    }

    @Test
    public void testOnReceiveFindPrimaryForInitializedButNotReadyShard() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = getSystem().actorOf(newPropsShardMgrWithMockShardActor());

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
            shardManager.tell(new ActorInitialized(), mockShardActor);

            shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, false), getRef());

            expectMsgClass(duration("5 seconds"), NoShardLeaderException.class);
        }};
    }

    @Test
    public void testOnReceiveFindPrimaryWaitForShardReady() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = getSystem().actorOf(newPropsShardMgrWithMockShardActor());

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

            // We're passing waitUntilInitialized = true to FindPrimary so the response should be
            // delayed until we send ActorInitialized and RoleChangeNotification.
            shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, true), getRef());

            expectNoMsg(FiniteDuration.create(200, TimeUnit.MILLISECONDS));

            shardManager.tell(new ActorInitialized(), mockShardActor);

            expectNoMsg(FiniteDuration.create(200, TimeUnit.MILLISECONDS));

            shardManager.tell(new RoleChangeNotification("member-1-shard-default-" + shardMrgIDSuffix,
                    RaftState.Candidate.name(), RaftState.Leader.name()), mockShardActor);

            expectMsgClass(duration("2 seconds"), PrimaryFound.class);

            expectNoMsg(FiniteDuration.create(200, TimeUnit.MILLISECONDS));
        }};
    }

    @Test
    public void testOnReceiveFindPrimaryWaitForReadyWithUninitializedShard() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = getSystem().actorOf(newPropsShardMgrWithMockShardActor());

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

            shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, true), getRef());

            expectMsgClass(duration("2 seconds"), NotInitializedException.class);

            shardManager.tell(new ActorInitialized(), mockShardActor);

            expectNoMsg(FiniteDuration.create(200, TimeUnit.MILLISECONDS));
        }};
    }

    @Test
    public void testOnReceiveFindPrimaryWaitForReadyWithCandidateShard() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = getSystem().actorOf(newPropsShardMgrWithMockShardActor());

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
            shardManager.tell(new ActorInitialized(), mockShardActor);
            shardManager.tell(new RoleChangeNotification("member-1-shard-default-" + shardMrgIDSuffix,
                    null, RaftState.Candidate.name()), mockShardActor);

            shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, true), getRef());

            expectMsgClass(duration("2 seconds"), NoShardLeaderException.class);
        }};
    }

    @Test
    public void testOnReceiveFindPrimaryWaitForReadyWithNoRoleShard() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = getSystem().actorOf(newPropsShardMgrWithMockShardActor());

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
            shardManager.tell(new ActorInitialized(), mockShardActor);

            shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, true), getRef());

            expectMsgClass(duration("2 seconds"), NoShardLeaderException.class);
        }};
    }

    @Test
    public void testOnReceiveFindPrimaryForRemoteShard() throws Exception {
        String shardManagerID = ShardManagerIdentifier.builder().type(shardMrgIDSuffix).build().toString();

        // Create an ActorSystem ShardManager actor for member-1.

        final ActorSystem system1 = ActorSystem.create("cluster-test", ConfigFactory.load().getConfig("Member1"));
        Cluster.get(system1).join(AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558"));

        ActorRef mockShardActor1 = newMockShardActor(system1, Shard.DEFAULT_NAME, "member-1");

        final TestActorRef<ForwardingShardManager> shardManager1 = TestActorRef.create(system1,
                newPropsShardMgrWithMockShardActor("shardManager1", mockShardActor1, new ClusterWrapperImpl(system1),
                        new MockConfiguration()), shardManagerID);

        // Create an ActorSystem ShardManager actor for member-2.

        final ActorSystem system2 = ActorSystem.create("cluster-test", ConfigFactory.load().getConfig("Member2"));

        Cluster.get(system2).join(AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558"));

        final ActorRef mockShardActor2 = newMockShardActor(system2, "astronauts", "member-2");

        MockConfiguration mockConfig2 = new MockConfiguration(ImmutableMap.<String, List<String>>builder().
                put("default", Arrays.asList("member-1", "member-2")).
                put("astronauts", Arrays.asList("member-2")).build());

        final TestActorRef<ForwardingShardManager> shardManager2 = TestActorRef.create(system2,
                newPropsShardMgrWithMockShardActor("shardManager2", mockShardActor2, new ClusterWrapperImpl(system2),
                        mockConfig2), shardManagerID);

        new JavaTestKit(system1) {{

            shardManager1.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
            shardManager2.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

            shardManager2.tell(new ActorInitialized(), mockShardActor2);
            shardManager2.tell(new RoleChangeNotification("member-2-shard-astronauts-" + shardMrgIDSuffix,
                    RaftState.Candidate.name(), RaftState.Leader.name()), mockShardActor2);

            shardManager1.underlyingActor().waitForMemberUp();

            shardManager1.tell(new FindPrimary("astronauts", false), getRef());

            PrimaryFound found = expectMsgClass(duration("5 seconds"), PrimaryFound.class);
            String path = found.getPrimaryPath();
            assertTrue("Unexpected primary path " + path, path.contains("member-2-shard-astronauts-config"));

            shardManager2.underlyingActor().verifyFindPrimary();

            Cluster.get(system2).down(AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558"));

            shardManager1.underlyingActor().waitForMemberRemoved();

            shardManager1.tell(new FindPrimary("astronauts", false), getRef());

            expectMsgClass(duration("5 seconds"), PrimaryNotFoundException.class);
        }};

        JavaTestKit.shutdownActorSystem(system1);
    }

    @Test
    public void testOnReceiveFindLocalShardForNonExistentShard() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = getSystem().actorOf(newPropsShardMgrWithMockShardActor());

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

            shardManager.tell(new FindLocalShard("non-existent", false), getRef());

            LocalShardNotFound notFound = expectMsgClass(duration("5 seconds"), LocalShardNotFound.class);

            assertEquals("getShardName", "non-existent", notFound.getShardName());
        }};
    }

    @Test
    public void testOnReceiveFindLocalShardForExistentShard() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = getSystem().actorOf(newPropsShardMgrWithMockShardActor());

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
            shardManager.tell(new ActorInitialized(), mockShardActor);

            shardManager.tell(new FindLocalShard(Shard.DEFAULT_NAME, false), getRef());

            LocalShardFound found = expectMsgClass(duration("5 seconds"), LocalShardFound.class);

            assertTrue("Found path contains " + found.getPath().path().toString(),
                    found.getPath().path().toString().contains("member-1-shard-default-config"));
        }};
    }

    @Test
    public void testOnReceiveFindLocalShardForNotInitializedShard() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = getSystem().actorOf(newPropsShardMgrWithMockShardActor());

            shardManager.tell(new FindLocalShard(Shard.DEFAULT_NAME, false), getRef());

            expectMsgClass(duration("5 seconds"), NotInitializedException.class);
        }};
    }

    @Test
    public void testOnReceiveFindLocalShardWaitForShardInitialized() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = getSystem().actorOf(newPropsShardMgrWithMockShardActor());

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

            // We're passing waitUntilInitialized = true to FindLocalShard so the response should be
            // delayed until we send ActorInitialized.
            Future<Object> future = Patterns.ask(shardManager, new FindLocalShard(Shard.DEFAULT_NAME, true),
                    new Timeout(5, TimeUnit.SECONDS));

            shardManager.tell(new ActorInitialized(), mockShardActor);

            Object resp = Await.result(future, duration("5 seconds"));
            assertTrue("Expected: LocalShardFound, Actual: " + resp, resp instanceof LocalShardFound);
        }};
    }

    @Test
    public void testOnRecoveryJournalIsCleaned() {
        InMemoryJournal.addEntry(shardMgrID, 1L, new ShardManager.SchemaContextModules(
                ImmutableSet.of("foo")));
        InMemoryJournal.addEntry(shardMgrID, 2L, new ShardManager.SchemaContextModules(
                ImmutableSet.of("bar")));
        InMemoryJournal.addDeleteMessagesCompleteLatch(shardMgrID);

        new JavaTestKit(getSystem()) {{
            TestActorRef<TestShardManager> shardManager = TestActorRef.create(getSystem(),
                    Props.create(new TestShardManagerCreator(shardMrgIDSuffix)));

            shardManager.underlyingActor().waitForRecoveryComplete();
            InMemoryJournal.waitForDeleteMessagesComplete(shardMgrID);

            // Journal entries up to the last one should've been deleted
            Map<Long, Object> journal = InMemoryJournal.get(shardMgrID);
            synchronized (journal) {
                assertEquals("Journal size", 1, journal.size());
                assertEquals("Journal entry seq #", Long.valueOf(2), journal.keySet().iterator().next());
            }
        }};
    }

    @Test
    public void testOnRecoveryPreviouslyKnownModulesAreDiscovered() throws Exception {
        final ImmutableSet<String> persistedModules = ImmutableSet.of("foo", "bar");
        InMemoryJournal.addEntry(shardMgrID, 1L, new ShardManager.SchemaContextModules(
                persistedModules));
        new JavaTestKit(getSystem()) {{
            TestActorRef<TestShardManager> shardManager = TestActorRef.create(getSystem(),
                    Props.create(new TestShardManagerCreator(shardMrgIDSuffix)));

            shardManager.underlyingActor().waitForRecoveryComplete();

            Collection<String> knownModules = shardManager.underlyingActor().getKnownModules();

            assertEquals("getKnownModules", persistedModules, Sets.newHashSet(knownModules));
        }};
    }

    @Test
    public void testOnUpdateSchemaContextUpdateKnownModulesIfTheyContainASuperSetOfTheKnownModules()
            throws Exception {
        new JavaTestKit(getSystem()) {{
            final TestActorRef<ShardManager> shardManager =
                    TestActorRef.create(getSystem(), newShardMgrProps());

            assertEquals("getKnownModules size", 0, shardManager.underlyingActor().getKnownModules().size());

            ModuleIdentifier foo = mock(ModuleIdentifier.class);
            when(foo.getNamespace()).thenReturn(new URI("foo"));

            Set<ModuleIdentifier> moduleIdentifierSet = new HashSet<>();
            moduleIdentifierSet.add(foo);

            SchemaContext schemaContext = mock(SchemaContext.class);
            when(schemaContext.getAllModuleIdentifiers()).thenReturn(moduleIdentifierSet);

            shardManager.underlyingActor().onReceiveCommand(new UpdateSchemaContext(schemaContext));

            assertEquals("getKnownModules", Sets.newHashSet("foo"),
                    Sets.newHashSet(shardManager.underlyingActor().getKnownModules()));

            ModuleIdentifier bar = mock(ModuleIdentifier.class);
            when(bar.getNamespace()).thenReturn(new URI("bar"));

            moduleIdentifierSet.add(bar);

            shardManager.underlyingActor().onReceiveCommand(new UpdateSchemaContext(schemaContext));

            assertEquals("getKnownModules", Sets.newHashSet("foo", "bar"),
                    Sets.newHashSet(shardManager.underlyingActor().getKnownModules()));
        }};
    }

    @Test
    public void testOnUpdateSchemaContextDoNotUpdateKnownModulesIfTheyDoNotContainASuperSetOfKnownModules()
            throws Exception {
        new JavaTestKit(getSystem()) {{
            final TestActorRef<ShardManager> shardManager =
                    TestActorRef.create(getSystem(), newShardMgrProps());

            SchemaContext schemaContext = mock(SchemaContext.class);
            Set<ModuleIdentifier> moduleIdentifierSet = new HashSet<>();

            ModuleIdentifier foo = mock(ModuleIdentifier.class);
            when(foo.getNamespace()).thenReturn(new URI("foo"));

            moduleIdentifierSet.add(foo);

            when(schemaContext.getAllModuleIdentifiers()).thenReturn(moduleIdentifierSet);

            shardManager.underlyingActor().onReceiveCommand(new UpdateSchemaContext(schemaContext));

            assertEquals("getKnownModules", Sets.newHashSet("foo"),
                    Sets.newHashSet(shardManager.underlyingActor().getKnownModules()));

            //Create a completely different SchemaContext with only the bar module in it
            //schemaContext = mock(SchemaContext.class);
            moduleIdentifierSet.clear();
            ModuleIdentifier bar = mock(ModuleIdentifier.class);
            when(bar.getNamespace()).thenReturn(new URI("bar"));

            moduleIdentifierSet.add(bar);

            shardManager.underlyingActor().onReceiveCommand(new UpdateSchemaContext(schemaContext));

            assertEquals("getKnownModules", Sets.newHashSet("foo"),
                    Sets.newHashSet(shardManager.underlyingActor().getKnownModules()));

        }};
    }

    @Test
    public void testRecoveryApplicable(){
        new JavaTestKit(getSystem()) {
            {
                final Props persistentProps = ShardManager.props(
                        new MockClusterWrapper(),
                        new MockConfiguration(),
                        DatastoreContext.newBuilder().persistent(true).build(), ready);
                final TestActorRef<ShardManager> persistentShardManager =
                        TestActorRef.create(getSystem(), persistentProps);

                DataPersistenceProvider dataPersistenceProvider1 = persistentShardManager.underlyingActor().getDataPersistenceProvider();

                assertTrue("Recovery Applicable", dataPersistenceProvider1.isRecoveryApplicable());

                final Props nonPersistentProps = ShardManager.props(
                        new MockClusterWrapper(),
                        new MockConfiguration(),
                        DatastoreContext.newBuilder().persistent(false).build(), ready);
                final TestActorRef<ShardManager> nonPersistentShardManager =
                        TestActorRef.create(getSystem(), nonPersistentProps);

                DataPersistenceProvider dataPersistenceProvider2 = nonPersistentShardManager.underlyingActor().getDataPersistenceProvider();

                assertFalse("Recovery Not Applicable", dataPersistenceProvider2.isRecoveryApplicable());


            }};

    }

    @Test
    public void testOnUpdateSchemaContextUpdateKnownModulesCallsDataPersistenceProvider()
            throws Exception {
        final CountDownLatch persistLatch = new CountDownLatch(1);
        final Creator<ShardManager> creator = new Creator<ShardManager>() {
            private static final long serialVersionUID = 1L;
            @Override
            public ShardManager create() throws Exception {
                return new ShardManager(new MockClusterWrapper(), new MockConfiguration(), DatastoreContext.newBuilder().build(), ready) {
                    @Override
                    protected DataPersistenceProvider createDataPersistenceProvider(boolean persistent) {
                        DataPersistenceProviderMonitor dataPersistenceProviderMonitor
                                = new DataPersistenceProviderMonitor();
                        dataPersistenceProviderMonitor.setPersistLatch(persistLatch);
                        return dataPersistenceProviderMonitor;
                    }
                };
            }
        };

        new JavaTestKit(getSystem()) {{

            final TestActorRef<ShardManager> shardManager =
                    TestActorRef.create(getSystem(), Props.create(new DelegatingShardManagerCreator(creator)));

            ModuleIdentifier foo = mock(ModuleIdentifier.class);
            when(foo.getNamespace()).thenReturn(new URI("foo"));

            Set<ModuleIdentifier> moduleIdentifierSet = new HashSet<>();
            moduleIdentifierSet.add(foo);

            SchemaContext schemaContext = mock(SchemaContext.class);
            when(schemaContext.getAllModuleIdentifiers()).thenReturn(moduleIdentifierSet);

            shardManager.underlyingActor().onReceiveCommand(new UpdateSchemaContext(schemaContext));

            assertEquals("Persisted", true,
                    Uninterruptibles.awaitUninterruptibly(persistLatch, 5, TimeUnit.SECONDS));

        }};
    }

    @Test
    public void testRoleChangeNotificationReleaseReady() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                TestActorRef<ShardManager> shardManager = TestActorRef.create(getSystem(), newShardMgrProps());

                shardManager.underlyingActor().onReceiveCommand(new RoleChangeNotification(
                        "member-1-shard-default-" + shardMrgIDSuffix, RaftState.Candidate.name(), RaftState.Leader.name()));

                verify(ready, times(1)).countDown();

            }};
    }

    @Test
    public void testRoleChangeNotificationDoNothingForUnknownShard() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                TestActorRef<ShardManager> shardManager = TestActorRef.create(getSystem(), newShardMgrProps());

                shardManager.underlyingActor().onReceiveCommand(new RoleChangeNotification(
                        "unknown", RaftState.Candidate.name(), RaftState.Leader.name()));

                verify(ready, never()).countDown();

            }};
    }


    @Test
    public void testByDefaultSyncStatusIsFalse() throws Exception{
        final Props persistentProps = ShardManager.props(
                new MockClusterWrapper(),
                new MockConfiguration(),
                DatastoreContext.newBuilder().persistent(true).build(), ready);
        final TestActorRef<ShardManager> shardManager =
                TestActorRef.create(getSystem(), persistentProps);

        ShardManager shardManagerActor = shardManager.underlyingActor();

        assertEquals(false, shardManagerActor.getMBean().getSyncStatus());
    }

    @Test
    public void testWhenShardIsLeaderSyncStatusIsTrue() throws Exception{
        final Props persistentProps = ShardManager.props(
                new MockClusterWrapper(),
                new MockConfiguration(),
                DatastoreContext.newBuilder().persistent(true).build(), ready);
        final TestActorRef<ShardManager> shardManager =
                TestActorRef.create(getSystem(), persistentProps);

        ShardManager shardManagerActor = shardManager.underlyingActor();
        shardManagerActor.onReceiveCommand(new RoleChangeNotification("member-1-shard-default-unknown",
                RaftState.Follower.name(), RaftState.Leader.name()));

        assertEquals(true, shardManagerActor.getMBean().getSyncStatus());
    }

    @Test
    public void testWhenShardIsCandidateSyncStatusIsFalse() throws Exception{
        final Props persistentProps = ShardManager.props(
                new MockClusterWrapper(),
                new MockConfiguration(),
                DatastoreContext.newBuilder().persistent(true).build(), ready);
        final TestActorRef<ShardManager> shardManager =
                TestActorRef.create(getSystem(), persistentProps);

        ShardManager shardManagerActor = shardManager.underlyingActor();
        shardManagerActor.onReceiveCommand(new RoleChangeNotification("member-1-shard-default-unknown",
                RaftState.Follower.name(), RaftState.Candidate.name()));

        assertEquals(false, shardManagerActor.getMBean().getSyncStatus());

        // Send a FollowerInitialSyncStatus with status = true for the replica whose current state is candidate
        shardManagerActor.onReceiveCommand(new FollowerInitialSyncUpStatus(true, "member-1-shard-default-unknown"));

        assertEquals(false, shardManagerActor.getMBean().getSyncStatus());
    }

    @Test
    public void testWhenShardIsFollowerSyncStatusDependsOnFollowerInitialSyncStatus() throws Exception{
        final Props persistentProps = ShardManager.props(
                new MockClusterWrapper(),
                new MockConfiguration(),
                DatastoreContext.newBuilder().persistent(true).build(), ready);
        final TestActorRef<ShardManager> shardManager =
                TestActorRef.create(getSystem(), persistentProps);

        ShardManager shardManagerActor = shardManager.underlyingActor();
        shardManagerActor.onReceiveCommand(new RoleChangeNotification("member-1-shard-default-unknown",
                RaftState.Candidate.name(), RaftState.Follower.name()));

        // Initially will be false
        assertEquals(false, shardManagerActor.getMBean().getSyncStatus());

        // Send status true will make sync status true
        shardManagerActor.onReceiveCommand(new FollowerInitialSyncUpStatus(true, "member-1-shard-default-unknown"));

        assertEquals(true, shardManagerActor.getMBean().getSyncStatus());

        // Send status false will make sync status false
        shardManagerActor.onReceiveCommand(new FollowerInitialSyncUpStatus(false, "member-1-shard-default-unknown"));

        assertEquals(false, shardManagerActor.getMBean().getSyncStatus());

    }

    @Test
    public void testWhenMultipleShardsPresentSyncStatusMustBeTrueForAllShards() throws Exception{
        final Props persistentProps = ShardManager.props(
                new MockClusterWrapper(),
                new MockConfiguration() {
                    @Override
                    public List<String> getMemberShardNames(String memberName) {
                        return Arrays.asList("default", "astronauts");
                    }
                },
                DatastoreContext.newBuilder().persistent(true).build(), ready);
        final TestActorRef<ShardManager> shardManager =
                TestActorRef.create(getSystem(), persistentProps);

        ShardManager shardManagerActor = shardManager.underlyingActor();

        // Initially will be false
        assertEquals(false, shardManagerActor.getMBean().getSyncStatus());

        // Make default shard leader
        shardManagerActor.onReceiveCommand(new RoleChangeNotification("member-1-shard-default-unknown",
                RaftState.Follower.name(), RaftState.Leader.name()));

        // default = Leader, astronauts is unknown so sync status remains false
        assertEquals(false, shardManagerActor.getMBean().getSyncStatus());

        // Make astronauts shard leader as well
        shardManagerActor.onReceiveCommand(new RoleChangeNotification("member-1-shard-astronauts-unknown",
                RaftState.Follower.name(), RaftState.Leader.name()));

        // Now sync status should be true
        assertEquals(true, shardManagerActor.getMBean().getSyncStatus());

        // Make astronauts a Follower
        shardManagerActor.onReceiveCommand(new RoleChangeNotification("member-1-shard-astronauts-unknown",
                RaftState.Leader.name(), RaftState.Follower.name()));

        // Sync status is not true
        assertEquals(false, shardManagerActor.getMBean().getSyncStatus());

        // Make the astronauts follower sync status true
        shardManagerActor.onReceiveCommand(new FollowerInitialSyncUpStatus(true, "member-1-shard-astronauts-unknown"));

        // Sync status is now true
        assertEquals(true, shardManagerActor.getMBean().getSyncStatus());

    }

    private static class TestShardManager extends ShardManager {
        private final CountDownLatch recoveryComplete = new CountDownLatch(1);

        TestShardManager(String shardMrgIDSuffix) {
            super(new MockClusterWrapper(), new MockConfiguration(),
                    DatastoreContext.newBuilder().dataStoreType(shardMrgIDSuffix).build(), ready);
        }

        @Override
        public void handleRecover(Object message) throws Exception {
            try {
                super.handleRecover(message);
            } finally {
                if(message instanceof RecoveryCompleted) {
                    recoveryComplete.countDown();
                }
            }
        }

        void waitForRecoveryComplete() {
            assertEquals("Recovery complete", true,
                    Uninterruptibles.awaitUninterruptibly(recoveryComplete, 5, TimeUnit.SECONDS));
        }
    }

    @SuppressWarnings("serial")
    static class TestShardManagerCreator implements Creator<TestShardManager> {
        String shardMrgIDSuffix;

        TestShardManagerCreator(String shardMrgIDSuffix) {
            this.shardMrgIDSuffix = shardMrgIDSuffix;
        }

        @Override
        public TestShardManager create() throws Exception {
            return new TestShardManager(shardMrgIDSuffix);
        }

    }

    private static class DelegatingShardManagerCreator implements Creator<ShardManager> {
        private static final long serialVersionUID = 1L;
        private final Creator<ShardManager> delegate;

        public DelegatingShardManagerCreator(Creator<ShardManager> delegate) {
            this.delegate = delegate;
        }

        @Override
        public ShardManager create() throws Exception {
            return delegate.create();
        }
    }

    private static class ForwardingShardManager extends ShardManager {
        private CountDownLatch findPrimaryMessageReceived = new CountDownLatch(1);
        private CountDownLatch memberUpReceived = new CountDownLatch(1);
        private CountDownLatch memberRemovedReceived = new CountDownLatch(1);
        private final ActorRef shardActor;
        private final String name;

        protected ForwardingShardManager(ClusterWrapper cluster, Configuration configuration,
                DatastoreContext datastoreContext, CountDownLatch waitTillReadyCountdownLatch, String name,
                ActorRef shardActor) {
            super(cluster, configuration, datastoreContext, waitTillReadyCountdownLatch);
            this.shardActor = shardActor;
            this.name = name;
        }

        @Override
        public void handleCommand(Object message) throws Exception {
            try{
                super.handleCommand(message);
            } finally {
                if(message instanceof FindPrimary) {
                    findPrimaryMessageReceived.countDown();
                } else if(message instanceof ClusterEvent.MemberUp) {
                    String role = ((ClusterEvent.MemberUp)message).member().roles().head();
                    if(!getCluster().getCurrentMemberName().equals(role)) {
                        memberUpReceived.countDown();
                    }
                } else if(message instanceof ClusterEvent.MemberRemoved) {
                    String role = ((ClusterEvent.MemberRemoved)message).member().roles().head();
                    if(!getCluster().getCurrentMemberName().equals(role)) {
                        memberRemovedReceived.countDown();
                    }
                }
            }
        }

        @Override
        public String persistenceId() {
            return name;
        }

        @Override
        protected ActorRef newShardActor(SchemaContext schemaContext, ShardInformation info) {
            return shardActor;
        }

        void waitForMemberUp() {
            assertEquals("MemberUp received", true,
                    Uninterruptibles.awaitUninterruptibly(memberUpReceived, 5, TimeUnit.SECONDS));
            memberUpReceived = new CountDownLatch(1);
        }

        void waitForMemberRemoved() {
            assertEquals("MemberRemoved received", true,
                    Uninterruptibles.awaitUninterruptibly(memberRemovedReceived, 5, TimeUnit.SECONDS));
            memberRemovedReceived = new CountDownLatch(1);
        }

        void verifyFindPrimary() {
            assertEquals("FindPrimary received", true,
                    Uninterruptibles.awaitUninterruptibly(findPrimaryMessageReceived, 5, TimeUnit.SECONDS));
            findPrimaryMessageReceived = new CountDownLatch(1);
        }
    }
}
