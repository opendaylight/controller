/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
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
import akka.actor.ActorRef;
import akka.testkit.TestActorRef;
import org.junit.After;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.AbstractActorTest;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.EntityOwnershipChanged;
import org.opendaylight.controller.cluster.raft.TestActorFactory;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListener;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Unit tests for EntityOwnershipListenerActor.
 *
 * @author Thomas Pantelis
 */
public class EntityOwnershipListenerActorTest extends AbstractActorTest {
    private final TestActorFactory actorFactory = new TestActorFactory(getSystem());

    @After
    public void tearDown() {
        actorFactory.close();
    }

    @Test
    public void testOnEntityOwnershipChanged() {
        EntityOwnershipListener mockListener = mock(EntityOwnershipListener.class);

        TestActorRef<EntityOwnershipListenerActor> listenerActor = actorFactory.createTestActor(
                EntityOwnershipListenerActor.props(mockListener), actorFactory.generateActorId("listener"));

        Entity entity = new Entity("test", YangInstanceIdentifier.of(QName.create("test", "id1")));
        boolean wasOwner = false;
        boolean isOwner = true;
        listenerActor.tell(new EntityOwnershipChanged(entity, wasOwner, isOwner), ActorRef.noSender());

        verify(mockListener, timeout(5000)).ownershipChanged(entity, wasOwner, isOwner);
    }

    @Test
    public void testOnEntityOwnershipChangedWithListenerEx() {
        EntityOwnershipListener mockListener = mock(EntityOwnershipListener.class);

        Entity entity1 = new Entity("test", YangInstanceIdentifier.of(QName.create("test", "id1")));
        doThrow(new RuntimeException("mock")).when(mockListener).ownershipChanged(entity1, false, true);
        Entity entity2 = new Entity("test", YangInstanceIdentifier.of(QName.create("test", "id2")));
        doNothing().when(mockListener).ownershipChanged(entity2, true, false);

        TestActorRef<EntityOwnershipListenerActor> listenerActor = actorFactory.createTestActor(
                EntityOwnershipListenerActor.props(mockListener), actorFactory.generateActorId("listener"));

        listenerActor.tell(new EntityOwnershipChanged(entity1, false, true), ActorRef.noSender());
        listenerActor.tell(new EntityOwnershipChanged(entity2, true, false), ActorRef.noSender());

        verify(mockListener, timeout(5000)).ownershipChanged(entity2, true, false);
    }
}
