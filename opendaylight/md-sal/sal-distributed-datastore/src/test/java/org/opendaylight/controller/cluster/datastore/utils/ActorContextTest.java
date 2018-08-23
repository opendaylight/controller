/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
import akka.actor.UntypedAbstractActor;
import akka.dispatch.Futures;
import akka.japi.Creator;
import akka.testkit.TestActorRef;
import akka.testkit.javadsl.TestKit;
import akka.util.Timeout;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.typesafe.config.ConfigFactory;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

public class ActorContextTest extends AbstractActorTest {

    static final Logger LOG = LoggerFactory.getLogger(ActorContextTest.class);

    private static class TestMessage {
    }

    private static final class MockShardManager extends UntypedAbstractActor {

        private final boolean found;
        private final ActorRef actorRef;
        private final Map<String,Object> findPrimaryResponses = Maps.newHashMap();

        private MockShardManager(final boolean found, final ActorRef actorRef) {

            this.found = found;
            this.actorRef = actorRef;
        }

        @Override public void onReceive(final Object message) {
            if (message instanceof FindPrimary) {
                FindPrimary fp = (FindPrimary)message;
                Object resp = findPrimaryResponses.get(fp.getShardName());
                if (resp == null) {
                    LOG.error("No expected FindPrimary response found for shard name {}", fp.getShardName());
                } else {
                    getSender().tell(resp, getSelf());
                }

                return;
            }

            if (found) {
                getSender().tell(new LocalShardFound(actorRef), getSelf());
            } else {
                getSender().tell(new LocalShardNotFound(((FindLocalShard) message).getShardName()), getSelf());
            }
        }

        void addFindPrimaryResp(final String shardName, final Object resp) {
            findPrimaryResponses.put(shardName, resp);
        }

        private static Props props(final boolean found, final ActorRef actorRef) {
            return Props.create(new MockShardManagerCreator(found, actorRef));
        }

        private static Props props() {
            return Props.create(new MockShardManagerCreator());
        }

        @SuppressWarnings("serial")
        private static class MockShardManagerCreator implements Creator<MockShardManager> {
            final boolean found;
            final ActorRef actorRef;

            MockShardManagerCreator() {
                this.found = false;
                this.actorRef = null;
            }

            MockShardManagerCreator(final boolean found, final ActorRef actorRef) {
                this.found = found;
                this.actorRef = actorRef;
            }

            @Override
            public MockShardManager create() {
                return new MockShardManager(found, actorRef);
            }
        }
    }

    @Test
    public void testFindLocalShardWithShardFound() {
        final TestKit testKit = new TestKit(getSystem());
        testKit.within(testKit.duration("1 seconds"), () -> {
            ActorRef shardActorRef = getSystem().actorOf(Props.create(EchoActor.class));

            ActorRef shardManagerActorRef = getSystem().actorOf(MockShardManager.props(true, shardActorRef));

            ActorContext actorContext = new ActorContext(getSystem(), shardManagerActorRef,
                mock(ClusterWrapper.class), mock(Configuration.class));

            Optional<ActorRef> out = actorContext.findLocalShard("default");

            assertEquals(shardActorRef, out.get());

            testKit.expectNoMessage();
            return null;
        });
    }

    @Test
    public void testFindLocalShardWithShardNotFound() {
        ActorRef shardManagerActorRef = getSystem().actorOf(MockShardManager.props(false, null));

        ActorContext actorContext = new ActorContext(getSystem(), shardManagerActorRef, mock(ClusterWrapper.class),
            mock(Configuration.class));

        Optional<ActorRef> out = actorContext.findLocalShard("default");
        assertFalse(out.isPresent());
    }

    @Test
    public void testExecuteRemoteOperation() {
        ActorRef shardActorRef = getSystem().actorOf(Props.create(EchoActor.class));

        ActorRef shardManagerActorRef = getSystem().actorOf(MockShardManager.props(true, shardActorRef));

        ActorContext actorContext = new ActorContext(getSystem(), shardManagerActorRef,
            mock(ClusterWrapper.class), mock(Configuration.class));

        ActorSelection actor = actorContext.actorSelection(shardActorRef.path());

        Object out = actorContext.executeOperation(actor, "hello");

        assertEquals("hello", out);
    }

    @Test
    public void testExecuteRemoteOperationAsync() throws Exception {
        ActorRef shardActorRef = getSystem().actorOf(Props.create(EchoActor.class));

        ActorRef shardManagerActorRef = getSystem().actorOf(MockShardManager.props(true, shardActorRef));

        ActorContext actorContext = new ActorContext(getSystem(), shardManagerActorRef,
            mock(ClusterWrapper.class), mock(Configuration.class));

        ActorSelection actor = actorContext.actorSelection(shardActorRef.path());

        Future<Object> future = actorContext.executeOperationAsync(actor, "hello");

        Object result = Await.result(future, Duration.create(3, TimeUnit.SECONDS));
        assertEquals("Result", "hello", result);
    }

    @Test
    public void testIsPathLocal() {
        MockClusterWrapper clusterWrapper = new MockClusterWrapper();
        ActorContext actorContext = null;

        actorContext = new ActorContext(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertFalse(actorContext.isPathLocal(null));
        assertFalse(actorContext.isPathLocal(""));

        clusterWrapper.setSelfAddress(null);
        actorContext = new ActorContext(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertFalse(actorContext.isPathLocal(""));

        // even if the path is in local format, match the primary path (first 3 elements) and return true
        clusterWrapper.setSelfAddress(new Address("akka", "test"));
        actorContext = new ActorContext(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertTrue(actorContext.isPathLocal("akka://test/user/$a"));

        clusterWrapper.setSelfAddress(new Address("akka", "test"));
        actorContext = new ActorContext(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertTrue(actorContext.isPathLocal("akka://test/user/$a"));

        clusterWrapper.setSelfAddress(new Address("akka", "test"));
        actorContext = new ActorContext(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertTrue(actorContext.isPathLocal("akka://test/user/token2/token3/$a"));

        // self address of remote format,but Tx path local format.
        clusterWrapper.setSelfAddress(new Address("akka", "system", "127.0.0.1", 2550));
        actorContext = new ActorContext(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertTrue(actorContext.isPathLocal("akka://system/user/shardmanager/shard/transaction"));

        // self address of local format,but Tx path remote format.
        clusterWrapper.setSelfAddress(new Address("akka", "system"));
        actorContext = new ActorContext(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertFalse(actorContext.isPathLocal("akka://system@127.0.0.1:2550/user/shardmanager/shard/transaction"));

        //local path but not same
        clusterWrapper.setSelfAddress(new Address("akka", "test"));
        actorContext = new ActorContext(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertTrue(actorContext.isPathLocal("akka://test1/user/$a"));

        //ip and port same
        clusterWrapper.setSelfAddress(new Address("akka", "system", "127.0.0.1", 2550));
        actorContext = new ActorContext(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertTrue(actorContext.isPathLocal("akka://system@127.0.0.1:2550/"));

        // forward-slash missing in address
        clusterWrapper.setSelfAddress(new Address("akka", "system", "127.0.0.1", 2550));
        actorContext = new ActorContext(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertFalse(actorContext.isPathLocal("akka://system@127.0.0.1:2550"));

        //ips differ
        clusterWrapper.setSelfAddress(new Address("akka", "system", "127.0.0.1", 2550));
        actorContext = new ActorContext(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertFalse(actorContext.isPathLocal("akka://system@127.1.0.1:2550/"));

        //ports differ
        clusterWrapper.setSelfAddress(new Address("akka", "system", "127.0.0.1", 2550));
        actorContext = new ActorContext(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertFalse(actorContext.isPathLocal("akka://system@127.0.0.1:2551/"));
    }

    @Test
    public void testClientDispatcherIsGlobalDispatcher() {
        ActorContext actorContext = new ActorContext(getSystem(), mock(ActorRef.class), mock(ClusterWrapper.class),
                mock(Configuration.class), DatastoreContext.newBuilder().build(), new PrimaryShardInfoFutureCache());

        assertEquals(getSystem().dispatchers().defaultGlobalDispatcher(), actorContext.getClientDispatcher());
    }

    @Test
    public void testClientDispatcherIsNotGlobalDispatcher() {
        ActorSystem actorSystem = ActorSystem.create("with-custom-dispatchers",
                ConfigFactory.load("application-with-custom-dispatchers.conf"));

        ActorContext actorContext = new ActorContext(actorSystem, mock(ActorRef.class), mock(ClusterWrapper.class),
                mock(Configuration.class), DatastoreContext.newBuilder().build(), new PrimaryShardInfoFutureCache());

        assertNotEquals(actorSystem.dispatchers().defaultGlobalDispatcher(), actorContext.getClientDispatcher());

        actorSystem.terminate();
    }

    @Test
    public void testSetDatastoreContext() {
        final TestKit testKit = new TestKit(getSystem());
        ActorContext actorContext = new ActorContext(getSystem(), testKit.getRef(),
            mock(ClusterWrapper.class), mock(Configuration.class), DatastoreContext.newBuilder()
            .operationTimeoutInSeconds(5).shardTransactionCommitTimeoutInSeconds(7).build(),
            new PrimaryShardInfoFutureCache());

        assertEquals("getOperationDuration", 5, actorContext.getOperationDuration().toSeconds());
        assertEquals("getTransactionCommitOperationTimeout", 7,
            actorContext.getTransactionCommitOperationTimeout().duration().toSeconds());

        DatastoreContext newContext = DatastoreContext.newBuilder().operationTimeoutInSeconds(6)
                .shardTransactionCommitTimeoutInSeconds(8).build();

        DatastoreContextFactory mockContextFactory = mock(DatastoreContextFactory.class);
        Mockito.doReturn(newContext).when(mockContextFactory).getBaseDatastoreContext();

        actorContext.setDatastoreContext(mockContextFactory);

        testKit.expectMsgClass(testKit.duration("5 seconds"), DatastoreContextFactory.class);

        Assert.assertSame("getDatastoreContext", newContext, actorContext.getDatastoreContext());

        assertEquals("getOperationDuration", 6, actorContext.getOperationDuration().toSeconds());
        assertEquals("getTransactionCommitOperationTimeout", 8,
            actorContext.getTransactionCommitOperationTimeout().duration().toSeconds());
    }

    @Test
    public void testFindPrimaryShardAsyncRemotePrimaryFound() throws Exception {

        ActorRef shardManager = getSystem().actorOf(MessageCollectorActor.props());

        DatastoreContext dataStoreContext = DatastoreContext.newBuilder()
                .logicalStoreType(LogicalDatastoreType.CONFIGURATION)
                .shardLeaderElectionTimeout(100, TimeUnit.MILLISECONDS).build();

        final String expPrimaryPath = "akka://test-system/find-primary-shard";
        final short expPrimaryVersion = DataStoreVersions.CURRENT_VERSION;
        ActorContext actorContext = new ActorContext(getSystem(), shardManager, mock(ClusterWrapper.class),
                mock(Configuration.class), dataStoreContext, new PrimaryShardInfoFutureCache()) {
            @Override
            protected Future<Object> doAsk(final ActorRef actorRef, final Object message, final Timeout timeout) {
                return Futures.successful((Object) new RemotePrimaryShardFound(expPrimaryPath, expPrimaryVersion));
            }
        };

        Future<PrimaryShardInfo> foobar = actorContext.findPrimaryShardAsync("foobar");
        PrimaryShardInfo actual = Await.result(foobar, Duration.apply(5000, TimeUnit.MILLISECONDS));

        assertNotNull(actual);
        assertFalse("LocalShardDataTree present", actual.getLocalShardDataTree().isPresent());
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

        ActorRef shardManager = getSystem().actorOf(MessageCollectorActor.props());

        DatastoreContext dataStoreContext = DatastoreContext.newBuilder()
                .logicalStoreType(LogicalDatastoreType.CONFIGURATION)
                .shardLeaderElectionTimeout(100, TimeUnit.MILLISECONDS).build();

        final DataTree mockDataTree = Mockito.mock(DataTree.class);
        final String expPrimaryPath = "akka://test-system/find-primary-shard";
        ActorContext actorContext = new ActorContext(getSystem(), shardManager, mock(ClusterWrapper.class),
                mock(Configuration.class), dataStoreContext, new PrimaryShardInfoFutureCache()) {
            @Override
            protected Future<Object> doAsk(final ActorRef actorRef, final Object message, final Timeout timeout) {
                return Futures.successful((Object) new LocalPrimaryShardFound(expPrimaryPath, mockDataTree));
            }
        };

        Future<PrimaryShardInfo> foobar = actorContext.findPrimaryShardAsync("foobar");
        PrimaryShardInfo actual = Await.result(foobar, Duration.apply(5000, TimeUnit.MILLISECONDS));

        assertNotNull(actual);
        assertTrue("LocalShardDataTree present", actual.getLocalShardDataTree().isPresent());
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
    public void testFindPrimaryShardAsyncPrimaryNotFound() {
        testFindPrimaryExceptions(new PrimaryNotFoundException("not found"));
    }

    @Test
    public void testFindPrimaryShardAsyncActorNotInitialized() {
        testFindPrimaryExceptions(new NotInitializedException("not initialized"));
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private static void testFindPrimaryExceptions(final Object expectedException) {
        ActorRef shardManager = getSystem().actorOf(MessageCollectorActor.props());

        DatastoreContext dataStoreContext = DatastoreContext.newBuilder()
                .logicalStoreType(LogicalDatastoreType.CONFIGURATION)
                .shardLeaderElectionTimeout(100, TimeUnit.MILLISECONDS).build();

        ActorContext actorContext =
            new ActorContext(getSystem(), shardManager, mock(ClusterWrapper.class),
                mock(Configuration.class), dataStoreContext, new PrimaryShardInfoFutureCache()) {
                @Override
                protected Future<Object> doAsk(final ActorRef actorRef, final Object message, final Timeout timeout) {
                    return Futures.successful(expectedException);
                }
            };

        Future<PrimaryShardInfo> foobar = actorContext.findPrimaryShardAsync("foobar");

        try {
            Await.result(foobar, Duration.apply(100, TimeUnit.MILLISECONDS));
            fail("Expected" + expectedException.getClass().toString());
        } catch (Exception e) {
            if (!expectedException.getClass().isInstance(e)) {
                fail("Expected Exception of type " + expectedException.getClass().toString());
            }
        }

        Future<PrimaryShardInfo> cached = actorContext.getPrimaryShardInfoCache().getIfPresent("foobar");

        assertNull(cached);
    }

    @Test
    public void testBroadcast() {
        ActorRef shardActorRef1 = getSystem().actorOf(MessageCollectorActor.props());
        ActorRef shardActorRef2 = getSystem().actorOf(MessageCollectorActor.props());

        TestActorRef<MockShardManager> shardManagerActorRef = TestActorRef.create(getSystem(),
            MockShardManager.props());
        MockShardManager shardManagerActor = shardManagerActorRef.underlyingActor();
        shardManagerActor.addFindPrimaryResp("shard1", new RemotePrimaryShardFound(
            shardActorRef1.path().toString(), DataStoreVersions.CURRENT_VERSION));
        shardManagerActor.addFindPrimaryResp("shard2", new RemotePrimaryShardFound(
            shardActorRef2.path().toString(), DataStoreVersions.CURRENT_VERSION));
        shardManagerActor.addFindPrimaryResp("shard3", new NoShardLeaderException("not found"));

        Configuration mockConfig = mock(Configuration.class);
        doReturn(Sets.newLinkedHashSet(Arrays.asList("shard1", "shard2", "shard3"))).when(mockConfig)
        .getAllShardNames();

        ActorContext actorContext = new ActorContext(getSystem(), shardManagerActorRef,
            mock(ClusterWrapper.class), mockConfig,
            DatastoreContext.newBuilder().shardInitializationTimeout(200, TimeUnit.MILLISECONDS).build(),
            new PrimaryShardInfoFutureCache());

        actorContext.broadcast(v -> new TestMessage(), TestMessage.class);

        MessageCollectorActor.expectFirstMatching(shardActorRef1, TestMessage.class);
        MessageCollectorActor.expectFirstMatching(shardActorRef2, TestMessage.class);
    }
}
