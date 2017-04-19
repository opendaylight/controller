/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import akka.testkit.TestProbe;
import java.util.Collections;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.access.commands.ConnectClientFailure;
import org.opendaylight.controller.cluster.access.commands.ConnectClientRequest;
import org.opendaylight.controller.cluster.access.commands.ConnectClientSuccess;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.RuntimeRequestException;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategy;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategyFactory;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.cluster.datastore.utils.PrimaryShardInfoFutureCache;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import scala.concurrent.Promise;

public class ModuleShardBackendResolverTest {

    private static final MemberName MEMBER_NAME = MemberName.forName("member-1");
    private static final FrontendType FRONTEND_TYPE = FrontendType.forName("type-1");
    private static final FrontendIdentifier FRONTEND_ID = FrontendIdentifier.create(MEMBER_NAME, FRONTEND_TYPE);
    private static final ClientIdentifier CLIENT_ID = ClientIdentifier.create(FRONTEND_ID, 0);

    private ActorSystem system;
    private ModuleShardBackendResolver moduleShardBackendResolver;
    private TestProbe contextProbe;

    @Mock
    private ShardStrategyFactory shardStrategyFactory;
    @Mock
    private ShardStrategy shardStrategy;
    @Mock
    private DataTree dataTree;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        system = ActorSystem.apply();
        contextProbe = new TestProbe(system, "context");
        final ActorContext actorContext = createActorContextMock(system, contextProbe.ref());
        moduleShardBackendResolver = new ModuleShardBackendResolver(CLIENT_ID, actorContext);
        when(actorContext.getShardStrategyFactory()).thenReturn(shardStrategyFactory);
        when(shardStrategyFactory.getStrategy(YangInstanceIdentifier.EMPTY)).thenReturn(shardStrategy);
        final PrimaryShardInfoFutureCache cache = new PrimaryShardInfoFutureCache();
        when(actorContext.getPrimaryShardInfoCache()).thenReturn(cache);
    }

    @After
    public void tearDown() throws Exception {
        JavaTestKit.shutdownActorSystem(system);
    }

    @Test
    public void testResolveShardForPathNonNullCookie() throws Exception {
        when(shardStrategy.findShard(YangInstanceIdentifier.EMPTY)).thenReturn("default");
        final Long cookie = moduleShardBackendResolver.resolveShardForPath(YangInstanceIdentifier.EMPTY);
        Assert.assertEquals(0L, cookie.longValue());
    }

    @Test
    public void testResolveShardForPathNullCookie() throws Exception {
        when(shardStrategy.findShard(YangInstanceIdentifier.EMPTY)).thenReturn("foo");
        final Long cookie = moduleShardBackendResolver.resolveShardForPath(YangInstanceIdentifier.EMPTY);
        Assert.assertEquals(1L, cookie.longValue());
    }

    @Test
    public void testGetBackendInfo() throws Exception {
        final CompletionStage<ShardBackendInfo> i = moduleShardBackendResolver.getBackendInfo(0L);
        contextProbe.expectMsgClass(ConnectClientRequest.class);
        final TestProbe backendProbe = new TestProbe(system, "backend");
        final ConnectClientSuccess msg = new ConnectClientSuccess(CLIENT_ID, 0L, backendProbe.ref(),
                Collections.emptyList(), dataTree, 3);
        contextProbe.reply(msg);
        final CompletionStage<ShardBackendInfo> stage = moduleShardBackendResolver.getBackendInfo(0L);
        final ShardBackendInfo shardBackendInfo = TestUtils.getWithTimeout(stage.toCompletableFuture());
        Assert.assertEquals(0L, shardBackendInfo.getCookie().longValue());
        Assert.assertEquals(dataTree, shardBackendInfo.getDataTree().get());
        Assert.assertEquals("default", shardBackendInfo.getShardName());
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
        Assert.assertEquals(cause, caught.getCause());
    }

    @Test
    public void testRefreshBackendInfo() throws Exception {
        final CompletionStage<ShardBackendInfo> backendInfo = moduleShardBackendResolver.getBackendInfo(0L);
        //handle first connect
        contextProbe.expectMsgClass(ConnectClientRequest.class);
        final TestProbe staleBackendProbe = new TestProbe(system, "staleBackend");
        final ConnectClientSuccess msg = new ConnectClientSuccess(CLIENT_ID, 0L, staleBackendProbe.ref(),
                Collections.emptyList(), dataTree, 3);
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
                Collections.emptyList(), dataTree, 3);
        contextProbe.reply(msg2);
        final ShardBackendInfo refreshedBackendInfo = TestUtils.getWithTimeout(refreshed.toCompletableFuture());
        Assert.assertEquals(staleBackendInfo.getCookie(), refreshedBackendInfo.getCookie());
        Assert.assertEquals(refreshedBackendProbe.ref(), refreshedBackendInfo.getActor());
    }

    private static ActorContext createActorContextMock(final ActorSystem system, final ActorRef actor) {
        final ActorContext mock = mock(ActorContext.class);
        final Promise<PrimaryShardInfo> promise = new scala.concurrent.impl.Promise.DefaultPromise<>();
        final ActorSelection selection = system.actorSelection(actor.path());
        final PrimaryShardInfo shardInfo = new PrimaryShardInfo(selection, (short) 0);
        promise.success(shardInfo);
        when(mock.findPrimaryShardAsync("default")).thenReturn(promise.future());
        return mock;
    }
}
