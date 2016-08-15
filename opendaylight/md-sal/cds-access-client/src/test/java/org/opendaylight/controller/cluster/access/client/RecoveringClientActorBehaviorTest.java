/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.access.client;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import akka.actor.ActorRef;
import akka.persistence.RecoveryCompleted;
import akka.persistence.SnapshotMetadata;
import akka.persistence.SnapshotOffer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.MemberName;

public class RecoveringClientActorBehaviorTest {

    private static final MemberName MEMBER_NAME = MemberName.forName("member-1");

    @Mock
    private ActorRef mockActorRef;
    @Mock
    private AbstractClientActor mockAbstractActor;

    private RecoveringClientActorBehavior recoveringActorBehavior;
    private String persistenceId;
    private ClientIdentifier clientId;

    @Before
    public void initialization() {
        MockitoAnnotations.initMocks(this);

        persistenceId = this.getClass().getName();
        final FrontendType frontendType = FrontendType.forName(persistenceId);
        final FrontendIdentifier frontendId = FrontendIdentifier.create(MEMBER_NAME, frontendType);

        doReturn(mockActorRef).when(mockAbstractActor).getSelf();
        doNothing().when(mockAbstractActor).saveSnapshot(any(ClientIdentifier.class));

        final InitialClientActorContext context = new InitialClientActorContext(mockAbstractActor, persistenceId);
        recoveringActorBehavior = new RecoveringClientActorBehavior(context, frontendId);
        clientId = ClientIdentifier.create(frontendId, 0);
    }

    @Test(expected = IllegalStateException.class)
    public void testOnReceiveCommand() {
        recoveringActorBehavior.onReceiveCommand(new Object());
    }

    @Test
    public void testOnReceiveRecover() {
        final AbstractClientActorBehavior<?> clientActorBehavior = recoveringActorBehavior
                .onReceiveRecover(new Object());
        assertNotNull(clientActorBehavior);
        assertSame(clientActorBehavior, recoveringActorBehavior);
    }

    @Test
    public void testOnReceiveRecoverRecoveryCompleted() {
        final RecoveryCompleted rc = mock(RecoveryCompleted.class);
        final AbstractClientActorBehavior<?> clientActorBehavior = recoveringActorBehavior.onReceiveRecover(rc);

        assertNotNull(clientActorBehavior);
        assertTrue(clientActorBehavior instanceof SavingClientActorBehavior);
    }

    @Test
    public void testOnReceiveRecoverSnapshotOffer() {
        final SnapshotMetadata metadata = new SnapshotMetadata(persistenceId, 1L, 5L);
        final SnapshotOffer so = new SnapshotOffer(metadata, clientId);
        final AbstractClientActorBehavior<?> clientActorBehavior = recoveringActorBehavior
                .onReceiveRecover(so);
        assertNotNull(clientActorBehavior);
        assertSame(clientActorBehavior, recoveringActorBehavior);
    }

    @Test
    public void testOnReceiveRecoverAddGenereationId() {
        final SnapshotMetadata metadata = new SnapshotMetadata(persistenceId, 1L, 5L);
        final SnapshotOffer so = new SnapshotOffer(metadata, clientId);
        AbstractClientActorBehavior<?> clientActorBehavior = recoveringActorBehavior.onReceiveRecover(so);
        assertNotNull(clientActorBehavior);
        assertSame(clientActorBehavior, recoveringActorBehavior);
        final RecoveryCompleted rc = mock(RecoveryCompleted.class);
        clientActorBehavior = recoveringActorBehavior.onReceiveRecover(rc);
        assertNotNull(clientActorBehavior);
        assertTrue(clientActorBehavior instanceof SavingClientActorBehavior);
    }

    @Test
    public void testOnReceiveRecoverNull() {
        final FrontendType frontendType = FrontendType.forName(persistenceId);
        final FrontendIdentifier frontendId = FrontendIdentifier.create(MemberName.forName("member-2"), frontendType);
        final SnapshotMetadata metadata = new SnapshotMetadata(persistenceId, 1L, 5L);
        final SnapshotOffer so = new SnapshotOffer(metadata, ClientIdentifier.create(frontendId, 0));
        final AbstractClientActorBehavior<?> clientActorBehavior = recoveringActorBehavior.onReceiveRecover(so);
        assertNotNull(clientActorBehavior);
        assertSame(clientActorBehavior, recoveringActorBehavior);
        final RecoveryCompleted rc = mock(RecoveryCompleted.class);
        final AbstractClientActorBehavior<?> nullActorBehavior = recoveringActorBehavior.onReceiveRecover(rc);
        assertNull(nullActorBehavior);
    }
}
