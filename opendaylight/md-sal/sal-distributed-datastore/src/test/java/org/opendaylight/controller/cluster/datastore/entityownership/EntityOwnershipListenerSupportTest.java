/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.entityownership;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActorContext;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.AbstractActorTest;
import org.opendaylight.controller.cluster.raft.TestActorFactory;
import org.opendaylight.controller.cluster.raft.utils.DoNothingActor;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListener;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import scala.collection.Iterator;
import scala.collection.immutable.Iterable;

/**
 * Unit tests for EntityOwnershipListenerSupport.
 *
 * @author Thomas Pantelis
 */
public class EntityOwnershipListenerSupportTest extends AbstractActorTest {
    private final TestActorFactory actorFactory = new TestActorFactory(getSystem());

    @After
    public void tearDown() {
        actorFactory.close();
    }

    @Test
    public void testNotifyEntityOwnershipListeners() {
        TestActorRef<DoNothingActor> actor = actorFactory.createTestActor(
                Props.create(DoNothingActor.class), actorFactory.generateActorId("test"));

        UntypedActorContext actorContext = actor.underlyingActor().getContext();
        EntityOwnershipListenerSupport support = new EntityOwnershipListenerSupport(actorContext);

        EntityOwnershipListener mockListener1 = mock(EntityOwnershipListener.class, "EntityOwnershipListener1");
        EntityOwnershipListener mockListener2 = mock(EntityOwnershipListener.class, "EntityOwnershipListener2");
        Entity entity1 = new Entity("test", YangInstanceIdentifier.of(QName.create("test", "id1")));
        Entity entity2 = new Entity("test", YangInstanceIdentifier.of(QName.create("test", "id2")));

        // Add EntityOwnershipListener registrations.

        support.addEntityOwnershipListener(entity1, mockListener1);
        support.addEntityOwnershipListener(entity2, mockListener1);
        support.addEntityOwnershipListener(entity1, mockListener2);

        // Notify entity1 changed and verify both listeners are notified.

        support.notifyEntityOwnershipListeners(entity1, false, true);

        verify(mockListener1, timeout(5000)).ownershipChanged(entity1, false, true);
        verify(mockListener2, timeout(5000)).ownershipChanged(entity1, false, true);
        assertEquals("# of listener actors", 2, actorContext.children().size());

        // Notify entity2 changed and verify only mockListener1 is notified.

        support.notifyEntityOwnershipListeners(entity2, false, true);

        verify(mockListener1, timeout(5000)).ownershipChanged(entity2, false, true);
        verify(mockListener2, never()).ownershipChanged(entity2, false, true);
        assertEquals("# of listener actors", 2, actorContext.children().size());

        // Notify entity3 changed and verify neither listener is notified.

        Entity entity3 = new Entity("test", YangInstanceIdentifier.of(QName.create("test", "id3")));
        support.notifyEntityOwnershipListeners(entity3, false, true);

        Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);

        verify(mockListener1, never()).ownershipChanged(entity3, false, true);
        verify(mockListener2, never()).ownershipChanged(entity3, false, true);

        reset(mockListener1, mockListener2);

        // Unregister mockListener1 for entity1, issue a change and verify only mockListener2 is notified.

        support.removeEntityOwnershipListener(entity1, mockListener1);

        support.notifyEntityOwnershipListeners(entity1, false, true);

        verify(mockListener2, timeout(5000)).ownershipChanged(entity1, false, true);
        verify(mockListener1, never()).ownershipChanged(entity1, false, true);

        // Completely unregister both listeners and verify their listener actors are destroyed.

        Iterable<ActorRef> listenerActors = actorContext.children();
        assertEquals("# of listener actors", 2, listenerActors.size());

        Iterator<ActorRef> iter = listenerActors.iterator();
        ActorRef listenerActor1 = iter.next();
        ActorRef listenerActor2 = iter.next();

        JavaTestKit kit1 = new JavaTestKit(getSystem());
        kit1.watch(listenerActor1);

        JavaTestKit kit2 = new JavaTestKit(getSystem());
        kit2.watch(listenerActor2);

        support.removeEntityOwnershipListener(entity2, mockListener1);
        support.removeEntityOwnershipListener(entity1, mockListener2);

        kit1.expectTerminated(JavaTestKit.duration("3 seconds"), listenerActor1);
        kit2.expectTerminated(JavaTestKit.duration("3 seconds"), listenerActor2);
        assertEquals("# of listener actors", 0, actorContext.children().size());

        // Re-register mockListener1 for entity1 and verify it is notified.

        reset(mockListener1, mockListener2);

        support.addEntityOwnershipListener(entity1, mockListener1);

        support.notifyEntityOwnershipListeners(entity1, false, true);

        verify(mockListener1, timeout(5000)).ownershipChanged(entity1, false, true);
        verify(mockListener2, never()).ownershipChanged(entity2, false, true);

        // Quickly register and unregister mockListener2 - expecting no exceptions.

        support.addEntityOwnershipListener(entity1, mockListener2);
        support.removeEntityOwnershipListener(entity1, mockListener2);
    }
}
