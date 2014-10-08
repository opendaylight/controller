package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.persistence.RecoveryCompleted;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import akka.japi.Creator;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Uninterruptibles;
import org.junit.Before;
import org.junit.Test;
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
import org.opendaylight.controller.cluster.datastore.utils.InMemorySnapshotStore;
import org.opendaylight.controller.cluster.datastore.utils.MockClusterWrapper;
import org.opendaylight.controller.cluster.datastore.utils.MockConfiguration;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.yang.model.api.ModuleIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ShardManagerTest extends AbstractActorTest {
    private static final String SHARD_MGR_ID = "shard-manager-config";
    private static ActorRef mockShardActor;

    @Before
    public void setUp() {
        InMemorySnapshotStore.clear();
        InMemoryJournal.clear();

        if(mockShardActor == null) {
            String name = new ShardIdentifier(Shard.DEFAULT_NAME, "member-1","config").toString();
            mockShardActor = getSystem().actorOf(Props.create(DoNothingActor.class), name);
        }
    }

    @Before
    public void setUpTest() {
        InMemorySnapshotStore.clear();
        InMemoryJournal.clear();
    }

    private Props newShardMgrProps() {
        return ShardManager.props("config", new MockClusterWrapper(), new MockConfiguration(),
                new DatastoreContext());
    }

    @Test
    public void testOnReceiveFindPrimaryForNonExistentShard() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = getSystem().actorOf(newShardMgrProps());

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

            shardManager.tell(new FindPrimary("non-existent").toSerializable(), getRef());

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

            shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME).toSerializable(), getRef());

            expectMsgClass(duration("5 seconds"), PrimaryFound.SERIALIZABLE_CLASS);
        }};
    }

    @Test
    public void testOnReceiveFindPrimaryForNotInitialzedShard() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = getSystem().actorOf(newShardMgrProps());

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

            shardManager.tell(new FindPrimary(Shard.DEFAULT_NAME).toSerializable(), getRef());

            expectMsgClass(duration("5 seconds"), ActorNotInitialized.class);
        }};
    }

    @Test
    public void testOnReceiveFindLocalShardForNonExistentShard() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = getSystem().actorOf(newShardMgrProps());

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

            shardManager.tell(new FindLocalShard("non-existent"), getRef());

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

            shardManager.tell(new FindLocalShard(Shard.DEFAULT_NAME), getRef());

            LocalShardFound found = expectMsgClass(duration("5 seconds"), LocalShardFound.class);

            assertTrue("Found path contains " + found.getPath().path().toString(),
                    found.getPath().path().toString().contains("member-1-shard-default-config"));
        }};
    }

    @Test
    public void testOnReceiveFindLocalShardForNotInitializedShard() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = getSystem().actorOf(newShardMgrProps());

            shardManager.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());
            //shardManager.tell(new ActorInitialized(), mockShardActor);

            shardManager.tell(new FindLocalShard(Shard.DEFAULT_NAME), getRef());

            expectMsgClass(duration("5 seconds"), ActorNotInitialized.class);
        }};
    }

    @Test
    public void testOnReceiveMemberUp() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shardManager = getSystem().actorOf(newShardMgrProps());

            MockClusterWrapper.sendMemberUp(shardManager, "member-2", getRef().path().toString());

            shardManager.tell(new FindPrimary("astronauts").toSerializable(), getRef());

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

            shardManager.tell(new FindPrimary("astronauts").toSerializable(), getRef());

            expectMsgClass(duration("5 seconds"), PrimaryFound.SERIALIZABLE_CLASS);

            MockClusterWrapper.sendMemberRemoved(shardManager, "member-2", getRef().path().toString());

            shardManager.tell(new FindPrimary("astronauts").toSerializable(), getRef());

            expectMsgClass(duration("5 seconds"), PrimaryNotFound.SERIALIZABLE_CLASS);
        }};
    }

    @Test
    public void testOnRecoveryJournalIsCleaned() {
        InMemoryJournal.addEntry(SHARD_MGR_ID, 1L, new ShardManager.SchemaContextModules(
                ImmutableSet.of("foo")));
        InMemoryJournal.addEntry(SHARD_MGR_ID, 2L, new ShardManager.SchemaContextModules(
                ImmutableSet.of("bar")));

        new JavaTestKit(getSystem()) {{
            TestActorRef<TestShardManager> shardManager = TestActorRef.create(getSystem(),
                    Props.create(new TestShardManagerCreator()));

            shardManager.underlyingActor().waitForRecoveryComplete();
            InMemoryJournal.waitForDeleteMessagesComplete();

            // Journal entries up to the last one should've been deleted
            Map<Long, Object> journal = InMemoryJournal.get(SHARD_MGR_ID);
            synchronized (journal) {
                assertEquals("Journal size", 1, journal.size());
                assertEquals("Journal entry seq #", Long.valueOf(2), journal.keySet().iterator().next());
            }
        }};
    }

    @Test
    public void testOnRecoveryPreviouslyKnownModulesAreDiscovered() throws Exception {
        final ImmutableSet<String> persistedModules = ImmutableSet.of("foo", "bar");
        InMemoryJournal.addEntry(SHARD_MGR_ID, 1L, new ShardManager.SchemaContextModules(
                persistedModules));
        new JavaTestKit(getSystem()) {{
            TestActorRef<TestShardManager> shardManager = TestActorRef.create(getSystem(),
                    Props.create(new TestShardManagerCreator()));

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


    private static class TestShardManager extends ShardManager {
        private final CountDownLatch recoveryComplete = new CountDownLatch(1);

        TestShardManager() {
            super("config", new MockClusterWrapper(), new MockConfiguration(),
                    new DatastoreContext());
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
        @Override
        public TestShardManager create() throws Exception {
            return new TestShardManager();
        }

    }
}
