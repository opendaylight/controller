/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

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
import akka.testkit.JavaTestKit;
import akka.util.Timeout;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.cluster.common.actor.Dispatchers;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.exceptions.NotInitializedException;
import org.opendaylight.controller.cluster.datastore.messages.CloseDataTreeChangeListenerRegistration;
import org.opendaylight.controller.cluster.datastore.messages.FindLocalShard;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardFound;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardNotFound;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeChangeListenerReply;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.cluster.raft.utils.DoNothingActor;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.md.sal.dom.api.ClusteredDOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

public class DataTreeChangeListenerProxyTest extends AbstractActorTest {
    private final DOMDataTreeChangeListener mockListener = mock(DOMDataTreeChangeListener.class);

    @Test(timeout=10000)
    public void testSuccessfulRegistration() {
        new JavaTestKit(getSystem()) {{
            ActorContext actorContext = new ActorContext(getSystem(), getRef(),
                    mock(ClusterWrapper.class), mock(Configuration.class));

            final DataTreeChangeListenerProxy<DOMDataTreeChangeListener> proxy =
                    new DataTreeChangeListenerProxy<>(actorContext, mockListener);

            final YangInstanceIdentifier path = YangInstanceIdentifier.of(TestModel.TEST_QNAME);
            new Thread() {
                @Override
                public void run() {
                    proxy.init("shard-1", path);
                }

            }.start();

            FiniteDuration timeout = duration("5 seconds");
            FindLocalShard findLocalShard = expectMsgClass(timeout, FindLocalShard.class);
            Assert.assertEquals("getShardName", "shard-1", findLocalShard.getShardName());

            reply(new LocalShardFound(getRef()));

            RegisterDataTreeChangeListener registerMsg = expectMsgClass(timeout, RegisterDataTreeChangeListener.class);
            Assert.assertEquals("getPath", path, registerMsg.getPath());
            Assert.assertEquals("isRegisterOnAllInstances", false, registerMsg.isRegisterOnAllInstances());

            reply(new RegisterDataTreeChangeListenerReply(getRef()));


            for(int i = 0; (i < 20 * 5) && proxy.getListenerRegistrationActor() == null; i++) {
                Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
            }

            Assert.assertEquals("getListenerRegistrationActor", getSystem().actorSelection(getRef().path()),
                    proxy.getListenerRegistrationActor());

            watch(proxy.getDataChangeListenerActor());

            proxy.close();

            // The listener registration actor should get a Close message
            expectMsgClass(timeout, CloseDataTreeChangeListenerRegistration.class);

            // The DataChangeListener actor should be terminated
            expectMsgClass(timeout, Terminated.class);

            proxy.close();

            expectNoMsg();
        }};
    }

    @Test(timeout=10000)
    public void testSuccessfulRegistrationForClusteredListener() {
        new JavaTestKit(getSystem()) {{
            ActorContext actorContext = new ActorContext(getSystem(), getRef(),
                    mock(ClusterWrapper.class), mock(Configuration.class));

            ClusteredDOMDataTreeChangeListener mockClusteredListener = mock(ClusteredDOMDataTreeChangeListener.class);

            final DataTreeChangeListenerProxy<ClusteredDOMDataTreeChangeListener> proxy =
                    new DataTreeChangeListenerProxy<>(actorContext, mockClusteredListener);

            final YangInstanceIdentifier path = YangInstanceIdentifier.of(TestModel.TEST_QNAME);
            new Thread() {
                @Override
                public void run() {
                    proxy.init("shard-1", path);
                }

            }.start();

            FiniteDuration timeout = duration("5 seconds");
            FindLocalShard findLocalShard = expectMsgClass(timeout, FindLocalShard.class);
            Assert.assertEquals("getShardName", "shard-1", findLocalShard.getShardName());

            reply(new LocalShardFound(getRef()));

            RegisterDataTreeChangeListener registerMsg = expectMsgClass(timeout, RegisterDataTreeChangeListener.class);
            Assert.assertEquals("getPath", path, registerMsg.getPath());
            Assert.assertEquals("isRegisterOnAllInstances", true, registerMsg.isRegisterOnAllInstances());
        }};
    }

    @Test(timeout=10000)
    public void testLocalShardNotFound() {
        new JavaTestKit(getSystem()) {{
            ActorContext actorContext = new ActorContext(getSystem(), getRef(),
                    mock(ClusterWrapper.class), mock(Configuration.class));

            final DataTreeChangeListenerProxy<DOMDataTreeChangeListener> proxy =
                    new DataTreeChangeListenerProxy<>(actorContext, mockListener);

            final YangInstanceIdentifier path = YangInstanceIdentifier.of(TestModel.TEST_QNAME);
            new Thread() {
                @Override
                public void run() {
                    proxy.init("shard-1", path);
                }

            }.start();

            FiniteDuration timeout = duration("5 seconds");
            FindLocalShard findLocalShard = expectMsgClass(timeout, FindLocalShard.class);
            Assert.assertEquals("getShardName", "shard-1", findLocalShard.getShardName());

            reply(new LocalShardNotFound("shard-1"));

            expectNoMsg(duration("1 seconds"));
        }};
    }

    @Test(timeout=10000)
    public void testLocalShardNotInitialized() {
        new JavaTestKit(getSystem()) {{
            ActorContext actorContext = new ActorContext(getSystem(), getRef(),
                    mock(ClusterWrapper.class), mock(Configuration.class));

            final DataTreeChangeListenerProxy<DOMDataTreeChangeListener> proxy =
                    new DataTreeChangeListenerProxy<>(actorContext, mockListener);

            final YangInstanceIdentifier path = YangInstanceIdentifier.of(TestModel.TEST_QNAME);
            new Thread() {
                @Override
                public void run() {
                    proxy.init("shard-1", path);
                }

            }.start();

            FiniteDuration timeout = duration("5 seconds");
            FindLocalShard findLocalShard = expectMsgClass(timeout, FindLocalShard.class);
            Assert.assertEquals("getShardName", "shard-1", findLocalShard.getShardName());

            reply(new NotInitializedException("not initialized"));

            new Within(duration("1 seconds")) {
                @Override
                protected void run() {
                    expectNoMsg();
                }
            };
        }};
    }

    @Test
    public void testFailedRegistration() {
        new JavaTestKit(getSystem()) {{
            ActorSystem mockActorSystem = mock(ActorSystem.class);

            ActorRef mockActor = getSystem().actorOf(Props.create(DoNothingActor.class),
                    "testFailedRegistration");
            doReturn(mockActor).when(mockActorSystem).actorOf(any(Props.class));
            ExecutionContextExecutor executor = ExecutionContexts.fromExecutor(
                    MoreExecutors.directExecutor());


            ActorContext actorContext = mock(ActorContext.class);
            final YangInstanceIdentifier path = YangInstanceIdentifier.of(TestModel.TEST_QNAME);

            doReturn(executor).when(actorContext).getClientDispatcher();
            doReturn(mockActorSystem).when(actorContext).getActorSystem();

            String shardName = "shard-1";
            final DataTreeChangeListenerProxy<DOMDataTreeChangeListener> proxy =
                    new DataTreeChangeListenerProxy<>(actorContext, mockListener);

            doReturn(duration("5 seconds")).when(actorContext).getOperationDuration();
            doReturn(Futures.successful(getRef())).when(actorContext).findLocalShardAsync(eq(shardName));
            doReturn(Futures.failed(new RuntimeException("mock"))).
                    when(actorContext).executeOperationAsync(any(ActorRef.class),
                    any(Object.class), any(Timeout.class));
            doReturn(mock(DatastoreContext.class)).when(actorContext).getDatastoreContext();

            proxy.init("shard-1", path);

            Assert.assertEquals("getListenerRegistrationActor", null,
                    proxy.getListenerRegistrationActor());
        }};
    }

    @Test
    public void testCloseBeforeRegistration() {
        new JavaTestKit(getSystem()) {{
            ActorContext actorContext = mock(ActorContext.class);

            String shardName = "shard-1";

            doReturn(DatastoreContext.newBuilder().build()).when(actorContext).getDatastoreContext();
            doReturn(getSystem().dispatchers().defaultGlobalDispatcher()).when(actorContext).getClientDispatcher();
            doReturn(getSystem()).when(actorContext).getActorSystem();
            doReturn(Dispatchers.DEFAULT_DISPATCHER_PATH).when(actorContext).getNotificationDispatcherPath();
            doReturn(getSystem().actorSelection(getRef().path())).
                    when(actorContext).actorSelection(getRef().path());
            doReturn(duration("5 seconds")).when(actorContext).getOperationDuration();
            doReturn(Futures.successful(getRef())).when(actorContext).findLocalShardAsync(eq(shardName));

            final DataTreeChangeListenerProxy<DOMDataTreeChangeListener> proxy =
                    new DataTreeChangeListenerProxy<>(actorContext, mockListener);


            Answer<Future<Object>> answer = new Answer<Future<Object>>() {
                @Override
                public Future<Object> answer(InvocationOnMock invocation) {
                    proxy.close();
                    return Futures.successful((Object)new RegisterDataTreeChangeListenerReply(getRef()));
                }
            };

            doAnswer(answer).when(actorContext).executeOperationAsync(any(ActorRef.class),
                    any(Object.class), any(Timeout.class));

            proxy.init(shardName, YangInstanceIdentifier.of(TestModel.TEST_QNAME));

            expectMsgClass(duration("5 seconds"), CloseDataTreeChangeListenerRegistration.class);

            Assert.assertEquals("getListenerRegistrationActor", null,
                    proxy.getListenerRegistrationActor());
        }};
    }
}
