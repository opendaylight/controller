package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.japi.Procedure;
import akka.persistence.PersistentConfirmation;
import akka.persistence.PersistentId;
import akka.persistence.PersistentImpl;
import akka.persistence.PersistentRepr;
import akka.persistence.journal.japi.AsyncWriteJournal;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.messages.FindLocalShard;
import org.opendaylight.controller.cluster.datastore.messages.FindPrimary;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardFound;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardNotFound;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryFound;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryNotFound;
import org.opendaylight.controller.cluster.datastore.messages.UpdateSchemaContext;
import org.opendaylight.controller.cluster.datastore.utils.MockClusterWrapper;
import org.opendaylight.controller.cluster.datastore.utils.MockConfiguration;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.yang.model.api.ModuleIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import scala.concurrent.Future;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ShardManagerTest {
    private static ActorSystem system;

    @BeforeClass
    public static void setUpClass() {
        Map<String, String> myJournal = new HashMap<>();
        myJournal.put("class", "org.opendaylight.controller.cluster.datastore.ShardManagerTest$MyJournal");
        myJournal.put("plugin-dispatcher", "akka.actor.default-dispatcher");
        Config config = ConfigFactory.load()
            .withValue("akka.persistence.journal.plugin",
                ConfigValueFactory.fromAnyRef("my-journal"))
            .withValue("my-journal", ConfigValueFactory.fromMap(myJournal));

        MyJournal.clear();

        system = ActorSystem.create("test", config);
    }

    @AfterClass
    public static void tearDown() {
        JavaTestKit.shutdownActorSystem(system);
        system = null;
    }

    @Before
    public void setUpTest(){
        MyJournal.clear();
    }

    @Test
    public void testOnReceiveFindPrimaryForNonExistentShard() throws Exception {

        new JavaTestKit(system) {{
            final Props props = ShardManager
                .props("config", new MockClusterWrapper(),
                    new MockConfiguration(), new DatastoreContext());

            final ActorRef subject = getSystem().actorOf(props);

            new Within(duration("3 seconds")) {
                @Override
                protected void run() {

                    subject.tell(new FindPrimary("inventory").toSerializable(), getRef());

                    expectMsgEquals(duration("2 seconds"),
                        new PrimaryNotFound("inventory").toSerializable());
                }
            };
        }};
    }

    @Test
    public void testOnReceiveFindPrimaryForExistentShard() throws Exception {

        new JavaTestKit(system) {{
            final Props props = ShardManager
                .props("config", new MockClusterWrapper(),
                    new MockConfiguration(), new DatastoreContext());

            final ActorRef subject = getSystem().actorOf(props);

            subject.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

            new Within(duration("3 seconds")) {
                @Override
                protected void run() {

                    subject.tell(new FindPrimary(Shard.DEFAULT_NAME).toSerializable(), getRef());

                    expectMsgClass(duration("1 seconds"), PrimaryFound.SERIALIZABLE_CLASS);
                }
            };
        }};
    }

    @Test
    public void testOnReceiveFindLocalShardForNonExistentShard() throws Exception {

        new JavaTestKit(system) {{
            final Props props = ShardManager
                .props("config", new MockClusterWrapper(),
                    new MockConfiguration(), new DatastoreContext());

            final ActorRef subject = getSystem().actorOf(props);

            new Within(duration("3 seconds")) {
                @Override
                protected void run() {

                    subject.tell(new FindLocalShard("inventory"), getRef());

                    final String out = new ExpectMsg<String>(duration("3 seconds"), "find local") {
                        @Override
                        protected String match(Object in) {
                            if (in instanceof LocalShardNotFound) {
                                return ((LocalShardNotFound) in).getShardName();
                            } else {
                                throw noMatch();
                            }
                        }
                    }.get(); // this extracts the received message

                    assertEquals("inventory", out);
                }
            };
        }};
    }

    @Test
    public void testOnReceiveFindLocalShardForExistentShard() throws Exception {

        final MockClusterWrapper mockClusterWrapper = new MockClusterWrapper();

        new JavaTestKit(system) {{
            final Props props = ShardManager
                .props("config", mockClusterWrapper,
                    new MockConfiguration(), new DatastoreContext());

            final ActorRef subject = getSystem().actorOf(props);

            subject.tell(new UpdateSchemaContext(TestModel.createTestContext()), getRef());

            new Within(duration("3 seconds")) {
                @Override
                protected void run() {

                    subject.tell(new FindLocalShard(Shard.DEFAULT_NAME), getRef());

                    final ActorRef out = new ExpectMsg<ActorRef>(duration("3 seconds"), "find local") {
                        @Override
                        protected ActorRef match(Object in) {
                            if (in instanceof LocalShardFound) {
                                return ((LocalShardFound) in).getPath();
                            } else {
                                throw noMatch();
                            }
                        }
                    }.get(); // this extracts the received message

                    assertTrue(out.path().toString(), out.path().toString().contains("member-1-shard-default-config"));
                }
            };
        }};
    }

    @Test
    public void testOnReceiveMemberUp() throws Exception {

        new JavaTestKit(system) {{
            final Props props = ShardManager
                .props("config", new MockClusterWrapper(),
                    new MockConfiguration(), new DatastoreContext());

            final ActorRef subject = getSystem().actorOf(props);

            // the run() method needs to finish within 3 seconds
            new Within(duration("3 seconds")) {
                @Override
                protected void run() {

                    MockClusterWrapper.sendMemberUp(subject, "member-2", getRef().path().toString());

                    subject.tell(new FindPrimary("astronauts").toSerializable(), getRef());

                    final String out = new ExpectMsg<String>(duration("3 seconds"), "primary found") {
                        // do not put code outside this method, will run afterwards
                        @Override
                        protected String match(Object in) {
                            if (in.getClass().equals(PrimaryFound.SERIALIZABLE_CLASS)) {
                                PrimaryFound f = PrimaryFound.fromSerializable(in);
                                return f.getPrimaryPath();
                            } else {
                                throw noMatch();
                            }
                        }
                    }.get(); // this extracts the received message

                    assertTrue(out, out.contains("member-2-shard-astronauts-config"));
                }
            };
        }};
    }

    @Test
    public void testOnReceiveMemberDown() throws Exception {

        new JavaTestKit(system) {{
            final Props props = ShardManager
                .props("config", new MockClusterWrapper(),
                    new MockConfiguration(), new DatastoreContext());

            final ActorRef subject = getSystem().actorOf(props);

            // the run() method needs to finish within 3 seconds
            new Within(duration("10 seconds")) {
                @Override
                protected void run() {

                    MockClusterWrapper.sendMemberUp(subject, "member-2", getRef().path().toString());

                    subject.tell(new FindPrimary("astronauts").toSerializable(), getRef());

                    expectMsgClass(duration("3 seconds"), PrimaryFound.SERIALIZABLE_CLASS);

                    MockClusterWrapper.sendMemberRemoved(subject, "member-2", getRef().path().toString());

                    subject.tell(new FindPrimary("astronauts").toSerializable(), getRef());

                    expectMsgClass(duration("1 seconds"), PrimaryNotFound.SERIALIZABLE_CLASS);

                }
            };
        }};
    }

    @Test
    public void testOnRecoveryJournalIsEmptied(){
        MyJournal.addToJournal(1L, new ShardManager.SchemaContextModules(
            ImmutableList.of("foo")));

        assertEquals(1, MyJournal.get().size());

        new JavaTestKit(system) {{
            final Props props = ShardManager
                .props("config", new MockClusterWrapper(),
                    new MockConfiguration(), new DatastoreContext());

            final ActorRef subject = getSystem().actorOf(props);

            // Send message to check that ShardManager is ready
            subject.tell(new FindPrimary("unknown").toSerializable(), getRef());

            expectMsgClass(duration("3 seconds"), PrimaryNotFound.SERIALIZABLE_CLASS);

            assertEquals(0, MyJournal.get().size());
        }};
    }

    @Test
    public void testOnRecoveryPreviouslyKnownModulesAreDiscovered(){
        MyJournal.addToJournal(1L, new ShardManager.SchemaContextModules(ImmutableList.of("foo")));

        new JavaTestKit(system) {{
            final Props props = ShardManager
                .props("config", new MockClusterWrapper(),
                    new MockConfiguration(), new DatastoreContext());
            final TestActorRef<ShardManager> subject =
                TestActorRef.create(system, props);


            sleep(1000);

            Collection<String> knownModules = subject.underlyingActor().getKnownModules();

            assertTrue(knownModules.contains("foo"));
        }};
    }

    @Test
    public void testOnUpdateSchemaContextUpdateKnownModulesIfTheyContainASuperSetOfTheKnownModules()
        throws URISyntaxException {
        new JavaTestKit(system) {{
            final Props props = ShardManager
                .props("config", new MockClusterWrapper(),
                    new MockConfiguration(), new DatastoreContext());
            final TestActorRef<ShardManager> subject =
                TestActorRef.create(system, props);

            Collection<String> knownModules = subject.underlyingActor().getKnownModules();

            assertEquals(0, knownModules.size());

            sleep(1000);

            SchemaContext schemaContext = mock(SchemaContext.class);
            Set<ModuleIdentifier> moduleIdentifierSet = new HashSet<>();

            ModuleIdentifier foo = mock(ModuleIdentifier.class);
            when(foo.getNamespace()).thenReturn(new URI("foo"));

            moduleIdentifierSet.add(foo);

            when(schemaContext.getAllModuleIdentifiers()).thenReturn(moduleIdentifierSet);

            subject.tell(new UpdateSchemaContext(schemaContext), getRef());

            sleep(500);

            assertTrue(knownModules.contains("foo"));

            assertEquals(1, knownModules.size());

            ModuleIdentifier bar = mock(ModuleIdentifier.class);
            when(bar.getNamespace()).thenReturn(new URI("bar"));

            moduleIdentifierSet.add(bar);

            subject.tell(new UpdateSchemaContext(schemaContext), getRef());

            sleep(500);

            assertTrue(knownModules.contains("bar"));

            assertEquals(2, knownModules.size());

        }};

    }


    @Test
    public void testOnUpdateSchemaContextDoNotUpdateKnownModulesIfTheyDoNotContainASuperSetOfKnownModules()
        throws URISyntaxException {
        new JavaTestKit(system) {{
            final Props props = ShardManager
                .props("config", new MockClusterWrapper(),
                    new MockConfiguration(), new DatastoreContext());
            final TestActorRef<ShardManager> subject =
                TestActorRef.create(system, props);

            Collection<String> knownModules = subject.underlyingActor().getKnownModules();

            assertEquals(0, knownModules.size());

            sleep(1000);

            SchemaContext schemaContext = mock(SchemaContext.class);
            Set<ModuleIdentifier> moduleIdentifierSet = new HashSet<>();

            ModuleIdentifier foo = mock(ModuleIdentifier.class);
            when(foo.getNamespace()).thenReturn(new URI("foo"));

            moduleIdentifierSet.add(foo);

            when(schemaContext.getAllModuleIdentifiers()).thenReturn(moduleIdentifierSet);

            subject.tell(new UpdateSchemaContext(schemaContext), getRef());

            sleep(500);

            assertTrue(knownModules.contains("foo"));

            assertEquals(1, knownModules.size());

            //Create a completely different SchemaContext with only the bar module in it
            schemaContext = mock(SchemaContext.class);
            moduleIdentifierSet = new HashSet<>();
            ModuleIdentifier bar = mock(ModuleIdentifier.class);
            when(bar.getNamespace()).thenReturn(new URI("bar"));

            moduleIdentifierSet.add(bar);

            subject.tell(new UpdateSchemaContext(schemaContext), getRef());

            sleep(500);

            assertFalse(knownModules.contains("bar"));

            assertEquals(1, knownModules.size());

        }};

    }



    private void sleep(long period){
        try {
            Thread.sleep(period);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static class MyJournal extends AsyncWriteJournal {

        private static Map<Long, Object> journal = Maps.newTreeMap();

        public static void addToJournal(Long sequenceNr, Object value){
            journal.put(sequenceNr, value);
        }

        public static Map<Long, Object> get(){
            return journal;
        }

        public static void clear(){
            journal.clear();
        }

        @Override public Future<Void> doAsyncReplayMessages(final String persistenceId, long fromSequenceNr, long toSequenceNr, long max,
            final Procedure<PersistentRepr> replayCallback) {
            return Futures.future(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    for (Map.Entry<Long, Object> entry : journal.entrySet()) {
                        PersistentRepr persistentMessage =
                            new PersistentImpl(entry.getValue(), entry.getKey(), persistenceId,
                                false, null, null);
                        replayCallback.apply(persistentMessage);
                    }
                    return null;
                }
            }, context().dispatcher());
        }

        @Override public Future<Long> doAsyncReadHighestSequenceNr(String s, long l) {
            return Futures.successful(-1L);
        }

        @Override public Future<Void> doAsyncWriteMessages(
            final Iterable<PersistentRepr> persistentReprs) {
            return Futures.future(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    for (PersistentRepr repr : persistentReprs){
                        if(repr.payload() instanceof ShardManager.SchemaContextModules) {
                            journal.put(repr.sequenceNr(), repr.payload());
                        }
                    }
                    return null;
                }
            }, context().dispatcher());
        }

        @Override public Future<Void> doAsyncWriteConfirmations(
            Iterable<PersistentConfirmation> persistentConfirmations) {
            return Futures.successful(null);
        }

        @Override public Future<Void> doAsyncDeleteMessages(Iterable<PersistentId> persistentIds,
            boolean b) {
            clear();
            return Futures.successful(null);
        }

        @Override public Future<Void> doAsyncDeleteMessagesTo(String s, long l, boolean b) {
            clear();
            return Futures.successful(null);
        }
    }
}
