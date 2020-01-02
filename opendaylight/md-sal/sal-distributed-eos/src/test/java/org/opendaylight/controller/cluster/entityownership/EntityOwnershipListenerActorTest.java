/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.entityownership;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import akka.actor.ActorRef;
import akka.testkit.TestActorRef;
import org.junit.After;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.TestActorFactory;
import org.opendaylight.mdsal.eos.common.api.EntityOwnershipChangeState;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipChange;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipListener;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Unit tests for EntityOwnershipListenerActor.
 *
 * @author Thomas Pantelis
 */
public class EntityOwnershipListenerActorTest extends AbstractEntityOwnershipTest {
    private final TestActorFactory actorFactory = new TestActorFactory(getSystem());

    @After
    public void tearDown() {
        actorFactory.close();
    }

    @Test
    public void testOnEntityOwnershipChanged() {
        DOMEntityOwnershipListener mockListener = mock(DOMEntityOwnershipListener.class);

        TestActorRef<EntityOwnershipListenerActor> listenerActor = actorFactory.createTestActor(
                EntityOwnershipListenerActor.props(mockListener), actorFactory.generateActorId("listener"));

        DOMEntity entity = new DOMEntity("test", YangInstanceIdentifier.of(QName.create("test", "id1")));
        boolean wasOwner = false;
        boolean isOwner = true;
        boolean hasOwner = true;
        listenerActor.tell(new DOMEntityOwnershipChange(entity, EntityOwnershipChangeState.from(
                wasOwner, isOwner, hasOwner)), ActorRef.noSender());

        verify(mockListener, timeout(5000)).ownershipChanged(ownershipChange(entity, wasOwner, isOwner, hasOwner));
    }

    @Test
    public void testOnEntityOwnershipChangedWithListenerEx() {
        DOMEntityOwnershipListener mockListener = mock(DOMEntityOwnershipListener.class);

        DOMEntity entity1 = new DOMEntity("test", YangInstanceIdentifier.of(QName.create("test", "id1")));
        doThrow(new RuntimeException("mock")).when(mockListener).ownershipChanged(
                ownershipChange(entity1, false, true, true));
        DOMEntity entity2 = new DOMEntity("test", YangInstanceIdentifier.of(QName.create("test", "id2")));
        doNothing().when(mockListener).ownershipChanged(ownershipChange(entity2, true, false, false));

        TestActorRef<EntityOwnershipListenerActor> listenerActor = actorFactory.createTestActor(
                EntityOwnershipListenerActor.props(mockListener), actorFactory.generateActorId("listener"));

        listenerActor.tell(new DOMEntityOwnershipChange(entity1, EntityOwnershipChangeState.from(
                false, true, true)), ActorRef.noSender());
        listenerActor.tell(new DOMEntityOwnershipChange(entity2, EntityOwnershipChangeState.from(
                true, false, false)), ActorRef.noSender());

        verify(mockListener, timeout(5000)).ownershipChanged(ownershipChange(entity2, true, false, false));
    }
}
