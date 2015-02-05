package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.Creator;
import akka.pattern.Patterns;
import akka.persistence.RecoveryCompleted;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import akka.util.Timeout;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Uninterruptibles;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.ActorInitialized;
import org.opendaylight.controller.cluster.datastore.messages.ActorNotInitialized;
import org.opendaylight.controller.cluster.datastore.messages.FindLocalShard;
import org.opendaylight.controller.cluster.datastore.messages.FindPrimary;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardFound;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardNotFound;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryFound;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryNotFound;
import org.opendaylight.controller.cluster.datastore.messages.UpdateSchemaContext;
import org.opendaylight.controller.cluster.datastore.utils.DoNothingActor;
import org.opendaylight.controller.cluster.datastore.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.datastore.utils.MockClusterWrapper;
import org.opendaylight.controller.cluster.datastore.utils.MockConfiguration;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.yang.model.api.ModuleIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import scala.concurrent.Await;
import scala.concurrent.Future;

public class ShardManagerTest extends AbstractActorTest {
    private static int ID_COUNTER = 1;

    private final String shardMrgIDSuffix = "config" + ID_COUNTER++;
    private final String shardMgrID = "shard-manager-" + shardMrgIDSuffix;

    private static ActorRef mockShardActor;

    @Before
    public void setUp() {
        InMemoryJournal.clear();

        if(mockShardActor == null) {
            String name = new ShardIdentifier(Shard.DEFAULT_NAME, "member-1","config").toString();
            mockShardActor = getSystem().actorOf(Props.create(DoNothingActor.class), name);
        }
    }

    @After
    public void tearDown() {
        InMemoryJournal.clear();
    }

    private Props newShardMgrProps() {

        DatastoreContext.Builder builder = DatastoreContext.newBuilder();
        builder.dataStoreType(shardMrgIDSuffix);
        return ShardManager.props(new MockClusterWrapper(), new MockConfiguration(), builder.build());
    }

    @Test
    public void testOnReceiveFindPrimaryForNonExistentShard() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = getSystem().actorOf(newShardMgrProps());

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

            shardManager.tell(new FindPrimary("non-existent", false).toSerializable(), getRef());

            expectMsgEquals(duration("5 seconds"),
                    new PrimaryNotFound("non-existent").toSerializable());
        }};
    }

    @Test
    public void testOnReceiveFindPrimaryForExistentShard() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = getSystem().actorOf(newShardMgrProps());

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
            shardManager.tell(new ActorInitialized(), mockShardActor);

            shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, false).toSerializable(), getRef());

            expectMsgClass(duration("5 seconds"), PrimaryFound.SERIALIZABLE_CLASS);
        }};
    }

    @Test
    public void testOnReceiveFindPrimaryForNotInitializedShard() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = getSystem().actorOf(newShardMgrProps());

            shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME, false).toSerializable(), getRef());

            expectMsgClass(duration("5 seconds"), ActorNotInitialized.class);
        }};
    }

    @Test
    public void testOnReceiveFindPrimaryWaitForShardInitialized() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = getSystem().actorOf(newShardMgrProps());

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

            // We're passing waitUntilInitialized = true to FindPrimary so the response should be
            // delayed until we send ActorInitialized.
            Future<Object> future = Patterns.ask(shardManager, new FindPrimary(Shard.DEFAULT_NAME, true),
                    new Timeout(5, TimeUnit.SECONDS));

            shardManager.tell(new ActorInitialized(), mockShardActor);

            Object resp = Await.result(future, duration("5 seconds"));
            assertTrue("Expected: PrimaryFound, Actual: " + resp, resp instanceof PrimaryFound);
        }};
    }

    @Test
    public void testOnReceiveFindLocalShardForNonExistentShard() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = getSystem().actorOf(newShardMgrProps());

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

            shardManager.tell(new FindLocalShard("non-existent", false), getRef());

            LocalShardNotFound notFound = expectMsgClass(duration("5 seconds"), LocalShardNotFound.class);

            assertEquals("getShardName", "non-existent", notFound.getShardName());
        }};
    }

    @Test
    public void testOnReceiveFindLocalShardForExistentShard() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = getSystem().actorOf(newShardMgrProps());

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
            final ActorRef shardManager = getSystem().actorOf(newShardMgrProps());

            shardManager.tell(new FindLocalShard(Shard.DEFAULT_NAME, false), getRef());

            expectMsgClass(duration("5 seconds"), ActorNotInitialized.class);
        }};
    }

    @Test
    public void testOnReceiveFindLocalShardWaitForShardInitialized() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = getSystem().actorOf(newShardMgrProps());

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
    public void testOnReceiveMemberUp() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = getSystem().actorOf(newShardMgrProps());

            MockClusterWrapper.sendMemberUp(shardManager, "member-2", getRef().path().toString());

            shardManager.tell(new FindPrimary("astronauts", false).toSerializable(), getRef());

            PrimaryFound found = PrimaryFound.fromSerializable(expectMsgClass(duration("5 seconds"),
                    PrimaryFound.SERIALIZABLE_CLASS));
            String path = found.getPrimaryPath();
            assertTrue("Found path contains " + path, path.contains("member-2-shard-astronauts-config"));
        }};
    }

    @Test
    public void testOnReceiveMemberDown() throws Exception {

        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = getSystem().actorOf(newShardMgrProps());

            MockClusterWrapper.sendMemberUp(shardManager, "member-2", getRef().path().toString());

            shardManager.tell(new FindPrimary("astronauts", false).toSerializable(), getRef());

            expectMsgClass(duration("5 seconds"), PrimaryFound.SERIALIZABLE_CLASS);

            MockClusterWrapper.sendMemberRemoved(shardManager, "member-2", getRef().path().toString());

            shardManager.tell(new FindPrimary("astronauts", false).toSerializable(), getRef());

            expectMsgClass(duration("5 seconds"), PrimaryNotFound.SERIALIZABLE_CLASS);
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
                final Props persistentProps = ShardManager.props(new MockClusterWrapper(), new MockConfiguration(),
                        DatastoreContext.newBuilder().persistent(true).dataStoreType(shardMrgIDSuffix).build());
                final TestActorRef<ShardManager> persistentShardManager =
                        TestActorRef.create(getSystem(), persistentProps);

                DataPersistenceProvider dataPersistenceProvider1 = persistentShardManager.underlyingActor().getDataPersistenceProvider();

                assertTrue("Recovery Applicable", dataPersistenceProvider1.isRecoveryApplicable());

                final Props nonPersistentProps = ShardManager.props(new MockClusterWrapper(), new MockConfiguration(),
                        DatastoreContext.newBuilder().persistent(false).dataStoreType(shardMrgIDSuffix).build());
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
                return new ShardManager(new MockClusterWrapper(), new MockConfiguration(),
                        DatastoreContext.newBuilder().dataStoreType(shardMrgIDSuffix).build()) {
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



    private static class TestShardManager extends ShardManager {
        private final CountDownLatch recoveryComplete = new CountDownLatch(1);

        TestShardManager(String shardMrgIDSuffix) {
            super(new MockClusterWrapper(), new MockConfiguration(),
                    DatastoreContext.newBuilder().dataStoreType(shardMrgIDSuffix).build());
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
        private Creator<ShardManager> delegate;

        public DelegatingShardManagerCreator(Creator<ShardManager> delegate) {
            this.delegate = delegate;
        }

        @Override
        public ShardManager create() throws Exception {
            return delegate.create();
        }
    }
}
