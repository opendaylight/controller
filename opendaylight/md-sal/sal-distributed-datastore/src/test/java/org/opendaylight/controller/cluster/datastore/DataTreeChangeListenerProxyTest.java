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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Uninterruptibles;
import java.time.Duration;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
import org.apache.pekko.actor.Terminated;
import org.apache.pekko.dispatch.ExecutionContexts;
import org.apache.pekko.dispatch.Futures;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.controller.cluster.common.actor.Dispatchers;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.exceptions.NotInitializedException;
import org.opendaylight.controller.cluster.datastore.messages.CloseDataTreeNotificationListenerRegistration;
import org.opendaylight.controller.cluster.datastore.messages.FindLocalShard;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardFound;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardNotFound;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeNotificationListenerReply;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.controller.cluster.raft.DoNothingActor;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class DataTreeChangeListenerProxyTest extends AbstractActorTest {
    private final DOMDataTreeChangeListener mockListener = mock(DOMDataTreeChangeListener.class);

    @Test(timeout = 10000)
    public void testSuccessfulRegistration() {
        final var kit = new TestKit(getSystem());
        final var actorUtils = new ActorUtils(getSystem(), kit.getRef(), mock(ClusterWrapper.class),
            mock(Configuration.class));

        final var path = YangInstanceIdentifier.of(TestModel.TEST_QNAME);
        final var proxy = startProxyAsync(actorUtils, path, false);

        final var timeout = Duration.ofSeconds(5);
        final var findLocalShard = kit.expectMsgClass(timeout, FindLocalShard.class);
        assertEquals("shard-1", findLocalShard.getShardName());

        kit.reply(new LocalShardFound(kit.getRef()));

        final var registerMsg = kit.expectMsgClass(timeout, RegisterDataTreeChangeListener.class);
        assertEquals(path, registerMsg.getPath());
        assertFalse(registerMsg.isRegisterOnAllInstances());

        kit.reply(new RegisterDataTreeNotificationListenerReply(kit.getRef()));

        for (int i = 0; i < 20 * 5 && proxy.getListenerRegistrationActor() == null; i++) {
            Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
        }

        assertEquals(getSystem().actorSelection(kit.getRef().path()), proxy.getListenerRegistrationActor());

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
        final var kit = new TestKit(getSystem());
        final var actorUtils = new ActorUtils(getSystem(), kit.getRef(), mock(ClusterWrapper.class),
            mock(Configuration.class));

        final var path = YangInstanceIdentifier.of(TestModel.TEST_QNAME);
        final var proxy = startProxyAsync(actorUtils, path, true);

        final var timeout = Duration.ofSeconds(5);
        final var findLocalShard = kit.expectMsgClass(timeout, FindLocalShard.class);
        assertEquals("shard-1", findLocalShard.getShardName());

        kit.reply(new LocalShardFound(kit.getRef()));

        final var registerMsg = kit.expectMsgClass(timeout, RegisterDataTreeChangeListener.class);
        assertEquals(path, registerMsg.getPath());
        assertTrue(registerMsg.isRegisterOnAllInstances());

        proxy.close();
    }

    @Test(timeout = 10000)
    public void testLocalShardNotFound() {
        final var kit = new TestKit(getSystem());
        final var actorUtils = new ActorUtils(getSystem(), kit.getRef(), mock(ClusterWrapper.class),
            mock(Configuration.class));

        final var path = YangInstanceIdentifier.of(TestModel.TEST_QNAME);
        final var proxy = startProxyAsync(actorUtils, path, true);

        final var timeout = Duration.ofSeconds(5);
        final var findLocalShard = kit.expectMsgClass(timeout, FindLocalShard.class);
        assertEquals("shard-1", findLocalShard.getShardName());

        kit.reply(new LocalShardNotFound("shard-1"));

        kit.expectNoMessage(Duration.ofSeconds(1));

        proxy.close();
    }

    @Test(timeout = 10000)
    public void testLocalShardNotInitialized() {
        final var kit = new TestKit(getSystem());
        final var actorUtils = new ActorUtils(getSystem(), kit.getRef(), mock(ClusterWrapper.class),
            mock(Configuration.class));

        final var path = YangInstanceIdentifier.of(TestModel.TEST_QNAME);
        final var proxy = startProxyAsync(actorUtils, path, false);

        final var timeout = Duration.ofSeconds(5);
        final var findLocalShard = kit.expectMsgClass(timeout, FindLocalShard.class);
        assertEquals("shard-1", findLocalShard.getShardName());

        kit.reply(new NotInitializedException("not initialized"));

        kit.within(Duration.ofSeconds(1), () ->  {
            kit.expectNoMessage();
            return null;
        });

        proxy.close();
    }

    @Test
    public void testFailedRegistration() {
        final var kit = new TestKit(getSystem());
        final var mockActorSystem = mock(ActorSystem.class);

        final var mockActor = getSystem().actorOf(Props.create(DoNothingActor.class), "testFailedRegistration");
        doReturn(mockActor).when(mockActorSystem).actorOf(any(Props.class));
        final var executor = ExecutionContexts.fromExecutor(MoreExecutors.directExecutor());

        final var actorUtils = mock(ActorUtils.class);
        final var path = YangInstanceIdentifier.of(TestModel.TEST_QNAME);

        doReturn(executor).when(actorUtils).getClientDispatcher();
        doReturn(DatastoreContext.newBuilder().build()).when(actorUtils).getDatastoreContext();
        doReturn(mockActorSystem).when(actorUtils).getActorSystem();

        doReturn(kit.duration("5 seconds")).when(actorUtils).getOperationDuration();
        doReturn(Futures.successful(kit.getRef())).when(actorUtils).findLocalShardAsync("shard-1");
        doReturn(CompletableFuture.failedStage(new RuntimeException("mock"))).when(actorUtils)
            .ask(any(), any(), any());

        final var proxy = DataTreeChangeListenerProxy.of(actorUtils, mockListener, path, true, "shard-1");
        assertNull(proxy.getListenerRegistrationActor());

        proxy.close();
    }

    @Test
    public void testCloseBeforeRegistration() {
        final var kit = new TestKit(getSystem());
        final var actorUtils = mock(ActorUtils.class);

        doReturn(DatastoreContext.newBuilder().build()).when(actorUtils).getDatastoreContext();
        doReturn(getSystem().dispatchers().defaultGlobalDispatcher()).when(actorUtils).getClientDispatcher();
        doReturn(getSystem()).when(actorUtils).getActorSystem();
        doReturn(Dispatchers.DEFAULT_DISPATCHER_PATH).when(actorUtils).getNotificationDispatcherPath();
        doReturn(getSystem().actorSelection(kit.getRef().path())).when(actorUtils).actorSelection(
            kit.getRef().path());
        doReturn(kit.duration("5 seconds")).when(actorUtils).getOperationDuration();
        doReturn(Futures.successful(kit.getRef())).when(actorUtils).findLocalShardAsync("shard-1");

        final var proxy = createProxy(actorUtils, YangInstanceIdentifier.of(TestModel.TEST_QNAME), true);
        final var instance = proxy.getKey();

        doAnswer(invocation -> {
            instance.close();
            return CompletableFuture.completedStage(new RegisterDataTreeNotificationListenerReply(kit.getRef()));
        }).when(actorUtils).ask(any(), any(), any());
        proxy.getValue().run();

        kit.expectMsgClass(Duration.ofSeconds(5), CloseDataTreeNotificationListenerRegistration.class);

        assertNull(instance.getListenerRegistrationActor());
    }

    @NonNullByDefault
    private DataTreeChangeListenerProxy startProxyAsync(final ActorUtils actorUtils, final YangInstanceIdentifier path,
            final boolean clustered) {
        return startProxyAsync(actorUtils, path, clustered, Runnable::run);
    }

    @NonNullByDefault
    private DataTreeChangeListenerProxy startProxyAsync(final ActorUtils actorUtils, final YangInstanceIdentifier path,
            final boolean clustered, final Consumer<Runnable> execute) {
        final var proxy = createProxy(actorUtils, path, clustered);
        final var thread = new Thread(proxy.getValue());
        thread.setDaemon(true);
        thread.start();
        return proxy.getKey();
    }

    @NonNullByDefault
    private Entry<DataTreeChangeListenerProxy, Runnable> createProxy(final ActorUtils actorUtils,
            final YangInstanceIdentifier path, final boolean clustered) {
        final var executor = mock(Executor.class);
        final var captor = ArgumentCaptor.forClass(Runnable.class);
        doNothing().when(executor).execute(captor.capture());
        final var proxy = DataTreeChangeListenerProxy.ofTesting(actorUtils, mockListener, path, clustered, "shard-1",
            executor);
        return Map.entry(proxy, captor.getValue());
    }
}
