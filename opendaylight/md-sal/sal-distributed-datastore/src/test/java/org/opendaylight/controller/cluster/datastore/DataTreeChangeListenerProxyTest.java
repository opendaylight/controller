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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
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
import java.util.concurrent.TimeUnit;
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
import scala.concurrent.duration.FiniteDuration;

public class DataTreeChangeListenerProxyTest extends AbstractActorTest {
    private final DOMDataTreeChangeListener mockListener = mock(DOMDataTreeChangeListener.class);

    @Test(timeout = 10000)
    public void testSuccessfulRegistration() {
        final TestKit kit = new TestKit(getSystem());
        ActorContext actorContext = new ActorContext(getSystem(), kit.getRef(), mock(ClusterWrapper.class),
            mock(Configuration.class));

        final YangInstanceIdentifier path = YangInstanceIdentifier.of(TestModel.TEST_QNAME);
        final DataTreeChangeListenerProxy<DOMDataTreeChangeListener> proxy = new DataTreeChangeListenerProxy<>(
                actorContext, mockListener, path);

        new Thread(() -> proxy.init("shard-1")).start();

        FiniteDuration timeout = kit.duration("5 seconds");
        FindLocalShard findLocalShard = kit.expectMsgClass(timeout, FindLocalShard.class);
        assertEquals("getShardName", "shard-1", findLocalShard.getShardName());

        kit.reply(new LocalShardFound(kit.getRef()));

        RegisterDataTreeChangeListener registerMsg = kit.expectMsgClass(timeout,
            RegisterDataTreeChangeListener.class);
        assertEquals("getPath", path, registerMsg.getPath());
        assertFalse("isRegisterOnAllInstances", registerMsg.isRegisterOnAllInstances());

        kit.reply(new RegisterDataTreeNotificationListenerReply(kit.getRef()));

        for (int i = 0; i < 20 * 5 && proxy.getListenerRegistrationActor() == null; i++) {
            Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
        }

        assertEquals("getListenerRegistrationActor", getSystem().actorSelection(kit.getRef().path()),
            proxy.getListenerRegistrationActor());

        kit.watch(proxy.getDataChangeListenerActor());

        proxy.close();

        // The listener registration actor should get a Close message
        kit.expectMsgClass(timeout, CloseDataTreeNotificationListenerRegistration.class);

        // The DataChangeListener actor should be terminated
        kit.expectMsgClass(timeout, Terminated.class);

        proxy.close();

        kit.expectNoMessage();
    }

    @Test(timeout = 10000)
    public void testSuccessfulRegistrationForClusteredListener() {
        final TestKit kit = new TestKit(getSystem());
        ActorContext actorContext = new ActorContext(getSystem(), kit.getRef(), mock(ClusterWrapper.class),
            mock(Configuration.class));

        ClusteredDOMDataTreeChangeListener mockClusteredListener = mock(
            ClusteredDOMDataTreeChangeListener.class);

        final YangInstanceIdentifier path = YangInstanceIdentifier.of(TestModel.TEST_QNAME);
        final DataTreeChangeListenerProxy<ClusteredDOMDataTreeChangeListener> proxy =
                new DataTreeChangeListenerProxy<>(actorContext, mockClusteredListener, path);

        new Thread(() -> proxy.init("shard-1")).start();

        FiniteDuration timeout = kit.duration("5 seconds");
        FindLocalShard findLocalShard = kit.expectMsgClass(timeout, FindLocalShard.class);
        assertEquals("getShardName", "shard-1", findLocalShard.getShardName());

        kit.reply(new LocalShardFound(kit.getRef()));

        RegisterDataTreeChangeListener registerMsg = kit.expectMsgClass(timeout,
            RegisterDataTreeChangeListener.class);
        assertEquals("getPath", path, registerMsg.getPath());
        assertTrue("isRegisterOnAllInstances", registerMsg.isRegisterOnAllInstances());

        proxy.close();
    }

    @Test(timeout = 10000)
    public void testLocalShardNotFound() {
        final TestKit kit = new TestKit(getSystem());
        ActorContext actorContext = new ActorContext(getSystem(), kit.getRef(), mock(ClusterWrapper.class),
            mock(Configuration.class));

        final YangInstanceIdentifier path = YangInstanceIdentifier.of(TestModel.TEST_QNAME);
        final DataTreeChangeListenerProxy<DOMDataTreeChangeListener> proxy = new DataTreeChangeListenerProxy<>(
                actorContext, mockListener, path);

        new Thread(() -> proxy.init("shard-1")).start();

        FiniteDuration timeout = kit.duration("5 seconds");
        FindLocalShard findLocalShard = kit.expectMsgClass(timeout, FindLocalShard.class);
        assertEquals("getShardName", "shard-1", findLocalShard.getShardName());

        kit.reply(new LocalShardNotFound("shard-1"));

        kit.expectNoMessage(kit.duration("1 seconds"));

        proxy.close();
    }

    @Test(timeout = 10000)
    public void testLocalShardNotInitialized() {
        final TestKit kit = new TestKit(getSystem());
        ActorContext actorContext = new ActorContext(getSystem(), kit.getRef(), mock(ClusterWrapper.class),
            mock(Configuration.class));

        final YangInstanceIdentifier path = YangInstanceIdentifier.of(TestModel.TEST_QNAME);
        final DataTreeChangeListenerProxy<DOMDataTreeChangeListener> proxy = new DataTreeChangeListenerProxy<>(
                actorContext, mockListener, path);

        new Thread(() -> proxy.init("shard-1")).start();

        FiniteDuration timeout = kit.duration("5 seconds");
        FindLocalShard findLocalShard = kit.expectMsgClass(timeout, FindLocalShard.class);
        assertEquals("getShardName", "shard-1", findLocalShard.getShardName());

        kit.reply(new NotInitializedException("not initialized"));

        kit.within(kit.duration("1 seconds"), () ->  {
            kit.expectNoMessage();
            return null;
        });

        proxy.close();
    }

    @Test
    public void testFailedRegistration() {
        final TestKit kit = new TestKit(getSystem());
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

        doReturn(kit.duration("5 seconds")).when(actorContext).getOperationDuration();
        doReturn(Futures.successful(kit.getRef())).when(actorContext).findLocalShardAsync(eq(shardName));
        doReturn(Futures.failed(new RuntimeException("mock"))).when(actorContext).executeOperationAsync(
            any(ActorRef.class), any(Object.class), any(Timeout.class));
        doReturn(mock(DatastoreContext.class)).when(actorContext).getDatastoreContext();

        proxy.init("shard-1");

        assertEquals("getListenerRegistrationActor", null, proxy.getListenerRegistrationActor());

        proxy.close();
    }

    @Test
    public void testCloseBeforeRegistration() {
        final TestKit kit = new TestKit(getSystem());
        ActorContext actorContext = mock(ActorContext.class);

        String shardName = "shard-1";

        doReturn(DatastoreContext.newBuilder().build()).when(actorContext).getDatastoreContext();
        doReturn(getSystem().dispatchers().defaultGlobalDispatcher()).when(actorContext).getClientDispatcher();
        doReturn(getSystem()).when(actorContext).getActorSystem();
        doReturn(Dispatchers.DEFAULT_DISPATCHER_PATH).when(actorContext).getNotificationDispatcherPath();
        doReturn(getSystem().actorSelection(kit.getRef().path())).when(actorContext).actorSelection(
            kit.getRef().path());
        doReturn(kit.duration("5 seconds")).when(actorContext).getOperationDuration();
        doReturn(Futures.successful(kit.getRef())).when(actorContext).findLocalShardAsync(eq(shardName));

        final DataTreeChangeListenerProxy<DOMDataTreeChangeListener> proxy = new DataTreeChangeListenerProxy<>(
                actorContext, mockListener, YangInstanceIdentifier.of(TestModel.TEST_QNAME));

        Answer<Future<Object>> answer = invocation -> {
            proxy.close();
            return Futures.successful((Object) new RegisterDataTreeNotificationListenerReply(kit.getRef()));
        };

        doAnswer(answer).when(actorContext).executeOperationAsync(any(ActorRef.class), any(Object.class),
            any(Timeout.class));

        proxy.init(shardName);

        kit.expectMsgClass(kit.duration("5 seconds"), CloseDataTreeNotificationListenerRegistration.class);

        assertEquals("getListenerRegistrationActor", null, proxy.getListenerRegistrationActor());
    }
}
