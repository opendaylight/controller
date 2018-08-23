/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.dispatch.ExecutionContexts;
import akka.dispatch.Futures;
import akka.testkit.javadsl.TestKit;
import akka.util.Timeout;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Uninterruptibles;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.cluster.common.actor.Dispatchers;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.exceptions.NotInitializedException;
import org.opendaylight.controller.cluster.datastore.messages.CloseDataTreeNotificationListenerRegistration;
import org.opendaylight.controller.cluster.datastore.messages.FindLocalShard;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardFound;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardNotFound;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeNotificationListenerReply;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.cluster.raft.utils.DoNothingActor;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.mdsal.dom.api.ClusteredDOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.Future;

public class DataTreeChangeListenerProxyTest extends AbstractActorTest {
    private final DOMDataTreeChangeListener mockListener = mock(DOMDataTreeChangeListener.class);

    private TestKit testKit;

    @Before
    public void before() {
        testKit = new TestKit(getSystem());
    }

    @Test(timeout = 10000)
    public void testSuccessfulRegistration() {
        ActorContext actorContext = new ActorContext(getSystem(), testKit.getRef(), mock(ClusterWrapper.class),
            mock(Configuration.class));

        final YangInstanceIdentifier path = YangInstanceIdentifier.of(TestModel.TEST_QNAME);
        final DataTreeChangeListenerProxy<DOMDataTreeChangeListener> proxy = new DataTreeChangeListenerProxy<>(
                actorContext, mockListener, path);

        new Thread(() -> proxy.init("shard-1")).start();

        Duration timeout = Duration.ofSeconds(5);
        FindLocalShard findLocalShard = testKit.expectMsgClass(timeout, FindLocalShard.class);
        assertEquals("getShardName", "shard-1", findLocalShard.getShardName());

        testKit.reply(new LocalShardFound(testKit.getRef()));

        RegisterDataTreeChangeListener registerMsg = testKit.expectMsgClass(timeout,
            RegisterDataTreeChangeListener.class);
        assertEquals("getPath", path, registerMsg.getPath());
        assertFalse("isRegisterOnAllInstances", registerMsg.isRegisterOnAllInstances());

        testKit.reply(new RegisterDataTreeNotificationListenerReply(testKit.getRef()));

        for (int i = 0; i < 20 * 5 && proxy.getListenerRegistrationActor() == null; i++) {
            Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
        }

        assertEquals("getListenerRegistrationActor", getSystem().actorSelection(testKit.getRef().path()),
            proxy.getListenerRegistrationActor());

        testKit.watch(proxy.getDataChangeListenerActor());

        proxy.close();

        // The listener registration actor should get a Close message
        testKit.expectMsgClass(timeout, CloseDataTreeNotificationListenerRegistration.class);

        // The DataChangeListener actor should be terminated
        testKit.expectMsgClass(timeout, Terminated.class);

        proxy.close();

        testKit.expectNoMessage();
    }

    @Test(timeout = 10000)
    public void testSuccessfulRegistrationForClusteredListener() {
        ActorContext actorContext = new ActorContext(getSystem(), testKit.getRef(), mock(ClusterWrapper.class),
            mock(Configuration.class));

        ClusteredDOMDataTreeChangeListener mockClusteredListener = mock(
            ClusteredDOMDataTreeChangeListener.class);

        final YangInstanceIdentifier path = YangInstanceIdentifier.of(TestModel.TEST_QNAME);
        final DataTreeChangeListenerProxy<ClusteredDOMDataTreeChangeListener> proxy =
                new DataTreeChangeListenerProxy<>(actorContext, mockClusteredListener, path);

        new Thread(() -> proxy.init("shard-1")).start();

        Duration timeout = Duration.ofSeconds(5);
        FindLocalShard findLocalShard = testKit.expectMsgClass(timeout, FindLocalShard.class);
        assertEquals("getShardName", "shard-1", findLocalShard.getShardName());

        testKit.reply(new LocalShardFound(testKit.getRef()));

        RegisterDataTreeChangeListener registerMsg = testKit.expectMsgClass(timeout,
            RegisterDataTreeChangeListener.class);
        assertEquals("getPath", path, registerMsg.getPath());
        assertTrue("isRegisterOnAllInstances", registerMsg.isRegisterOnAllInstances());

        proxy.close();
    }

    @Test(timeout = 10000)
    public void testLocalShardNotFound() {
        ActorContext actorContext = new ActorContext(getSystem(), testKit.getRef(), mock(ClusterWrapper.class),
            mock(Configuration.class));

        final YangInstanceIdentifier path = YangInstanceIdentifier.of(TestModel.TEST_QNAME);
        final DataTreeChangeListenerProxy<DOMDataTreeChangeListener> proxy = new DataTreeChangeListenerProxy<>(
                actorContext, mockListener, path);

        new Thread(() -> proxy.init("shard-1")).start();

        FindLocalShard findLocalShard = testKit.expectMsgClass(Duration.ofSeconds(5), FindLocalShard.class);
        assertEquals("getShardName", "shard-1", findLocalShard.getShardName());

        testKit.reply(new LocalShardNotFound("shard-1"));

        testKit.expectNoMessage(Duration.ofSeconds(1));

        proxy.close();
    }

    @Test(timeout = 10000)
    public void testLocalShardNotInitialized() {
        ActorContext actorContext = new ActorContext(getSystem(), testKit.getRef(), mock(ClusterWrapper.class),
            mock(Configuration.class));

        final YangInstanceIdentifier path = YangInstanceIdentifier.of(TestModel.TEST_QNAME);
        final DataTreeChangeListenerProxy<DOMDataTreeChangeListener> proxy = new DataTreeChangeListenerProxy<>(
                actorContext, mockListener, path);

        new Thread(() -> proxy.init("shard-1")).start();

        FindLocalShard findLocalShard = testKit.expectMsgClass(Duration.ofSeconds(5), FindLocalShard.class);
        assertEquals("getShardName", "shard-1", findLocalShard.getShardName());

        testKit.reply(new NotInitializedException("not initialized"));

        testKit.within(Duration.ofSeconds(1), () ->  {
            testKit.expectNoMessage();
            return null;
        });

        proxy.close();
    }

    @Test
    public void testFailedRegistration() {
        ActorSystem mockActorSystem = mock(ActorSystem.class);

        ActorRef mockActor = getSystem().actorOf(Props.create(DoNothingActor.class), "testFailedRegistration");
        doReturn(mockActor).when(mockActorSystem).actorOf(any(Props.class));
        ExecutionContextExecutor executor = ExecutionContexts.fromExecutor(MoreExecutors.directExecutor());

        ActorContext actorContext = mock(ActorContext.class);
        final YangInstanceIdentifier path = YangInstanceIdentifier.of(TestModel.TEST_QNAME);

        doReturn(executor).when(actorContext).getClientDispatcher();
        doReturn(DatastoreContext.newBuilder().build()).when(actorContext).getDatastoreContext();
        doReturn(mockActorSystem).when(actorContext).getActorSystem();

        String shardName = "shard-1";
        final DataTreeChangeListenerProxy<DOMDataTreeChangeListener> proxy = new DataTreeChangeListenerProxy<>(
                actorContext, mockListener, path);

        doReturn(testKit.duration("5 seconds")).when(actorContext).getOperationDuration();
        doReturn(Futures.successful(testKit.getRef())).when(actorContext).findLocalShardAsync(eq(shardName));
        doReturn(Futures.failed(new RuntimeException("mock"))).when(actorContext)
        .executeOperationAsync(any(ActorRef.class), any(Object.class), any(Timeout.class));
        doReturn(mock(DatastoreContext.class)).when(actorContext).getDatastoreContext();

        proxy.init("shard-1");

        assertEquals("getListenerRegistrationActor", null, proxy.getListenerRegistrationActor());

        proxy.close();
    }

    @Test
    public void testCloseBeforeRegistration() {
        ActorContext actorContext = mock(ActorContext.class);

        String shardName = "shard-1";

        doReturn(DatastoreContext.newBuilder().build()).when(actorContext).getDatastoreContext();
        doReturn(getSystem().dispatchers().defaultGlobalDispatcher()).when(actorContext).getClientDispatcher();
        doReturn(getSystem()).when(actorContext).getActorSystem();
        doReturn(Dispatchers.DEFAULT_DISPATCHER_PATH).when(actorContext).getNotificationDispatcherPath();
        doReturn(getSystem().actorSelection(testKit.getRef().path())).when(actorContext).actorSelection(
            testKit.getRef().path());
        doReturn(testKit.duration("5 seconds")).when(actorContext).getOperationDuration();
        doReturn(Futures.successful(testKit.getRef())).when(actorContext).findLocalShardAsync(eq(shardName));

        final DataTreeChangeListenerProxy<DOMDataTreeChangeListener> proxy = new DataTreeChangeListenerProxy<>(
                actorContext, mockListener, YangInstanceIdentifier.of(TestModel.TEST_QNAME));

        Answer<Future<Object>> answer = invocation -> {
            proxy.close();
            return Futures.successful((Object) new RegisterDataTreeNotificationListenerReply(testKit.getRef()));
        };

        doAnswer(answer).when(actorContext).executeOperationAsync(any(ActorRef.class), any(Object.class),
            any(Timeout.class));

        proxy.init(shardName);

        testKit.expectMsgClass(Duration.ofSeconds(5), CloseDataTreeNotificationListenerRegistration.class);

        assertEquals("getListenerRegistrationActor", null, proxy.getListenerRegistrationActor());
    }
}
