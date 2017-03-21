/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.testkit.TestProbe;
import com.google.common.primitives.UnsignedLong;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategy;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategyFactory;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import scala.concurrent.Promise;

public class ModuleShardBackendResolverTest {

    private static final MemberName MEMBER_NAME = MemberName.forName("member-1");
    private static final FrontendType FRONTEND_TYPE = FrontendType.forName("type-1");
    private static final FrontendIdentifier FRONTEND_ID = FrontendIdentifier.create(MEMBER_NAME, FRONTEND_TYPE);
    private static final ClientIdentifier CLIENT_ID = ClientIdentifier.create(FRONTEND_ID, 0);

    private ActorSystem system;
    private ActorContext actorContext;
    private ModuleShardBackendResolver moduleShardBackendResolver;
    private TestProbe contextProbe;
    private ShardBackendInfo shardBackendInfo;

    @Mock
    private ShardStrategyFactory shardStrategyFactory;
    @Mock
    private ShardStrategy shardStrategy;
    @Mock
    private ActorRef actorRef;
    @Mock
    private DataTree dataTree;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        system = ActorSystem.apply();
        contextProbe = new TestProbe(system, "context");
        actorContext = createActorContextMock(system, contextProbe.ref());
        moduleShardBackendResolver = new ModuleShardBackendResolver(CLIENT_ID, actorContext);
        shardBackendInfo = new ShardBackendInfo(
                actorRef, 0L, ABIVersion.TEST_FUTURE_VERSION, "default", UnsignedLong.ONE, Optional.of(dataTree), 10);
        doReturn(shardStrategyFactory).when(actorContext).getShardStrategyFactory();
        doReturn(shardStrategy).when(shardStrategyFactory).getStrategy(any(YangInstanceIdentifier.class));
    }

    @Test
    public void testResolveShardForPathNonNullCookie() throws Exception {
        doReturn("default").when(shardStrategy).findShard(any(YangInstanceIdentifier.class));
        final Long cookie = moduleShardBackendResolver.resolveShardForPath(YangInstanceIdentifier.EMPTY);
        Assert.assertEquals(0L, cookie.longValue());
    }

    @Test
    public void testResolveShardForPathNullCookie() throws Exception {
        doReturn("foo").when(shardStrategy).findShard(any(YangInstanceIdentifier.class));
        final Long cookie = moduleShardBackendResolver.resolveShardForPath(YangInstanceIdentifier.EMPTY);
        Assert.assertEquals(1L, cookie.longValue());
    }

    @Test
    public void testGetBackendInfo() throws Exception {
        final CompletionStage<ShardBackendInfo> stage = moduleShardBackendResolver.getBackendInfo(0L);
        Assert.assertTrue(stage.toCompletableFuture().complete(shardBackendInfo));
    }

    @Test
    public void testRefreshBackendInfoWithoutShardState() throws Exception {
        final CompletionStage<ShardBackendInfo> stage =
                moduleShardBackendResolver.refreshBackendInfo(0L, shardBackendInfo);
        Assert.assertTrue(stage.toCompletableFuture().complete(shardBackendInfo));
    }

    @Test
    public void testRefreshBackendInfoExistingShardState() throws Exception {
        moduleShardBackendResolver.getBackendInfo(0L);
        final CompletionStage<ShardBackendInfo> stage =
                moduleShardBackendResolver.refreshBackendInfo(0L, shardBackendInfo);
        Assert.assertTrue(stage.toCompletableFuture().complete(shardBackendInfo));
    }

    private static ActorContext createActorContextMock(final ActorSystem system, final ActorRef actor) {
        final ActorContext mock = mock(ActorContext.class);
        final Promise<PrimaryShardInfo> promise = new scala.concurrent.impl.Promise.DefaultPromise<>();
        final ActorSelection selection = system.actorSelection(actor.path());
        final PrimaryShardInfo shardInfo = new PrimaryShardInfo(selection, (short) 0);
        promise.success(shardInfo);
        when(mock.findPrimaryShardAsync(any())).thenReturn(promise.future());
        when(mock.findPrimaryShardAsync(any())).thenReturn(promise.future());
        return mock;
    }
}
