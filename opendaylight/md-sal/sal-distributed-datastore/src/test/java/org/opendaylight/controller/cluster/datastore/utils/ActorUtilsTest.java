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
import static org.mockito.Mockito.mock;

import com.typesafe.config.ConfigFactory;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Address;
import org.apache.pekko.actor.Props;
import org.apache.pekko.actor.UntypedAbstractActor;
import org.apache.pekko.dispatch.Futures;
import org.apache.pekko.japi.Creator;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.apache.pekko.util.Timeout;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.cluster.datastore.AbstractActorTest;
import org.opendaylight.controller.cluster.datastore.ClusterWrapper;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.DatastoreContextFactory;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.exceptions.NotInitializedException;
import org.opendaylight.controller.cluster.datastore.exceptions.PrimaryNotFoundException;
import org.opendaylight.controller.cluster.datastore.messages.FindLocalShard;
import org.opendaylight.controller.cluster.datastore.messages.FindPrimary;
import org.opendaylight.controller.cluster.datastore.messages.LocalPrimaryShardFound;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardFound;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardNotFound;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;
import org.opendaylight.controller.cluster.datastore.messages.RemotePrimaryShardFound;
import org.opendaylight.controller.cluster.raft.EchoActor;
import org.opendaylight.controller.cluster.raft.MessageCollectorActor;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.data.tree.api.DataTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

public class ActorUtilsTest extends AbstractActorTest {
    static final Logger LOG = LoggerFactory.getLogger(ActorUtilsTest.class);

    private static final class MockShardManager extends UntypedAbstractActor {
        private final Map<String,Object> findPrimaryResponses = new HashMap<>();
        private final boolean found;
        private final ActorRef actorRef;

        private MockShardManager(final boolean found, final ActorRef actorRef) {

            this.found = found;
            this.actorRef = actorRef;
        }

        @Override
        @Deprecated(since = "11.0.0", forRemoval = true)
        public ActorRef getSender() {
            return super.getSender();
        }

        @Override
        public void onReceive(final Object message) {
            if (message instanceof FindPrimary fp) {
                Object resp = findPrimaryResponses.get(fp.getShardName());
                if (resp == null) {
                    LOG.error("No expected FindPrimary response found for shard name {}", fp.getShardName());
                } else {
                    getSender().tell(resp, self());
                }

                return;
            }

            if (found) {
                getSender().tell(new LocalShardFound(actorRef), self());
            } else {
                getSender().tell(new LocalShardNotFound(((FindLocalShard) message).shardName()), self());
            }
        }

        void addFindPrimaryResp(final String shardName, final Object resp) {
            findPrimaryResponses.put(shardName, resp);
        }

        private static Props props(final boolean found, final ActorRef actorRef) {
            return Props.create(MockShardManager.class, new MockShardManagerCreator(found, actorRef));
        }

        private static Props props() {
            return Props.create(MockShardManager.class, new MockShardManagerCreator());
        }

        @SuppressWarnings("serial")
        private static class MockShardManagerCreator implements Creator<MockShardManager> {
            final boolean found;
            final ActorRef actorRef;

            MockShardManagerCreator() {
                found = false;
                actorRef = null;
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
        testKit.within(Duration.ofSeconds(1), () -> {
            ActorRef shardActorRef = getSystem().actorOf(Props.create(EchoActor.class));

            ActorRef shardManagerActorRef = getSystem().actorOf(MockShardManager.props(true, shardActorRef));

            ActorUtils actorUtils = new ActorUtils(getSystem(), shardManagerActorRef,
                mock(ClusterWrapper.class), mock(Configuration.class));

            assertEquals(Optional.of(shardActorRef), actorUtils.findLocalShard("default"));

            testKit.expectNoMessage();
            return null;
        });
    }

    @Test
    public void testFindLocalShardWithShardNotFound() {
        ActorRef shardManagerActorRef = getSystem().actorOf(MockShardManager.props(false, null));

        ActorUtils actorUtils = new ActorUtils(getSystem(), shardManagerActorRef, mock(ClusterWrapper.class),
            mock(Configuration.class));

        Optional<ActorRef> out = actorUtils.findLocalShard("default");
        assertFalse(out.isPresent());
    }

    @Test
    public void testExecuteRemoteOperation() {
        ActorRef shardActorRef = getSystem().actorOf(Props.create(EchoActor.class));

        ActorRef shardManagerActorRef = getSystem().actorOf(MockShardManager.props(true, shardActorRef));

        ActorUtils actorUtils = new ActorUtils(getSystem(), shardManagerActorRef,
            mock(ClusterWrapper.class), mock(Configuration.class));

        ActorSelection actor = actorUtils.actorSelection(shardActorRef.path());

        Object out = actorUtils.executeOperation(actor, "hello");

        assertEquals("hello", out);
    }

    @Test
    public void testExecuteRemoteOperationAsync() throws Exception {
        ActorRef shardActorRef = getSystem().actorOf(Props.create(EchoActor.class));

        ActorRef shardManagerActorRef = getSystem().actorOf(MockShardManager.props(true, shardActorRef));

        ActorUtils actorUtils = new ActorUtils(getSystem(), shardManagerActorRef,
            mock(ClusterWrapper.class), mock(Configuration.class));

        ActorSelection actor = actorUtils.actorSelection(shardActorRef.path());

        Future<Object> future = actorUtils.executeOperationAsync(actor, "hello");

        Object result = Await.result(future, FiniteDuration.create(3, TimeUnit.SECONDS));
        assertEquals("Result", "hello", result);
    }

    @Test
    public void testIsPathLocal() {
        MockClusterWrapper clusterWrapper = new MockClusterWrapper();
        ActorUtils actorUtils = null;

        actorUtils = new ActorUtils(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertFalse(actorUtils.isPathLocal(null));
        assertFalse(actorUtils.isPathLocal(""));

        clusterWrapper.setSelfAddress(null);
        actorUtils = new ActorUtils(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertFalse(actorUtils.isPathLocal(""));

        // even if the path is in local format, match the primary path (first 3 elements) and return true
        clusterWrapper.setSelfAddress(new Address("akka", "test"));
        actorUtils = new ActorUtils(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertTrue(actorUtils.isPathLocal("pekko://test/user/$a"));

        clusterWrapper.setSelfAddress(new Address("akka", "test"));
        actorUtils = new ActorUtils(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertTrue(actorUtils.isPathLocal("pekko://test/user/$a"));

        clusterWrapper.setSelfAddress(new Address("akka", "test"));
        actorUtils = new ActorUtils(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertTrue(actorUtils.isPathLocal("pekko://test/user/token2/token3/$a"));

        // self address of remote format,but Tx path local format.
        clusterWrapper.setSelfAddress(new Address("akka", "system", "127.0.0.1", 2550));
        actorUtils = new ActorUtils(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertTrue(actorUtils.isPathLocal("pekko://system/user/shardmanager/shard/transaction"));

        // self address of local format,but Tx path remote format.
        clusterWrapper.setSelfAddress(new Address("akka", "system"));
        actorUtils = new ActorUtils(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertFalse(actorUtils.isPathLocal("pekko://system@127.0.0.1:2550/user/shardmanager/shard/transaction"));

        //local path but not same
        clusterWrapper.setSelfAddress(new Address("akka", "test"));
        actorUtils = new ActorUtils(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertTrue(actorUtils.isPathLocal("pekko://test1/user/$a"));

        //ip and port same
        clusterWrapper.setSelfAddress(new Address("akka", "system", "127.0.0.1", 2550));
        actorUtils = new ActorUtils(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertTrue(actorUtils.isPathLocal("pekko://system@127.0.0.1:2550/"));

        // forward-slash missing in address
        clusterWrapper.setSelfAddress(new Address("akka", "system", "127.0.0.1", 2550));
        actorUtils = new ActorUtils(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertFalse(actorUtils.isPathLocal("pekko://system@127.0.0.1:2550"));

        //ips differ
        clusterWrapper.setSelfAddress(new Address("akka", "system", "127.0.0.1", 2550));
        actorUtils = new ActorUtils(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertFalse(actorUtils.isPathLocal("pekko://system@127.1.0.1:2550/"));

        //ports differ
        clusterWrapper.setSelfAddress(new Address("akka", "system", "127.0.0.1", 2550));
        actorUtils = new ActorUtils(getSystem(), null, clusterWrapper, mock(Configuration.class));
        assertFalse(actorUtils.isPathLocal("pekko://system@127.0.0.1:2551/"));
    }

    @Test
    public void testClientDispatcherIsGlobalDispatcher() {
        ActorUtils actorUtils = new ActorUtils(getSystem(), mock(ActorRef.class), mock(ClusterWrapper.class),
                mock(Configuration.class), DatastoreContext.newBuilder().build(), new PrimaryShardInfoFutureCache());

        assertEquals(getSystem().dispatchers().defaultGlobalDispatcher(), actorUtils.getClientDispatcher());
    }

    @Test
    public void testClientDispatcherIsNotGlobalDispatcher() {
        ActorSystem actorSystem = ActorSystem.create("with-custom-dispatchers",
                ConfigFactory.load("application-with-custom-dispatchers.conf"));

        ActorUtils actorUtils = new ActorUtils(actorSystem, mock(ActorRef.class), mock(ClusterWrapper.class),
                mock(Configuration.class), DatastoreContext.newBuilder().build(), new PrimaryShardInfoFutureCache());

        assertNotEquals(actorSystem.dispatchers().defaultGlobalDispatcher(), actorUtils.getClientDispatcher());

        actorSystem.terminate();
    }

    @Test
    public void testSetDatastoreContext() {
        final TestKit testKit = new TestKit(getSystem());
        ActorUtils actorUtils = new ActorUtils(getSystem(), testKit.getRef(),
            mock(ClusterWrapper.class), mock(Configuration.class), DatastoreContext.newBuilder()
            .operationTimeoutInSeconds(5).shardTransactionCommitTimeoutInSeconds(7).build(),
            new PrimaryShardInfoFutureCache());

        assertEquals("getOperationDuration", 5, actorUtils.getOperationDuration().toSeconds());

        DatastoreContext newContext = DatastoreContext.newBuilder().operationTimeoutInSeconds(6)
                .shardTransactionCommitTimeoutInSeconds(8).build();

        DatastoreContextFactory mockContextFactory = mock(DatastoreContextFactory.class);
        Mockito.doReturn(newContext).when(mockContextFactory).getBaseDatastoreContext();

        actorUtils.setDatastoreContext(mockContextFactory);

        testKit.expectMsgClass(Duration.ofSeconds(5), DatastoreContextFactory.class);

        Assert.assertSame("getDatastoreContext", newContext, actorUtils.getDatastoreContext());

        assertEquals("getOperationDuration", 6, actorUtils.getOperationDuration().toSeconds());
    }

    @Test
    public void testFindPrimaryShardAsyncRemotePrimaryFound() throws Exception {

        ActorRef shardManager = getSystem().actorOf(MessageCollectorActor.props());

        DatastoreContext dataStoreContext = DatastoreContext.newBuilder()
                .logicalStoreType(LogicalDatastoreType.CONFIGURATION)
                .shardLeaderElectionTimeout(100, TimeUnit.MILLISECONDS).build();

        final String expPrimaryPath = "pekko://test-system/find-primary-shard";
        final short expPrimaryVersion = DataStoreVersions.CURRENT_VERSION;
        ActorUtils actorUtils = new ActorUtils(getSystem(), shardManager, mock(ClusterWrapper.class),
                mock(Configuration.class), dataStoreContext, new PrimaryShardInfoFutureCache()) {
            @Override
            protected Future<Object> doAsk(final ActorRef actorRef, final Object message, final Timeout timeout) {
                return Futures.successful((Object) new RemotePrimaryShardFound(expPrimaryPath, expPrimaryVersion));
            }
        };

        Future<PrimaryShardInfo> foobar = actorUtils.findPrimaryShardAsync("foobar");
        PrimaryShardInfo actual = Await.result(foobar, FiniteDuration.apply(5000, TimeUnit.MILLISECONDS));

        assertNotNull(actual);
        assertNull("LocalShardDataTree present", actual.getLocalShardDataTree());
        assertTrue("Unexpected PrimaryShardActor path " + actual.getPrimaryShardActor().path(),
                expPrimaryPath.endsWith(actual.getPrimaryShardActor().pathString()));
        assertEquals("getPrimaryShardVersion", expPrimaryVersion, actual.getPrimaryShardVersion());

        Future<PrimaryShardInfo> cached = actorUtils.getPrimaryShardInfoCache().getIfPresent("foobar");

        PrimaryShardInfo cachedInfo = Await.result(cached, FiniteDuration.apply(1, TimeUnit.MILLISECONDS));

        assertEquals(cachedInfo, actual);

        actorUtils.getPrimaryShardInfoCache().remove("foobar");

        cached = actorUtils.getPrimaryShardInfoCache().getIfPresent("foobar");

        assertNull(cached);
    }

    @Test
    public void testFindPrimaryShardAsyncLocalPrimaryFound() throws Exception {

        ActorRef shardManager = getSystem().actorOf(MessageCollectorActor.props());

        DatastoreContext dataStoreContext = DatastoreContext.newBuilder()
                .logicalStoreType(LogicalDatastoreType.CONFIGURATION)
                .shardLeaderElectionTimeout(100, TimeUnit.MILLISECONDS).build();

        final DataTree mockDataTree = Mockito.mock(DataTree.class);
        final String expPrimaryPath = "pekko://test-system/find-primary-shard";
        ActorUtils actorUtils = new ActorUtils(getSystem(), shardManager, mock(ClusterWrapper.class),
                mock(Configuration.class), dataStoreContext, new PrimaryShardInfoFutureCache()) {
            @Override
            protected Future<Object> doAsk(final ActorRef actorRef, final Object message, final Timeout timeout) {
                return Futures.successful((Object) new LocalPrimaryShardFound(expPrimaryPath, mockDataTree));
            }
        };

        Future<PrimaryShardInfo> foobar = actorUtils.findPrimaryShardAsync("foobar");
        PrimaryShardInfo actual = Await.result(foobar, FiniteDuration.apply(5000, TimeUnit.MILLISECONDS));

        assertNotNull(actual);
        assertSame("LocalShardDataTree", mockDataTree, actual.getLocalShardDataTree());
        assertTrue("Unexpected PrimaryShardActor path " + actual.getPrimaryShardActor().path(),
                expPrimaryPath.endsWith(actual.getPrimaryShardActor().pathString()));
        assertEquals("getPrimaryShardVersion", DataStoreVersions.CURRENT_VERSION, actual.getPrimaryShardVersion());

        Future<PrimaryShardInfo> cached = actorUtils.getPrimaryShardInfoCache().getIfPresent("foobar");

        PrimaryShardInfo cachedInfo = Await.result(cached, FiniteDuration.apply(1, TimeUnit.MILLISECONDS));

        assertEquals(cachedInfo, actual);

        actorUtils.getPrimaryShardInfoCache().remove("foobar");

        cached = actorUtils.getPrimaryShardInfoCache().getIfPresent("foobar");

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

        ActorUtils actorUtils = new ActorUtils(getSystem(), shardManager, mock(ClusterWrapper.class),
                mock(Configuration.class), dataStoreContext, new PrimaryShardInfoFutureCache()) {
            @Override
            protected Future<Object> doAsk(final ActorRef actorRef, final Object message, final Timeout timeout) {
                return Futures.successful(expectedException);
            }
        };

        Future<PrimaryShardInfo> foobar = actorUtils.findPrimaryShardAsync("foobar");

        try {
            Await.result(foobar, FiniteDuration.apply(100, TimeUnit.MILLISECONDS));
            fail("Expected" + expectedException.getClass().toString());
        } catch (Exception e) {
            if (!expectedException.getClass().isInstance(e)) {
                fail("Expected Exception of type " + expectedException.getClass().toString());
            }
        }

        Future<PrimaryShardInfo> cached = actorUtils.getPrimaryShardInfoCache().getIfPresent("foobar");

        assertNull(cached);
    }
}
