/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
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
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.cluster.datastore.messages.ActorNotInitialized;
import org.opendaylight.controller.cluster.datastore.messages.CloseDataChangeListenerRegistration;
import org.opendaylight.controller.cluster.datastore.messages.FindLocalShard;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardFound;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardNotFound;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListenerReply;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.cluster.datastore.utils.DoNothingActor;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

/**
 * Unit tests for DataChangeListenerRegistrationProxy.
 *
 * @author Thomas Pantelis
 */
public class DataChangeListenerRegistrationProxyTest extends AbstractActorTest {

    @SuppressWarnings("unchecked")
    private final AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>> mockListener =
            Mockito.mock(AsyncDataChangeListener.class);

    @Test
    public void testGetInstance() throws Exception {
        DataChangeListenerRegistrationProxy proxy = new DataChangeListenerRegistrationProxy(
                "shard", Mockito.mock(ActorContext.class), mockListener);

        Assert.assertEquals(mockListener, proxy.getInstance());
    }

    @Test(timeout=10000)
    public void testSuccessfulRegistration() {
        new JavaTestKit(getSystem()) {{
            ActorContext actorContext = new ActorContext(getSystem(), getRef(),
                    mock(ClusterWrapper.class), mock(Configuration.class));

            final DataChangeListenerRegistrationProxy proxy = new DataChangeListenerRegistrationProxy(
                    "shard-1", actorContext, mockListener);

            final YangInstanceIdentifier path = YangInstanceIdentifier.of(TestModel.TEST_QNAME);
            final DataChangeScope scope = AsyncDataBroker.DataChangeScope.ONE;
            new Thread() {
                @Override
                public void run() {
                    proxy.init(path, scope);
                }

            }.start();

            FiniteDuration timeout = duration("5 seconds");
            FindLocalShard findLocalShard = expectMsgClass(timeout, FindLocalShard.class);
            Assert.assertEquals("getShardName", "shard-1", findLocalShard.getShardName());

            reply(new LocalShardFound(getRef()));

            RegisterChangeListener registerMsg = expectMsgClass(timeout, RegisterChangeListener.class);
            Assert.assertEquals("getPath", path, registerMsg.getPath());
            Assert.assertEquals("getScope", scope, registerMsg.getScope());

            reply(new RegisterChangeListenerReply(getRef().path()));

            for(int i = 0; (i < 20 * 5) && proxy.getListenerRegistrationActor() == null; i++) {
                Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
            }

            Assert.assertEquals("getListenerRegistrationActor", getSystem().actorSelection(getRef().path()),
                    proxy.getListenerRegistrationActor());

            watch(proxy.getDataChangeListenerActor());

            proxy.close();

            // The listener registration actor should get a Close message
            expectMsgClass(timeout, CloseDataChangeListenerRegistration.SERIALIZABLE_CLASS);

            // The DataChangeListener actor should be terminated
            expectMsgClass(timeout, Terminated.class);

            proxy.close();

            expectNoMsg();
        }};
    }

    @Test(timeout=10000)
    public void testLocalShardNotFound() {
        new JavaTestKit(getSystem()) {{
            ActorContext actorContext = new ActorContext(getSystem(), getRef(),
                    mock(ClusterWrapper.class), mock(Configuration.class));

            final DataChangeListenerRegistrationProxy proxy = new DataChangeListenerRegistrationProxy(
                    "shard-1", actorContext, mockListener);

            final YangInstanceIdentifier path = YangInstanceIdentifier.of(TestModel.TEST_QNAME);
            final DataChangeScope scope = AsyncDataBroker.DataChangeScope.ONE;
            new Thread() {
                @Override
                public void run() {
                    proxy.init(path, scope);
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

            final DataChangeListenerRegistrationProxy proxy = new DataChangeListenerRegistrationProxy(
                    "shard-1", actorContext, mockListener);

            final YangInstanceIdentifier path = YangInstanceIdentifier.of(TestModel.TEST_QNAME);
            final DataChangeScope scope = AsyncDataBroker.DataChangeScope.ONE;
            new Thread() {
                @Override
                public void run() {
                    proxy.init(path, scope);
                }

            }.start();

            FiniteDuration timeout = duration("5 seconds");
            FindLocalShard findLocalShard = expectMsgClass(timeout, FindLocalShard.class);
            Assert.assertEquals("getShardName", "shard-1", findLocalShard.getShardName());

            reply(new ActorNotInitialized());

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
                    MoreExecutors.sameThreadExecutor());
            doReturn(executor).when(mockActorSystem).dispatcher();

            ActorContext actorContext = mock(ActorContext.class);

            String shardName = "shard-1";
            final DataChangeListenerRegistrationProxy proxy = new DataChangeListenerRegistrationProxy(
                    shardName, actorContext, mockListener);

            doReturn(mockActorSystem).when(actorContext).getActorSystem();
            doReturn(duration("5 seconds")).when(actorContext).getOperationDuration();
            doReturn(Futures.successful(getRef())).when(actorContext).findLocalShardAsync(eq(shardName));
            doReturn(Futures.failed(new RuntimeException("mock"))).
                    when(actorContext).executeOperationAsync(any(ActorRef.class),
                            any(Object.class), any(Timeout.class));
            doReturn(mock(DatastoreContext.class)).when(actorContext).getDatastoreContext();

            proxy.init(YangInstanceIdentifier.of(TestModel.TEST_QNAME),
                    AsyncDataBroker.DataChangeScope.ONE);

            Assert.assertEquals("getListenerRegistrationActor", null,
                    proxy.getListenerRegistrationActor());
        }};
    }

    @Test
    public void testCloseBeforeRegistration() {
        new JavaTestKit(getSystem()) {{
            ActorContext actorContext = mock(ActorContext.class);

            String shardName = "shard-1";
            final DataChangeListenerRegistrationProxy proxy = new DataChangeListenerRegistrationProxy(
                    shardName, actorContext, mockListener);

            doReturn(DatastoreContext.newBuilder().build()).when(actorContext).getDatastoreContext();
            doReturn(getSystem()).when(actorContext).getActorSystem();
            doReturn(getSystem().actorSelection(getRef().path())).
                    when(actorContext).actorSelection(getRef().path());
            doReturn(duration("5 seconds")).when(actorContext).getOperationDuration();
            doReturn(Futures.successful(getRef())).when(actorContext).findLocalShardAsync(eq(shardName));

            Answer<Future<Object>> answer = new Answer<Future<Object>>() {
                @Override
                public Future<Object> answer(InvocationOnMock invocation) {
                    proxy.close();
                    return Futures.successful((Object)new RegisterChangeListenerReply(getRef().path()));
                }
            };

            doAnswer(answer).when(actorContext).executeOperationAsync(any(ActorRef.class),
                    any(Object.class), any(Timeout.class));

            proxy.init(YangInstanceIdentifier.of(TestModel.TEST_QNAME),
                    AsyncDataBroker.DataChangeScope.ONE);

            expectMsgClass(duration("5 seconds"), CloseDataChangeListenerRegistration.SERIALIZABLE_CLASS);

            Assert.assertEquals("getListenerRegistrationActor", null,
                    proxy.getListenerRegistrationActor());
        }};
    }
}
