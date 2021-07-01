/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Status;
import akka.testkit.TestProbe;
import akka.testkit.javadsl.TestKit;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.controller.cluster.access.commands.ConnectClientFailure;
import org.opendaylight.controller.cluster.access.commands.ConnectClientRequest;
import org.opendaylight.controller.cluster.access.commands.ConnectClientSuccess;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.RuntimeRequestException;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;
import org.opendaylight.controller.cluster.datastore.shardmanager.RegisterForShardAvailabilityChanges;
import org.opendaylight.controller.cluster.datastore.shardstrategy.DefaultShardStrategy;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategy;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategyFactory;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.controller.cluster.datastore.utils.PrimaryShardInfoFutureCache;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import scala.concurrent.Promise;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class ModuleShardBackendResolverTest {

    private static final MemberName MEMBER_NAME = MemberName.forName("member-1");
    private static final FrontendType FRONTEND_TYPE = FrontendType.forName("type-1");
    private static final FrontendIdentifier FRONTEND_ID = FrontendIdentifier.create(MEMBER_NAME, FRONTEND_TYPE);
    private static final ClientIdentifier CLIENT_ID = ClientIdentifier.create(FRONTEND_ID, 0);

    private ActorSystem system;
    private ModuleShardBackendResolver moduleShardBackendResolver;
    private TestProbe contextProbe;
    private TestProbe shardManagerProbe;

    @Mock
    private ShardStrategyFactory shardStrategyFactory;
    @Mock
    private ShardStrategy shardStrategy;
    @Mock
    private DataTree dataTree;

    @Before
    public void setUp() {
        system = ActorSystem.apply();
        contextProbe = new TestProbe(system, "context");

        shardManagerProbe = new TestProbe(system, "ShardManager");

        final ActorUtils actorUtils = createActorUtilsMock(system, contextProbe.ref());
        doReturn(shardManagerProbe.ref()).when(actorUtils).getShardManager();

        moduleShardBackendResolver = new ModuleShardBackendResolver(CLIENT_ID, actorUtils);
        doReturn(shardStrategyFactory).when(actorUtils).getShardStrategyFactory();
        doReturn(shardStrategy).when(shardStrategyFactory).getStrategy(YangInstanceIdentifier.empty());
        final PrimaryShardInfoFutureCache cache = new PrimaryShardInfoFutureCache();
        doReturn(cache).when(actorUtils).getPrimaryShardInfoCache();
    }

    @After
    public void tearDown() {
        TestKit.shutdownActorSystem(system);
    }

    @Test
    public void testResolveShardForPathNonNullCookie() {
        doReturn(DefaultShardStrategy.DEFAULT_SHARD).when(shardStrategy).findShard(YangInstanceIdentifier.empty());
        final Long cookie = moduleShardBackendResolver.resolveShardForPath(YangInstanceIdentifier.empty());
        assertEquals(0L, (long) cookie);
    }

    @Test
    public void testResolveShardForPathNullCookie() {
        doReturn("foo").when(shardStrategy).findShard(YangInstanceIdentifier.empty());
        final Long cookie = moduleShardBackendResolver.resolveShardForPath(YangInstanceIdentifier.empty());
        assertEquals(1L, (long) cookie);
    }

    @Test
    public void testGetBackendInfo() throws Exception {
        final CompletionStage<ShardBackendInfo> i = moduleShardBackendResolver.getBackendInfo(0L);
        contextProbe.expectMsgClass(ConnectClientRequest.class);
        final TestProbe backendProbe = new TestProbe(system, "backend");
        final ConnectClientSuccess msg = new ConnectClientSuccess(CLIENT_ID, 0L, backendProbe.ref(),
                List.of(), dataTree, 3);
        contextProbe.reply(msg);
        final CompletionStage<ShardBackendInfo> stage = moduleShardBackendResolver.getBackendInfo(0L);
        final ShardBackendInfo shardBackendInfo = TestUtils.getWithTimeout(stage.toCompletableFuture());
        assertEquals(0L, shardBackendInfo.getCookie().longValue());
        assertEquals(dataTree, shardBackendInfo.getDataTree().get());
        assertEquals(DefaultShardStrategy.DEFAULT_SHARD, shardBackendInfo.getName());
    }

    @Test
    public void testGetBackendInfoFail() throws Exception {
        final CompletionStage<ShardBackendInfo> i = moduleShardBackendResolver.getBackendInfo(0L);
        final ConnectClientRequest req = contextProbe.expectMsgClass(ConnectClientRequest.class);
        final RuntimeException cause = new RuntimeException();
        final ConnectClientFailure response = req.toRequestFailure(new RuntimeRequestException("fail", cause));
        contextProbe.reply(response);
        final CompletionStage<ShardBackendInfo> stage = moduleShardBackendResolver.getBackendInfo(0L);
        final ExecutionException caught =
                TestUtils.assertOperationThrowsException(() -> TestUtils.getWithTimeout(stage.toCompletableFuture()),
                        ExecutionException.class);
        assertEquals(cause, caught.getCause());
    }

    @Test
    public void testRefreshBackendInfo() throws Exception {
        final CompletionStage<ShardBackendInfo> backendInfo = moduleShardBackendResolver.getBackendInfo(0L);
        //handle first connect
        contextProbe.expectMsgClass(ConnectClientRequest.class);
        final TestProbe staleBackendProbe = new TestProbe(system, "staleBackend");
        final ConnectClientSuccess msg = new ConnectClientSuccess(CLIENT_ID, 0L, staleBackendProbe.ref(),
                List.of(), dataTree, 3);
        contextProbe.reply(msg);
        //get backend info
        final ShardBackendInfo staleBackendInfo = TestUtils.getWithTimeout(backendInfo.toCompletableFuture());
        //refresh
        final CompletionStage<ShardBackendInfo> refreshed =
                moduleShardBackendResolver.refreshBackendInfo(0L, staleBackendInfo);
        //stale backend info should be removed and new connect request issued to the context
        contextProbe.expectMsgClass(ConnectClientRequest.class);
        final TestProbe refreshedBackendProbe = new TestProbe(system, "refreshedBackend");
        final ConnectClientSuccess msg2 = new ConnectClientSuccess(CLIENT_ID, 1L, refreshedBackendProbe.ref(),
                List.of(), dataTree, 3);
        contextProbe.reply(msg2);
        final ShardBackendInfo refreshedBackendInfo = TestUtils.getWithTimeout(refreshed.toCompletableFuture());
        assertEquals(staleBackendInfo.getCookie(), refreshedBackendInfo.getCookie());
        assertEquals(refreshedBackendProbe.ref(), refreshedBackendInfo.getActor());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testNotifyWhenBackendInfoIsStale() {
        final RegisterForShardAvailabilityChanges regMessage =
                shardManagerProbe.expectMsgClass(RegisterForShardAvailabilityChanges.class);
        Registration mockReg = mock(Registration.class);
        shardManagerProbe.reply(new Status.Success(mockReg));

        Consumer<Long> mockCallback = mock(Consumer.class);
        final Registration callbackReg = moduleShardBackendResolver.notifyWhenBackendInfoIsStale(mockCallback);

        regMessage.getCallback().accept(DefaultShardStrategy.DEFAULT_SHARD);
        verify(mockCallback, timeout(5000)).accept((long) 0);

        reset(mockCallback);
        callbackReg.close();

        regMessage.getCallback().accept(DefaultShardStrategy.DEFAULT_SHARD);
        Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
        verifyNoMoreInteractions(mockCallback);
    }

    private static ActorUtils createActorUtilsMock(final ActorSystem system, final ActorRef actor) {
        final ActorUtils mock = mock(ActorUtils.class);
        final Promise<PrimaryShardInfo> promise = new scala.concurrent.impl.Promise.DefaultPromise<>();
        final ActorSelection selection = system.actorSelection(actor.path());
        final PrimaryShardInfo shardInfo = new PrimaryShardInfo(selection, (short) 0);
        promise.success(shardInfo);
        doReturn(promise.future()).when(mock).findPrimaryShardAsync(DefaultShardStrategy.DEFAULT_SHARD);
        return mock;
    }
}
