/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.entityownership;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import org.junit.After;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.TestActorFactory;
import org.opendaylight.mdsal.common.api.clustering.EntityOwnershipChangeState;
import org.opendaylight.mdsal.dom.api.clustering.DOMEntity;
import org.opendaylight.mdsal.dom.api.clustering.DOMEntityOwnershipChange;
import org.opendaylight.mdsal.dom.api.clustering.DOMEntityOwnershipListener;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import akka.actor.ActorRef;
import akka.testkit.TestActorRef;

/**
 * Unit tests for DOMEntityOwnershipListenerActor.
 *
 */
public class DOMEntityOwnershipListenerActorTest extends AbstractEntityOwnershipTest {
    private final TestActorFactory actorFactory = new TestActorFactory(getSystem());

    @After
    public void tearDown() {
        actorFactory.close();
    }

    @Test
    public void testOnEntityOwnershipChanged() {
        final DOMEntityOwnershipListener mockListener = mock(DOMEntityOwnershipListener.class);

        final TestActorRef<DOMEntityOwnershipListenerActor> listenerActor = actorFactory.createTestActor(
                DOMEntityOwnershipListenerActor.props(mockListener), actorFactory.generateActorId("listener"));

        final DOMEntity entity = new DOMEntity("test", YangInstanceIdentifier.of(QName.create("test", "id1")));
        final boolean wasOwner = false;
        final boolean isOwner = true;
        final boolean hasOwner = true;
        listenerActor.tell(
                new DOMEntityOwnershipChange(entity, EntityOwnershipChangeState.from(wasOwner, isOwner, hasOwner)),
                ActorRef.noSender());

        verify(mockListener, timeout(5000)).ownershipChanged(ownershipChange(entity, wasOwner, isOwner, hasOwner));
    }

    @Test
    public void testOnEntityOwnershipChangedWithListenerEx() {
        final DOMEntityOwnershipListener mockListener = mock(DOMEntityOwnershipListener.class);

        final DOMEntity entity1 = new DOMEntity("test", YangInstanceIdentifier.of(QName.create("test", "id1")));
        doThrow(new RuntimeException("mock")).when(mockListener)
                .ownershipChanged(ownershipChange(entity1, false, true, true));
        final DOMEntity entity2 = new DOMEntity("test", YangInstanceIdentifier.of(QName.create("test", "id2")));
        doNothing().when(mockListener).ownershipChanged(ownershipChange(entity2, true, false, false));

        final TestActorRef<DOMEntityOwnershipListenerActor> listenerActor = actorFactory.createTestActor(
                DOMEntityOwnershipListenerActor.props(mockListener), actorFactory.generateActorId("listener"));

        listenerActor.tell(new DOMEntityOwnershipChange(entity1, EntityOwnershipChangeState.from(false, true, true)),
                ActorRef.noSender());
        listenerActor.tell(new DOMEntityOwnershipChange(entity2, EntityOwnershipChangeState.from(true, false, false)),
                ActorRef.noSender());

        verify(mockListener, timeout(5000)).ownershipChanged(ownershipChange(entity2, true, false, false));
    }
}
