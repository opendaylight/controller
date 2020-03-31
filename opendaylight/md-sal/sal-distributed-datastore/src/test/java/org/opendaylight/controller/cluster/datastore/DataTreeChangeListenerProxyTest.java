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
import static org.mockito.Mockito.when;

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
import org.opendaylight.controller.cluster.datastore.shardstrategy.ModuleShardStrategy;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategyFactory;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.controller.cluster.raft.utils.DoNothingActor;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.ClusteredDOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.Future;

public class DataTreeChangeListenerProxyTest extends AbstractActorTest {

    private static final String TEST_SHARD = "shard-1";
    private static final String TEST_MODULE = TestModel.TEST_QNAME.getLocalName();

    private final DOMDataTreeChangeListener mockListener = mock(DOMDataTreeChangeListener.class);

    private Configuration mockConfiguration;

    @Before
    public void init() {
        mockConfiguration = mock(Configuration.class);
        when(mockConfiguration.getModuleNameFromNameSpace(TestModel.TEST_QNAME.getNamespace().toASCIIString()))
                .thenReturn(TEST_MODULE);
        when(mockConfiguration.getShardNameForModule(TEST_MODULE)).thenReturn(TEST_SHARD);
        when(mockConfiguration.getStrategyForModule(TEST_MODULE)).thenReturn(ShardStrategyFactory
                .newShardStrategyInstance(TEST_MODULE, ModuleShardStrategy.NAME, mockConfiguration));
    }

    @Test(timeout = 10000)
    public void testSuccessfulRegistration() {
        final TestKit kit = new TestKit(getSystem());
        ActorUtils actorUtils = new ActorUtils(getSystem(), kit.getRef(), mock(ClusterWrapper.class),
                mockConfiguration);

        final YangInstanceIdentifier path = YangInstanceIdentifier.of(TestModel.TEST_QNAME);
        final DataTreeChangeListenerProxy<DOMDataTreeChangeListener> proxy =
                DataTreeChangeListenerProxy.create(actorUtils, mockListener, path);

        new Thread(() -> proxy.init()).start();

        Duration timeout = Duration.ofSeconds(5);
        FindLocalShard findLocalShard = kit.expectMsgClass(timeout, FindLocalShard.class);
        assertEquals("getShardName", TEST_SHARD, findLocalShard.getShardName());

        kit.reply(new LocalShardFound(kit.getRef()));

        RegisterDataTreeChangeListener registerMsg = kit.expectMsgClass(timeout,
            RegisterDataTreeChangeListener.class);
        assertEquals("getPath", path, registerMsg.getPath());
        assertFalse("isRegisterOnAllInstances", registerMsg.isRegisterOnAllInstances());

        kit.reply(new RegisterDataTreeNotificationListenerReply(kit.getRef()));

        for (int i = 0; i < 20 * 5 && ((DataTreeChangeListenerProxy.DataTreeChangeListenerSingleShardProxy)proxy)
                .getListenerRegistrationActor() == null; i++) {
            Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
        }

        assertEquals("getListenerRegistrationActor", getSystem().actorSelection(kit.getRef().path()),
                ((DataTreeChangeListenerProxy.DataTreeChangeListenerSingleShardProxy)proxy)
                        .getListenerRegistrationActor());

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
        ActorUtils actorUtils = new ActorUtils(getSystem(), kit.getRef(), mock(ClusterWrapper.class),
            mockConfiguration);

        ClusteredDOMDataTreeChangeListener mockClusteredListener = mock(
            ClusteredDOMDataTreeChangeListener.class);

        final YangInstanceIdentifier path = YangInstanceIdentifier.of(TestModel.TEST_QNAME);
        final DataTreeChangeListenerProxy<ClusteredDOMDataTreeChangeListener> proxy =
                DataTreeChangeListenerProxy.create(actorUtils, mockClusteredListener, path);

        new Thread(() -> proxy.init()).start();

        Duration timeout = Duration.ofSeconds(5);
        FindLocalShard findLocalShard = kit.expectMsgClass(timeout, FindLocalShard.class);
        assertEquals("getShardName", TEST_SHARD, findLocalShard.getShardName());

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
        ActorUtils actorUtils = new ActorUtils(getSystem(), kit.getRef(), mock(ClusterWrapper.class),
            mockConfiguration);

        final YangInstanceIdentifier path = YangInstanceIdentifier.of(TestModel.TEST_QNAME);
        final DataTreeChangeListenerProxy<DOMDataTreeChangeListener> proxy = DataTreeChangeListenerProxy.create(
                actorUtils, mockListener, path);

        new Thread(() -> proxy.init()).start();

        Duration timeout = Duration.ofSeconds(5);
        FindLocalShard findLocalShard = kit.expectMsgClass(timeout, FindLocalShard.class);
        assertEquals("getShardName", TEST_SHARD, findLocalShard.getShardName());

        kit.reply(new LocalShardNotFound(TEST_SHARD));

        kit.expectNoMessage(Duration.ofSeconds(1));

        proxy.close();
    }

    @Test(timeout = 10000)
    public void testLocalShardNotInitialized() {
        final TestKit kit = new TestKit(getSystem());
        ActorUtils actorUtils = new ActorUtils(getSystem(), kit.getRef(), mock(ClusterWrapper.class),
            mockConfiguration);

        final YangInstanceIdentifier path = YangInstanceIdentifier.of(TestModel.TEST_QNAME);
        final DataTreeChangeListenerProxy<DOMDataTreeChangeListener> proxy = DataTreeChangeListenerProxy.create(
                actorUtils, mockListener, path);

        new Thread(() -> proxy.init()).start();

        Duration timeout = Duration.ofSeconds(5);
        FindLocalShard findLocalShard = kit.expectMsgClass(timeout, FindLocalShard.class);
        assertEquals("getShardName", TEST_SHARD, findLocalShard.getShardName());

        kit.reply(new NotInitializedException("not initialized"));

        kit.within(Duration.ofSeconds(1), () ->  {
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

        ActorUtils actorUtils = mock(ActorUtils.class);
        final YangInstanceIdentifier path = YangInstanceIdentifier.of(TestModel.TEST_QNAME);
        doReturn(new ShardStrategyFactory(mockConfiguration, LogicalDatastoreType.CONFIGURATION))
                .when(actorUtils).getShardStrategyFactory();
        doReturn(executor).when(actorUtils).getClientDispatcher();
        doReturn(DatastoreContext.newBuilder().build()).when(actorUtils).getDatastoreContext();
        doReturn(mockActorSystem).when(actorUtils).getActorSystem();

        String shardName = TEST_SHARD;
        final DataTreeChangeListenerProxy<DOMDataTreeChangeListener> proxy = DataTreeChangeListenerProxy.create(
                actorUtils, mockListener, path);

        doReturn(kit.duration("5 seconds")).when(actorUtils).getOperationDuration();
        doReturn(Futures.successful(kit.getRef())).when(actorUtils).findLocalShardAsync(eq(shardName));
        doReturn(Futures.failed(new RuntimeException("mock"))).when(actorUtils).executeOperationAsync(
            any(ActorRef.class), any(Object.class), any(Timeout.class));
        doReturn(mock(DatastoreContext.class)).when(actorUtils).getDatastoreContext();

        proxy.init();

        assertEquals("getListenerRegistrationActor", null,
                ((DataTreeChangeListenerProxy.DataTreeChangeListenerSingleShardProxy)proxy)
                        .getListenerRegistrationActor());

        proxy.close();
    }

    @Test
    public void testCloseBeforeRegistration() {
        final TestKit kit = new TestKit(getSystem());
        ActorUtils actorUtils = mock(ActorUtils.class);

        String shardName = TEST_SHARD;

        doReturn(new ShardStrategyFactory(mockConfiguration, LogicalDatastoreType.CONFIGURATION))
                .when(actorUtils).getShardStrategyFactory();
        doReturn(DatastoreContext.newBuilder().build()).when(actorUtils).getDatastoreContext();
        doReturn(getSystem().dispatchers().defaultGlobalDispatcher()).when(actorUtils).getClientDispatcher();
        doReturn(getSystem()).when(actorUtils).getActorSystem();
        doReturn(Dispatchers.DEFAULT_DISPATCHER_PATH).when(actorUtils).getNotificationDispatcherPath();
        doReturn(getSystem().actorSelection(kit.getRef().path())).when(actorUtils).actorSelection(
            kit.getRef().path());
        doReturn(kit.duration("5 seconds")).when(actorUtils).getOperationDuration();
        doReturn(Futures.successful(kit.getRef())).when(actorUtils).findLocalShardAsync(eq(shardName));

        final DataTreeChangeListenerProxy<DOMDataTreeChangeListener> proxy = DataTreeChangeListenerProxy.create(
                actorUtils, mockListener, YangInstanceIdentifier.of(TestModel.TEST_QNAME));

        Answer<Future<Object>> answer = invocation -> {
            proxy.close();
            return Futures.successful((Object) new RegisterDataTreeNotificationListenerReply(kit.getRef()));
        };

        doAnswer(answer).when(actorUtils).executeOperationAsync(any(ActorRef.class), any(Object.class),
            any(Timeout.class));

        proxy.init();

        kit.expectMsgClass(Duration.ofSeconds(5), CloseDataTreeNotificationListenerRegistration.class);

        assertEquals("getListenerRegistrationActor", null,
                ((DataTreeChangeListenerProxy.DataTreeChangeListenerSingleShardProxy)proxy)
                        .getListenerRegistrationActor());
    }
}
