/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.exceptions.NotInitializedException;
import org.opendaylight.controller.cluster.datastore.messages.DataTreeChanged;
import org.opendaylight.controller.cluster.datastore.messages.EnableNotification;
import org.opendaylight.controller.cluster.datastore.messages.FindLocalShard;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardFound;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeNotificationListenerReply;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.controller.md.cluster.datastore.model.PeopleModel;
import org.opendaylight.mdsal.dom.api.ClusteredDOMDataTreeChangeListener;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.tree.spi.DataTreeCandidates;

public class RootDataTreeChangeListenerProxyTest extends AbstractActorTest {

    @Test(timeout = 10000)
    public void testSuccessfulRegistrationOnTwoShards() {
        final TestKit kit = new TestKit(getSystem());
        ActorUtils actorUtils = new ActorUtils(getSystem(), kit.getRef(), mock(ClusterWrapper.class),
            mock(Configuration.class));

        ClusteredDOMDataTreeChangeListener mockClusteredListener = mock(
            ClusteredDOMDataTreeChangeListener.class);

        final YangInstanceIdentifier path = YangInstanceIdentifier.of();
        final RootDataTreeChangeListenerProxy<ClusteredDOMDataTreeChangeListener> rootListenerProxy =
            new RootDataTreeChangeListenerProxy<>(actorUtils, mockClusteredListener,
            Set.of("shard-1", "shard-2"));

        final Duration timeout = Duration.ofSeconds(5);
        FindLocalShard findLocalShard1 = kit.expectMsgClass(FindLocalShard.class);
        kit.reply(new LocalShardFound(kit.getRef()));
        FindLocalShard findLocalShard2 = kit.expectMsgClass(FindLocalShard.class);
        kit.reply(new LocalShardFound(kit.getRef()));
        assertThat(List.of(findLocalShard1.shardName(), findLocalShard2.shardName()))
            .containsAll(List.of("shard-2", "shard-1"));

        RegisterDataTreeChangeListener registerForShard1 = kit.expectMsgClass(timeout,
            RegisterDataTreeChangeListener.class);
        assertEquals("getPath", path, registerForShard1.getPath());
        assertTrue("isRegisterOnAllInstances", registerForShard1.isRegisterOnAllInstances());

        kit.reply(new RegisterDataTreeNotificationListenerReply(kit.getRef()));

        RegisterDataTreeChangeListener registerForShard2 = kit.expectMsgClass(timeout,
            RegisterDataTreeChangeListener.class);
        assertEquals("getPath", path, registerForShard2.getPath());
        assertTrue("isRegisterOnAllInstances", registerForShard2.isRegisterOnAllInstances());

        kit.reply(new RegisterDataTreeNotificationListenerReply(kit.getRef()));

        assertEquals(registerForShard1.getListenerActorPath(), registerForShard2.getListenerActorPath());

        final TestKit kit2 = new TestKit(getSystem());
        final ActorSelection rootListenerActor = getSystem().actorSelection(registerForShard1.getListenerActorPath());
        rootListenerActor.tell(new EnableNotification(true, "test"), kit.getRef());
        final DataTreeCandidate peopleCandidate = DataTreeCandidates.fromNormalizedNode(YangInstanceIdentifier.of(),
            PeopleModel.create());
        rootListenerActor.tell(new DataTreeChanged(ImmutableList.of(peopleCandidate)), kit.getRef());
        rootListenerActor.tell(new DataTreeChanged(ImmutableList.of(peopleCandidate)), kit2.getRef());
        //verify the 2 candidates were processed into 1 initial candidate
        verify(mockClusteredListener, timeout(100).times(1)).onDataTreeChanged(any());

        rootListenerProxy.close();
    }

    @Test(timeout = 10000, expected = java.lang.AssertionError.class)
    public void testNotAllShardsFound() {
        final TestKit kit = new TestKit(getSystem());
        ActorUtils actorUtils = new ActorUtils(getSystem(), kit.getRef(), mock(ClusterWrapper.class),
            mock(Configuration.class));

        ClusteredDOMDataTreeChangeListener mockClusteredListener = mock(
            ClusteredDOMDataTreeChangeListener.class);

        final RootDataTreeChangeListenerProxy<ClusteredDOMDataTreeChangeListener> rootListenerProxy =
            new RootDataTreeChangeListenerProxy<>(actorUtils, mockClusteredListener, Set.of("shard-1", "shard-2"));

        Duration timeout = Duration.ofSeconds(5);
        kit.expectMsgClass(FindLocalShard.class);
        kit.reply(new LocalShardFound(kit.getRef()));
        kit.expectMsgClass(FindLocalShard.class);
        // don't send second reply
        kit.expectMsgClass(timeout, RegisterDataTreeChangeListener.class);

        rootListenerProxy.close();
    }

    @Test(timeout = 10000, expected = java.lang.AssertionError.class)
    public void testLocalShardNotInitialized() {
        final TestKit kit = new TestKit(getSystem());
        ActorUtils actorUtils = new ActorUtils(getSystem(), kit.getRef(), mock(ClusterWrapper.class),
            mock(Configuration.class));

        ClusteredDOMDataTreeChangeListener mockClusteredListener = mock(
            ClusteredDOMDataTreeChangeListener.class);

        final RootDataTreeChangeListenerProxy<ClusteredDOMDataTreeChangeListener> rootListenerProxy =
            new RootDataTreeChangeListenerProxy<>(actorUtils, mockClusteredListener, Set.of("shard-1"));

        Duration timeout = Duration.ofSeconds(5);
        kit.expectMsgClass(FindLocalShard.class);
        kit.reply(new NotInitializedException("not initialized"));
        // don't send second reply
        kit.expectMsgClass(timeout, RegisterDataTreeChangeListener.class);

        rootListenerProxy.close();
    }
}
