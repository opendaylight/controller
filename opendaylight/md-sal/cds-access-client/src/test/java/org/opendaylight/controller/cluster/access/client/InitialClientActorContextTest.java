/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.access.client;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Scheduler;
import akka.actor.UntypedActorContext;
import akka.persistence.SnapshotSelectionCriteria;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import scala.concurrent.ExecutionContextExecutor;

public class InitialClientActorContextTest {

    private static final MemberName MEMBER_NAME = MemberName.forName("member-1");

    @Mock
    private AbstractClientActor mockAbstractActor;
    @Mock
    private ActorRef mockActorRef;
    @Mock
    private ActorSystem mockActorSystem;
    @Mock
    private ExecutionContextExecutor mockDispatcher;
    @Mock
    private Scheduler mockScheduler;
    @Mock
    private UntypedActorContext mockUntypedActorCtx;
    @Mock
    private ClientActorBehavior mockActorBehavior;

    private InitialClientActorContext initActorCtx;
    private ClientIdentifier clientId;

    @Before
    public void initialization() {
        MockitoAnnotations.initMocks(this);

        final FrontendType frontendType = FrontendType.forName(getClass().getSimpleName());
        final FrontendIdentifier frontendId = FrontendIdentifier.create(MEMBER_NAME, frontendType);
        clientId = ClientIdentifier.create(frontendId, 0);

        doReturn(mockDispatcher).when(mockActorSystem).dispatcher();
        doReturn(mockScheduler).when(mockActorSystem).scheduler();
        doReturn(mockActorSystem).when(mockUntypedActorCtx).system();
        doReturn(mockActorRef).when(mockAbstractActor).getSelf();
        doReturn(mockUntypedActorCtx).when(mockAbstractActor).getContext();
        doReturn(mockActorBehavior).when(mockAbstractActor).initialBehavior(any(ClientActorContext.class));
        doNothing().when(mockAbstractActor).stash();
        doNothing().when(mockAbstractActor).unstashAll();
        doNothing().when(mockAbstractActor).saveSnapshot(any(ClientIdentifier.class));
        doNothing().when(mockAbstractActor).deleteSnapshots(any(SnapshotSelectionCriteria.class));

        initActorCtx = new InitialClientActorContext(mockAbstractActor, this.getClass().getName());
    }

    @Test
    public void testSaveSnapshot() {
        initActorCtx.saveSnapshot(clientId);
        verify(mockAbstractActor).saveSnapshot(clientId);
    }

    @Test
    public void testDeleteSnapshots() {
        final SnapshotSelectionCriteria criteria = new SnapshotSelectionCriteria(1L, 1L, 1L, 1L);
        initActorCtx.deleteSnapshots(criteria);
        verify(mockAbstractActor).deleteSnapshots(criteria);
    }

    @Test
    public void testCreateBehavior() {
        final ClientActorBehavior clientActorBehavior = initActorCtx.createBehavior(clientId);
        assertNotNull(clientActorBehavior);
        verify(mockAbstractActor).initialBehavior(any(ClientActorContext.class));
        assertSame(clientActorBehavior, mockActorBehavior);
    }

    @Test
    public void testStash() {
        initActorCtx.stash();
        verify(mockAbstractActor).stash();
    }

    @Test
    public void testUnstash() {
        initActorCtx.unstash();
        verify(mockAbstractActor).unstashAll();
    }
}
