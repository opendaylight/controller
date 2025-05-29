/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import com.google.common.util.concurrent.MoreExecutors;
import java.util.HashMap;
import java.util.Map;
import org.apache.pekko.actor.Props;
import org.apache.pekko.testkit.TestActorRef;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.opendaylight.controller.cluster.raft.persisted.ServerInfo;
import org.opendaylight.controller.cluster.raft.persisted.VotingConfig;
import org.opendaylight.controller.cluster.raft.spi.TestTermInfoStore;

/**
 * Unit tests for RaftActorContextImpl.
 *
 * @author Thomas Pantelis
 */
public class RaftActorContextImplTest extends AbstractActorTest {
    private final TestActorFactory actorFactory = new TestActorFactory(getSystem());
    private final TestActorRef<DoNothingActor> actor = actorFactory.createTestActor(
            Props.create(DoNothingActor.class), actorFactory.generateActorId("actor"));

    @Rule
    public TemporaryFolder stateDir = TemporaryFolder.builder().assureDeletion().build();

    @After
    public void tearDown() {
        actorFactory.close();
    }

    @Test
    public void testGetPeerAddress() {
        final var peerMap = new HashMap<String, String>();
        peerMap.put("peer1", "peerAddress1");
        peerMap.put("peer2", null);
        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        RaftActorContextImpl context = new RaftActorContextImpl(actor, actor.underlyingActor().getContext(),
            new LocalAccess("test", stateDir.getRoot().toPath(), new TestTermInfoStore()), peerMap, configParams,
            (short) 0, new TestPersistenceProvider(), (identifier, entry) -> { },  MoreExecutors.directExecutor());

        assertEquals("getPeerAddress", "peerAddress1", context.getPeerAddress("peer1"));
        assertEquals("getPeerAddress", null, context.getPeerAddress("peer2"));

        PeerAddressResolver mockResolver = mock(PeerAddressResolver.class);
        doReturn("peerAddress2").when(mockResolver).resolve("peer2");
        doReturn("peerAddress3").when(mockResolver).resolve("peer3");
        configParams.setPeerAddressResolver(mockResolver);

        assertEquals("getPeerAddress", "peerAddress2", context.getPeerAddress("peer2"));
        assertEquals("getPeerAddress", "peerAddress3", context.getPeerAddress("peer3"));

        reset(mockResolver);
        assertEquals("getPeerAddress", "peerAddress1", context.getPeerAddress("peer1"));
        assertEquals("getPeerAddress", "peerAddress2", context.getPeerAddress("peer2"));
        verify(mockResolver, never()).resolve(anyString());
    }

    @Test
    public void testSetPeerAddress() {
        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        RaftActorContextImpl context = new RaftActorContextImpl(actor, actor.underlyingActor().getContext(),
            new LocalAccess("test", stateDir.getRoot().toPath(), new TestTermInfoStore()),
            Map.of("peer1", "peerAddress1"), configParams, (short) 0, new TestPersistenceProvider(),
            (identifier, entry) -> { }, MoreExecutors.directExecutor());

        context.setPeerAddress("peer1", "peerAddress1_1");
        assertEquals("getPeerAddress", "peerAddress1_1", context.getPeerAddress("peer1"));

        context.setPeerAddress("peer2", "peerAddress2");
        assertEquals("getPeerAddress", null, context.getPeerAddress("peer2"));
    }

    @Test
    public void testUpdatePeerIds() {
        RaftActorContextImpl context = new RaftActorContextImpl(actor, actor.underlyingActor().getContext(),
            new LocalAccess("self", stateDir.getRoot().toPath(), new TestTermInfoStore()),
            Map.of("peer1", "peerAddress1"), new DefaultConfigParamsImpl(), (short) 0, new TestPersistenceProvider(),
            (identifier, entry) -> { }, MoreExecutors.directExecutor());

        context.updateVotingConfig(new VotingConfig(
            new ServerInfo("self", false), new ServerInfo("peer2", true), new ServerInfo("peer3", false)));
        verifyPeerInfo(context, "peer1", null);
        verifyPeerInfo(context, "peer2", Boolean.TRUE);
        verifyPeerInfo(context, "peer3", Boolean.FALSE);
        assertFalse("isVotingMember", context.isVotingMember());

        context.updateVotingConfig(new VotingConfig(
            new ServerInfo("self", true), new ServerInfo("peer2", true), new ServerInfo("peer3", true)));
        verifyPeerInfo(context, "peer2", Boolean.TRUE);
        verifyPeerInfo(context, "peer3", Boolean.TRUE);
        assertTrue("isVotingMember", context.isVotingMember());

        context.updateVotingConfig(new VotingConfig(new ServerInfo("peer2", true), new ServerInfo("peer3", true)));
        verifyPeerInfo(context, "peer2", Boolean.TRUE);
        verifyPeerInfo(context, "peer3", Boolean.TRUE);
        assertFalse("isVotingMember", context.isVotingMember());
    }

    private static void verifyPeerInfo(final RaftActorContextImpl context, final String peerId, final Boolean voting) {
        PeerInfo peerInfo = context.getPeerInfo(peerId);
        if (voting != null) {
            assertNotNull("Expected peer " + peerId, peerInfo);
            assertEquals("getVotingState for " + peerId, voting
                    ? VotingState.VOTING : VotingState.NON_VOTING, peerInfo.getVotingState());
        } else {
            assertNull("Unexpected peer " + peerId, peerInfo);
        }
    }
}
