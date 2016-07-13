/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors.client;

import akka.persistence.RecoveryCompleted;
import akka.persistence.SnapshotOffer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertNull;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.datastore.ShardTransactionTest;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

/**
 * @author Peter Gubka
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({InitialClientActorContext.class, RecoveryCompleted.class, SnapshotOffer.class, ClientIdentifier.class})
public class RecoveringClientActorBehaviorTest {
    private static final MemberName MEMBER_NAME = MemberName.forName("member-1");
    private static final MemberName MEMBER2_NAME = MemberName.forName("member-2");
    private static final FrontendType FRONTEND_TYPE = FrontendType.forName(ShardTransactionTest.class.getSimpleName());
    private static final FrontendIdentifier FRONTEND_ID = FrontendIdentifier.create(MEMBER_NAME, FRONTEND_TYPE);
    private static final FrontendIdentifier FRONTEND2_ID = FrontendIdentifier.create(MEMBER2_NAME, FRONTEND_TYPE);
    private static final ClientIdentifier CLIENT_ID = ClientIdentifier.create(FRONTEND_ID, 0);
    //private static final String PERSISTENCE_ID = ClientActorContextTest.class.getSimpleName();
    private final Object invalidRecover = new String("Invalid recovery object");
    private final Object anyCommand = new String("Any command");

    @Mock
    private RecoveryCompleted mockRecoveryCompleted;

    private RecoveringClientActorBehavior behavior;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

    }

    @Test(expected = IllegalStateException.class)
    public void testOnReceiveCommand() {
        InitialClientActorContext ctx = PowerMockito.mock(InitialClientActorContext.class);
        behavior = new RecoveringClientActorBehavior(ctx, FRONTEND_ID);
        behavior.onReceiveCommand(anyCommand);
    }

    @Test
    public void testOnReceiveRecover_RecoveryCompleted_missmathcedFEId() {
        InitialClientActorContext ctx = PowerMockito.mock(InitialClientActorContext.class);
        behavior = new RecoveringClientActorBehavior(ctx, FRONTEND_ID);
        ClientIdentifier mockClientId = PowerMockito.mock(ClientIdentifier.class);
        Whitebox.setInternalState(behavior, "lastId", mockClientId);
        PowerMockito.when(mockClientId.getFrontendId()).thenReturn(FRONTEND2_ID);
        Object rval = behavior.onReceiveRecover(mockRecoveryCompleted);
        assertNull(rval);
    }

    @Test
    public void testOnReceiveRecover_RecoveryCompleted_lastEqNext() {
        InitialClientActorContext ctx = PowerMockito.mock(InitialClientActorContext.class);
        behavior = new RecoveringClientActorBehavior(ctx, FRONTEND_ID);
        Whitebox.setInternalState(behavior, "lastId", CLIENT_ID);
        AbstractClientActorBehavior rval = behavior.onReceiveRecover(mockRecoveryCompleted);
        assertSame(rval.context(), ctx);
    }

    @Test
    public void testOnReceiveRecover_RecoveryCompleted_lastEqNull() {
        InitialClientActorContext ctx = PowerMockito.mock(InitialClientActorContext.class);
        behavior = new RecoveringClientActorBehavior(ctx, FRONTEND_ID);
        AbstractClientActorBehavior rval = behavior.onReceiveRecover(mockRecoveryCompleted);
        assertSame(rval.context(), ctx);
    }

    @Test
    public void testOnReceiveRecover_SnapshotOffer() {
        InitialClientActorContext ctx = PowerMockito.mock(InitialClientActorContext.class);
        behavior = new RecoveringClientActorBehavior(ctx, FRONTEND_ID);
        SnapshotOffer mockSnapshotOffer = PowerMockito.mock(SnapshotOffer.class);
        PowerMockito.when(mockSnapshotOffer.snapshot()).thenReturn(CLIENT_ID);
        Object rval = behavior.onReceiveRecover(mockSnapshotOffer);
        assertSame(CLIENT_ID, Whitebox.getInternalState(behavior, "lastId"));
        assertSame(rval, behavior);
    }

    @Test
    public void testOnReceiveRecover_other() {
        InitialClientActorContext ctx = PowerMockito.mock(InitialClientActorContext.class);
        behavior = new RecoveringClientActorBehavior(ctx, FRONTEND_ID);
        Object rval = behavior.onReceiveRecover(invalidRecover);
        assertSame(rval, behavior);
    }

}