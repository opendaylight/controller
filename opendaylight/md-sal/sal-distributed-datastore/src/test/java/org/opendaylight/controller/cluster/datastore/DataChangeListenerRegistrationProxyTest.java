/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import java.util.concurrent.TimeUnit;
import akka.actor.ActorRef;
import akka.actor.Terminated;
import akka.dispatch.Futures;
import akka.testkit.JavaTestKit;
import akka.util.Timeout;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.cluster.datastore.messages.CloseDataChangeListenerRegistration;
import org.opendaylight.controller.cluster.datastore.messages.FindLocalShard;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardFound;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListenerReply;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.Uninterruptibles;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doAnswer;

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

    @SuppressWarnings("unchecked")
    @Test
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

    @SuppressWarnings("unchecked")
    @Test
    public void testCloseBeforeRegistration() {
        new JavaTestKit(getSystem()) {{
            ActorContext actorContext = mock(ActorContext.class);

            String shardName = "shard-1";
            final DataChangeListenerRegistrationProxy proxy = new DataChangeListenerRegistrationProxy(
                    shardName, actorContext, mockListener);

            doReturn(getSystem()).when(actorContext).getActorSystem();
            doReturn(getSystem().actorSelection(getRef().path())).
                    when(actorContext).actorSelection(getRef().path());
            doReturn(duration("5 seconds")).when(actorContext).getOperationDuration();
            doReturn(Optional.of(getRef())).when(actorContext).findLocalShard(shardName);

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
