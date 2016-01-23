/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.Futures;
import akka.japi.Creator;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import akka.util.Timeout;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.typesafe.config.ConfigFactory;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.cluster.datastore.AbstractActorTest;
import org.opendaylight.controller.cluster.datastore.ClusterWrapper;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.DatastoreContextFactory;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.exceptions.NoShardLeaderException;
import org.opendaylight.controller.cluster.datastore.exceptions.NotInitializedException;
import org.opendaylight.controller.cluster.datastore.exceptions.PrimaryNotFoundException;
import org.opendaylight.controller.cluster.datastore.messages.FindLocalShard;
import org.opendaylight.controller.cluster.datastore.messages.FindPrimary;
import org.opendaylight.controller.cluster.datastore.messages.LocalPrimaryShardFound;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardFound;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardNotFound;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;
import org.opendaylight.controller.cluster.datastore.messages.RemotePrimaryShardFound;
import org.opendaylight.controller.cluster.raft.utils.EchoActor;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

public class ActorContextTest extends AbstractActorTest{

    static final Logger log = LoggerFactory.getLogger(ActorContextTest.class);

    private static class TestMessage {
    }

    private static class MockShardManager extends UntypedActor {

        private final boolean found;
        private final ActorRef actorRef;
        private final Map<String,Object> findPrimaryResponses = Maps.newHashMap();

        private MockShardManager(boolean found, ActorRef actorRef){

            this.found = found;
            this.actorRef = actorRef;
        }

        @Override public void onReceive(Object message) throws Exception {
            if(message instanceof FindPrimary) {
                FindPrimary fp = (FindPrimary)message;
                Object resp = findPrimaryResponses.get(fp.getShardName());
                if(resp == null) {
                    log.error("No expected FindPrimary response found for shard name {}", fp.getShardName());
                } else {
                    getSender().tell(resp, getSelf());
                }

                return;
            }

            if(found){
                getSender().tell(new LocalShardFound(actorRef), getSelf());
            } else {
                getSender().tell(new LocalShardNotFound(((FindLocalShard) message).getShardName()), getSelf());
            }
        }

        void addFindPrimaryResp(String shardName, Object resp) {
            findPrimaryResponses.put(shardName, resp);
        }

        private static Props props(final boolean found, final ActorRef actorRef){
            return Props.create(new MockShardManagerCreator(found, actorRef) );
        }

        private static Props props(){
            return Props.create(new MockShardManagerCreator() );
        }

        @SuppressWarnings("serial")
        private static class MockShardManagerCreator implements Creator<MockShardManager> {
            final boolean found;
            final ActorRef actorRef;

            MockShardManagerCreator() {
                this.found = false;
                this.actorRef = null;
            }

            MockShardManagerCreator(boolean found, ActorRef actorRef) {
                this.found = found;
                this.actorRef = actorRef;
            }

            @Override
            public MockShardManager create() throws Exception {
                return new MockShardManager(found, actorRef);
            }
        }
    }

    @Test
    public void testFindLocalShardWithShardFound(){
        new JavaTestKit(getSystem()) {{

            new Within(duration("1 seconds")) {
                @Override
                protected void run() {

                    ActorRef shardActorRef = getSystem().actorOf(Props.create(EchoActor.class));

                    ActorRef shardManagerActorRef = getSystem()
                        .actorOf(MockShardManager.props(true, shardActorRef));

                    ActorContext actorContext =
                        new ActorContext(getSystem(), shardManagerActorRef , mock(ClusterWrapper.class),
                            mock(Configuration.class));

                    Optional<ActorRef> out = actorContext.findLocalShard("default");

                    assertEquals(shardActorRef, out.get());


                    expectNoMsg();
                }
            };
        }};

    }

    @Test
    public void testFindLocalShardWithShardNotFound(){
        new JavaTestKit(getSystem()) {{
            ActorRef shardManagerActorRef = getSystem()
                    .actorOf(MockShardManager.props(false, null));

            ActorContext actorContext =
                    new ActorContext(getSystem(), shardManagerActorRef , mock(ClusterWrapper.class),
                            mock(Configuration.class));

            Optional<ActorRef> out = actorContext.findLocalShard("default");
            assertTrue(!out.isPresent());
        }};

    }

    @Test
    public void testExecuteRemoteOperation() {
        new JavaTestKit(getSystem()) {{
            ActorRef shardActorRef = getSystem().actorOf(Props.create(EchoActor.class));

            ActorRef shardManagerActorRef = getSystem()
                    .actorOf(MockShardManager.props(true, shardActorRef));

            ActorContext actorContext =
                    new ActorContext(getSystem(), shardManagerActorRef , mock(ClusterWrapper.class),
                            mock(Configuration.class));

            ActorSelection actor = actorContext.actorSelection(shardActorRef.path());

            Object out = actorContext.executeOperation(actor, "hello");

            assertEquals("hello", out);
        }};
    }

    @Test
    public void testExecuteRemoteOperationAsync() {
        new JavaTestKit(getSystem()) {{
            ActorRef shardActorRef = getSystem().actorOf(Props.create(EchoActor.class));

            ActorRef shardManagerActorRef = getSystem()
                    .actorOf(MockShardManager.props(true, shardActorRef));

            ActorContext actorContext =
                    new ActorContext(getSystem(), shardManagerActorRef , mock(ClusterWrapper.class),
                            mock(Configuration.class));

            ActorSelection actor = actorContext.actorSelection(shardActorRef.path());

            Future<Object> future = actorContext.executeOperationAsync(actor, "hello");

            try {
                Object result = Await.result(future, Duration.create(3, TimeUnit.SECONDS));
                assertEquals("Result", "hello", result);
            } catch(Exception e) {
                throw new AssertionError(e);
            }
        }};
    }

    @Test
    public void testIsPathLocal() {
        MockClusterWrapper clusterWrapper = new MockClusterWrapper();
        ActorContext actorContext = null;

        actorContext = new ActorContext(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertEquals(false, actorContext.isPathLocal(null));
        assertEquals(false, actorContext.isPathLocal(""));

        clusterWrapper.setSelfAddress(null);
        actorContext = new ActorContext(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertEquals(false, actorContext.isPathLocal(""));

        // even if the path is in local format, match the primary path (first 3 elements) and return true
        clusterWrapper.setSelfAddress(new Address("akka", "test"));
        actorContext = new ActorContext(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertEquals(true, actorContext.isPathLocal("akka://test/user/$a"));

        clusterWrapper.setSelfAddress(new Address("akka", "test"));
        actorContext = new ActorContext(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertEquals(true, actorContext.isPathLocal("akka://test/user/$a"));

        clusterWrapper.setSelfAddress(new Address("akka", "test"));
        actorContext = new ActorContext(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertEquals(true, actorContext.isPathLocal("akka://test/user/token2/token3/$a"));

        // self address of remote format,but Tx path local format.
        clusterWrapper.setSelfAddress(new Address("akka.tcp", "system", "127.0.0.1", 2550));
        actorContext = new ActorContext(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertEquals(true, actorContext.isPathLocal(
            "akka://system/user/shardmanager/shard/transaction"));

        // self address of local format,but Tx path remote format.
        clusterWrapper.setSelfAddress(new Address("akka.tcp", "system"));
        actorContext = new ActorContext(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertEquals(false, actorContext.isPathLocal(
            "akka://system@127.0.0.1:2550/user/shardmanager/shard/transaction"));

        //local path but not same
        clusterWrapper.setSelfAddress(new Address("akka", "test"));
        actorContext = new ActorContext(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertEquals(true, actorContext.isPathLocal("akka://test1/user/$a"));

        //ip and port same
        clusterWrapper.setSelfAddress(new Address("akka.tcp", "system", "127.0.0.1", 2550));
        actorContext = new ActorContext(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertEquals(true, actorContext.isPathLocal("akka.tcp://system@127.0.0.1:2550/"));

        // forward-slash missing in address
        clusterWrapper.setSelfAddress(new Address("akka.tcp", "system", "127.0.0.1", 2550));
        actorContext = new ActorContext(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertEquals(false, actorContext.isPathLocal("akka.tcp://system@127.0.0.1:2550"));

        //ips differ
        clusterWrapper.setSelfAddress(new Address("akka.tcp", "system", "127.0.0.1", 2550));
        actorContext = new ActorContext(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertEquals(false, actorContext.isPathLocal("akka.tcp://system@127.1.0.1:2550/"));

        //ports differ
        clusterWrapper.setSelfAddress(new Address("akka.tcp", "system", "127.0.0.1", 2550));
        actorContext = new ActorContext(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertEquals(false, actorContext.isPathLocal("akka.tcp://system@127.0.0.1:2551/"));
    }

    @Test
    public void testClientDispatcherIsGlobalDispatcher(){
        ActorContext actorContext =
                new ActorContext(getSystem(), mock(ActorRef.class), mock(ClusterWrapper.class),
                        mock(Configuration.class), DatastoreContext.newBuilder().build(), new PrimaryShardInfoFutureCache());

        assertEquals(getSystem().dispatchers().defaultGlobalDispatcher(), actorContext.getClientDispatcher());

    }

    @Test
    public void testClientDispatcherIsNotGlobalDispatcher(){
        ActorSystem actorSystem = ActorSystem.create("with-custom-dispatchers", ConfigFactory.load("application-with-custom-dispatchers.conf"));

        ActorContext actorContext =
                new ActorContext(actorSystem, mock(ActorRef.class), mock(ClusterWrapper.class),
                        mock(Configuration.class), DatastoreContext.newBuilder().build(), new PrimaryShardInfoFutureCache());

        assertNotEquals(actorSystem.dispatchers().defaultGlobalDispatcher(), actorContext.getClientDispatcher());

        actorSystem.shutdown();

    }

    @Test
    public void testSetDatastoreContext() {
        new JavaTestKit(getSystem()) {{
            ActorContext actorContext = new ActorContext(getSystem(), getRef(), mock(ClusterWrapper.class),
                            mock(Configuration.class), DatastoreContext.newBuilder().
                                operationTimeoutInSeconds(5).shardTransactionCommitTimeoutInSeconds(7).build(), new PrimaryShardInfoFutureCache());

            assertEquals("getOperationDuration", 5, actorContext.getOperationDuration().toSeconds());
            assertEquals("getTransactionCommitOperationTimeout", 7,
                    actorContext.getTransactionCommitOperationTimeout().duration().toSeconds());

            DatastoreContext newContext = DatastoreContext.newBuilder().operationTimeoutInSeconds(6).
                    shardTransactionCommitTimeoutInSeconds(8).build();

            DatastoreContextFactory mockContextFactory = mock(DatastoreContextFactory.class);
            Mockito.doReturn(newContext).when(mockContextFactory).getBaseDatastoreContext();

            actorContext.setDatastoreContext(mockContextFactory);

            expectMsgClass(duration("5 seconds"), DatastoreContextFactory.class);

            Assert.assertSame("getDatastoreContext", newContext, actorContext.getDatastoreContext());

            assertEquals("getOperationDuration", 6, actorContext.getOperationDuration().toSeconds());
            assertEquals("getTransactionCommitOperationTimeout", 8,
                    actorContext.getTransactionCommitOperationTimeout().duration().toSeconds());
        }};
    }

    @Test
    public void testFindPrimaryShardAsyncRemotePrimaryFound() throws Exception {

            TestActorRef<MessageCollectorActor> shardManager =
                    TestActorRef.create(getSystem(), Props.create(MessageCollectorActor.class));

            DatastoreContext dataStoreContext = DatastoreContext.newBuilder().
                    logicalStoreType(LogicalDatastoreType.CONFIGURATION).
                    shardLeaderElectionTimeout(100, TimeUnit.MILLISECONDS).build();

            final String expPrimaryPath = "akka://test-system/find-primary-shard";
            final short expPrimaryVersion = DataStoreVersions.CURRENT_VERSION;
            ActorContext actorContext =
                    new ActorContext(getSystem(), shardManager, mock(ClusterWrapper.class),
                            mock(Configuration.class), dataStoreContext, new PrimaryShardInfoFutureCache()) {
                        @Override
                        protected Future<Object> doAsk(ActorRef actorRef, Object message, Timeout timeout) {
                            return Futures.successful((Object) new RemotePrimaryShardFound(expPrimaryPath, expPrimaryVersion));
                        }
                    };

            Future<PrimaryShardInfo> foobar = actorContext.findPrimaryShardAsync("foobar");
            PrimaryShardInfo actual = Await.result(foobar, Duration.apply(5000, TimeUnit.MILLISECONDS));

            assertNotNull(actual);
            assertEquals("LocalShardDataTree present", false, actual.getLocalShardDataTree().isPresent());
            assertTrue("Unexpected PrimaryShardActor path " + actual.getPrimaryShardActor().path(),
                    expPrimaryPath.endsWith(actual.getPrimaryShardActor().pathString()));
            assertEquals("getPrimaryShardVersion", expPrimaryVersion, actual.getPrimaryShardVersion());

            Future<PrimaryShardInfo> cached = actorContext.getPrimaryShardInfoCache().getIfPresent("foobar");

            PrimaryShardInfo cachedInfo = Await.result(cached, FiniteDuration.apply(1, TimeUnit.MILLISECONDS));

            assertEquals(cachedInfo, actual);

            actorContext.getPrimaryShardInfoCache().remove("foobar");

            cached = actorContext.getPrimaryShardInfoCache().getIfPresent("foobar");

            assertNull(cached);
    }

    @Test
    public void testFindPrimaryShardAsyncLocalPrimaryFound() throws Exception {

            TestActorRef<MessageCollectorActor> shardManager =
                    TestActorRef.create(getSystem(), Props.create(MessageCollectorActor.class));

            DatastoreContext dataStoreContext = DatastoreContext.newBuilder().
                    logicalStoreType(LogicalDatastoreType.CONFIGURATION).
                    shardLeaderElectionTimeout(100, TimeUnit.MILLISECONDS).build();

            final DataTree mockDataTree = Mockito.mock(DataTree.class);
            final String expPrimaryPath = "akka://test-system/find-primary-shard";
            ActorContext actorContext =
                    new ActorContext(getSystem(), shardManager, mock(ClusterWrapper.class),
                            mock(Configuration.class), dataStoreContext, new PrimaryShardInfoFutureCache()) {
                        @Override
                        protected Future<Object> doAsk(ActorRef actorRef, Object message, Timeout timeout) {
                            return Futures.successful((Object) new LocalPrimaryShardFound(expPrimaryPath, mockDataTree));
                        }
                    };

            Future<PrimaryShardInfo> foobar = actorContext.findPrimaryShardAsync("foobar");
            PrimaryShardInfo actual = Await.result(foobar, Duration.apply(5000, TimeUnit.MILLISECONDS));

            assertNotNull(actual);
            assertEquals("LocalShardDataTree present", true, actual.getLocalShardDataTree().isPresent());
            assertSame("LocalShardDataTree", mockDataTree, actual.getLocalShardDataTree().get());
            assertTrue("Unexpected PrimaryShardActor path " + actual.getPrimaryShardActor().path(),
                    expPrimaryPath.endsWith(actual.getPrimaryShardActor().pathString()));
            assertEquals("getPrimaryShardVersion", DataStoreVersions.CURRENT_VERSION, actual.getPrimaryShardVersion());

            Future<PrimaryShardInfo> cached = actorContext.getPrimaryShardInfoCache().getIfPresent("foobar");

            PrimaryShardInfo cachedInfo = Await.result(cached, FiniteDuration.apply(1, TimeUnit.MILLISECONDS));

            assertEquals(cachedInfo, actual);

            actorContext.getPrimaryShardInfoCache().remove("foobar");

            cached = actorContext.getPrimaryShardInfoCache().getIfPresent("foobar");

            assertNull(cached);
    }

    @Test
    public void testFindPrimaryShardAsyncPrimaryNotFound() throws Exception {
        testFindPrimaryExceptions(new PrimaryNotFoundException("not found"));
    }

    @Test
    public void testFindPrimaryShardAsyncActorNotInitialized() throws Exception {
        testFindPrimaryExceptions(new NotInitializedException("not initialized"));
    }

    private void testFindPrimaryExceptions(final Object expectedException) throws Exception {
        TestActorRef<MessageCollectorActor> shardManager =
            TestActorRef.create(getSystem(), Props.create(MessageCollectorActor.class));

        DatastoreContext dataStoreContext = DatastoreContext.newBuilder().
            logicalStoreType(LogicalDatastoreType.CONFIGURATION).
            shardLeaderElectionTimeout(100, TimeUnit.MILLISECONDS).build();

        ActorContext actorContext =
            new ActorContext(getSystem(), shardManager, mock(ClusterWrapper.class),
                mock(Configuration.class), dataStoreContext, new PrimaryShardInfoFutureCache()) {
                @Override
                protected Future<Object> doAsk(ActorRef actorRef, Object message, Timeout timeout) {
                    return Futures.successful(expectedException);
                }
            };

        Future<PrimaryShardInfo> foobar = actorContext.findPrimaryShardAsync("foobar");

        try {
            Await.result(foobar, Duration.apply(100, TimeUnit.MILLISECONDS));
            fail("Expected" + expectedException.getClass().toString());
        } catch(Exception e){
            if(!expectedException.getClass().isInstance(e)) {
                fail("Expected Exception of type " + expectedException.getClass().toString());
            }
        }

        Future<PrimaryShardInfo> cached = actorContext.getPrimaryShardInfoCache().getIfPresent("foobar");

        assertNull(cached);
    }

    @Test
    public void testBroadcast() {
        new JavaTestKit(getSystem()) {{
            ActorRef shardActorRef1 = getSystem().actorOf(Props.create(MessageCollectorActor.class));
            ActorRef shardActorRef2 = getSystem().actorOf(Props.create(MessageCollectorActor.class));

            TestActorRef<MockShardManager> shardManagerActorRef = TestActorRef.create(getSystem(), MockShardManager.props());
            MockShardManager shardManagerActor = shardManagerActorRef.underlyingActor();
            shardManagerActor.addFindPrimaryResp("shard1", new RemotePrimaryShardFound(shardActorRef1.path().toString(),
                    DataStoreVersions.CURRENT_VERSION));
            shardManagerActor.addFindPrimaryResp("shard2", new RemotePrimaryShardFound(shardActorRef2.path().toString(),
                    DataStoreVersions.CURRENT_VERSION));
            shardManagerActor.addFindPrimaryResp("shard3", new NoShardLeaderException("not found"));

            Configuration mockConfig = mock(Configuration.class);
            doReturn(Sets.newLinkedHashSet(Arrays.asList("shard1", "shard2", "shard3"))).
                    when(mockConfig).getAllShardNames();

            ActorContext actorContext = new ActorContext(getSystem(), shardManagerActorRef,
                    mock(ClusterWrapper.class), mockConfig,
                    DatastoreContext.newBuilder().shardInitializationTimeout(200, TimeUnit.MILLISECONDS).build(), new PrimaryShardInfoFutureCache());

            actorContext.broadcast(new Function<Short, Object>() {
                @Override
                public Object apply(Short v) {
                    return new TestMessage();
                }
            });

            MessageCollectorActor.expectFirstMatching(shardActorRef1, TestMessage.class);
            MessageCollectorActor.expectFirstMatching(shardActorRef2, TestMessage.class);
        }};
    }

}
