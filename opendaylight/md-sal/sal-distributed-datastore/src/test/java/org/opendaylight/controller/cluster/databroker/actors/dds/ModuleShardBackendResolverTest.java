/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.google.common.util.concurrent.Uninterruptibles;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Status;
import org.apache.pekko.dispatch.ExecutionContexts;
import org.apache.pekko.testkit.TestProbe;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import org.opendaylight.yangtools.yang.data.tree.api.DataTree;
import scala.concurrent.Future;

@ExtendWith(MockitoExtension.class)
class ModuleShardBackendResolverTest {
    private static final MemberName MEMBER_NAME = MemberName.forName("member-1");
    private static final FrontendType FRONTEND_TYPE = FrontendType.forName("type-1");
    private static final FrontendIdentifier FRONTEND_ID = FrontendIdentifier.create(MEMBER_NAME, FRONTEND_TYPE);
    private static final ClientIdentifier CLIENT_ID = ClientIdentifier.create(FRONTEND_ID, 0);

    @Mock
    private ShardStrategyFactory shardStrategyFactory;
    @Mock
    private ShardStrategy shardStrategy;
    @Mock
    private DataTree dataTree;
    @Mock
    private ActorUtils actorUtils;
    @Mock
    private Consumer<Long> mockCallback;
    @Mock
    private Registration mockReg;

    private ActorSystem system;
    private ModuleShardBackendResolver moduleShardBackendResolver;
    private TestProbe contextProbe;
    private TestProbe shardManagerProbe;
    private Future<PrimaryShardInfo> future;

    @BeforeEach
    void beforeEach() {
        system = ActorSystem.apply();
        contextProbe = new TestProbe(system, "context");
        shardManagerProbe = new TestProbe(system, "ShardManager");
        future = Future.successful(new PrimaryShardInfo(system.actorSelection(contextProbe.ref().path()), (short) 0));
        doReturn(shardManagerProbe.ref()).when(actorUtils).getShardManager();
        moduleShardBackendResolver = new ModuleShardBackendResolver(CLIENT_ID, actorUtils);
    }

    @AfterEach
    void afterEach() {
        TestKit.shutdownActorSystem(system);
    }

    @Test
    void testResolveShardForPathNonNullCookie() {
        doReturn(DefaultShardStrategy.DEFAULT_SHARD).when(shardStrategy).findShard(YangInstanceIdentifier.of());
        doReturn(shardStrategyFactory).when(actorUtils).getShardStrategyFactory();
        doReturn(shardStrategy).when(shardStrategyFactory).getStrategy(YangInstanceIdentifier.of());

        final var cookie = moduleShardBackendResolver.resolveShardForPath(YangInstanceIdentifier.of());
        assertEquals(0L, (long) cookie);
    }

    @Test
    void testResolveShardForPathNullCookie() {
        doReturn("foo").when(shardStrategy).findShard(YangInstanceIdentifier.of());
        doReturn(shardStrategyFactory).when(actorUtils).getShardStrategyFactory();
        doReturn(shardStrategy).when(shardStrategyFactory).getStrategy(YangInstanceIdentifier.of());

        final var cookie = moduleShardBackendResolver.resolveShardForPath(YangInstanceIdentifier.of());
        assertEquals(1L, (long) cookie);
    }

    @Test
    void testGetBackendInfo() throws Exception {
        doReturn(future).when(actorUtils).findPrimaryShardAsync(DefaultShardStrategy.DEFAULT_SHARD);
        doReturn(ExecutionContexts.global()).when(actorUtils).getClientDispatcher();

        final var initial = moduleShardBackendResolver.getBackendInfo(0L);
        contextProbe.expectMsgClass(ConnectClientRequest.class);
        final var backendProbe = new TestProbe(system, "backend");
        final var msg = new ConnectClientSuccess(CLIENT_ID, 0L, backendProbe.ref(), List.of(), dataTree, 3);
        contextProbe.reply(msg);
        assertSame(initial, moduleShardBackendResolver.getBackendInfo(0L));

        final var shardBackendInfo = TestUtils.getWithTimeout(initial.toCompletableFuture());
        assertEquals(0L, shardBackendInfo.getCookie().longValue());
        assertEquals(dataTree, shardBackendInfo.getDataTree().orElseThrow());
        assertEquals(DefaultShardStrategy.DEFAULT_SHARD, shardBackendInfo.getName());
    }

    @Test
    void testGetBackendInfoFail() throws Exception {
        doReturn(future).when(actorUtils).findPrimaryShardAsync(DefaultShardStrategy.DEFAULT_SHARD);
        doReturn(ExecutionContexts.global()).when(actorUtils).getClientDispatcher();

        final var initial = moduleShardBackendResolver.getBackendInfo(0L);
        final var req = contextProbe.expectMsgClass(ConnectClientRequest.class);
        final var cause = new RuntimeException();
        final var response = req.toRequestFailure(new RuntimeRequestException("fail", cause));
        contextProbe.reply(response);

        final var caught = assertThrows(ExecutionException.class,
            () -> TestUtils.getWithTimeout(initial.toCompletableFuture()));
        assertEquals(cause, caught.getCause());
    }

    @Test
    void testRefreshBackendInfo() throws Exception {
        doReturn(future).when(actorUtils).findPrimaryShardAsync(DefaultShardStrategy.DEFAULT_SHARD);
        doReturn(new PrimaryShardInfoFutureCache()).when(actorUtils).getPrimaryShardInfoCache();
        doReturn(ExecutionContexts.global()).when(actorUtils).getClientDispatcher();

        final var backendInfo = moduleShardBackendResolver.getBackendInfo(0L);
        // handle first connect
        contextProbe.expectMsgClass(ConnectClientRequest.class);

        final var staleBackendProbe = new TestProbe(system, "staleBackend");
        final var msg = new ConnectClientSuccess(CLIENT_ID, 0L, staleBackendProbe.ref(), List.of(), dataTree, 3);
        contextProbe.reply(msg);

        // get backend info
        final var staleBackendInfo = TestUtils.getWithTimeout(backendInfo.toCompletableFuture());
        // refresh
        final var refreshed = moduleShardBackendResolver.refreshBackendInfo(0L, staleBackendInfo);

        // stale backend info should be removed and new connect request issued to the context
        contextProbe.expectMsgClass(ConnectClientRequest.class);
        final var refreshedBackendProbe = new TestProbe(system, "refreshedBackend");
        final var msg2 = new ConnectClientSuccess(CLIENT_ID, 1L, refreshedBackendProbe.ref(), List.of(), dataTree, 3);
        contextProbe.reply(msg2);
        final var refreshedBackendInfo = TestUtils.getWithTimeout(refreshed.toCompletableFuture());
        assertEquals(staleBackendInfo.getCookie(), refreshedBackendInfo.getCookie());
        assertEquals(refreshedBackendProbe.ref(), refreshedBackendInfo.getActor());
    }

    @Test
    void testNotifyWhenBackendInfoIsStale() {
        final var regMessage = shardManagerProbe.expectMsgClass(RegisterForShardAvailabilityChanges.class);
        shardManagerProbe.reply(new Status.Success(mockReg));

        final var callbackReg = moduleShardBackendResolver.notifyWhenBackendInfoIsStale(mockCallback);

        regMessage.getCallback().accept(DefaultShardStrategy.DEFAULT_SHARD);
        verify(mockCallback, timeout(5000)).accept((long) 0);

        reset(mockCallback);
        callbackReg.close();

        regMessage.getCallback().accept(DefaultShardStrategy.DEFAULT_SHARD);
        Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
        verifyNoMoreInteractions(mockCallback);
    }
}
