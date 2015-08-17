/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.entityownership;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
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
import java.util.ArrayList;
import java.util.List;
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
        EntityOwnershipListener mockListener3 = mock(EntityOwnershipListener.class, "EntityOwnershipListener3");
        Entity entity1 = new Entity("type1", YangInstanceIdentifier.of(QName.create("test", "id1")));
        Entity entity2 = new Entity("type1", YangInstanceIdentifier.of(QName.create("test", "id2")));
        Entity entity3 = new Entity("type1", YangInstanceIdentifier.of(QName.create("test", "id3")));
        Entity entity4 = new Entity("type2", YangInstanceIdentifier.of(QName.create("test", "id4")));
        Entity entity5 = new Entity("noListener", YangInstanceIdentifier.of(QName.create("test", "id5")));

        // Add EntityOwnershipListener registrations.

        support.addEntityOwnershipListener(entity1, mockListener1);
        support.addEntityOwnershipListener(entity1, mockListener1); // register again - should be noop
        support.addEntityOwnershipListener(entity2, mockListener1);
        support.addEntityOwnershipListener(entity1, mockListener2);
        support.addEntityOwnershipListener(entity1.getType(), mockListener3);

        // Notify entity1 changed and verify listeners are notified.

        support.notifyEntityOwnershipListeners(entity1, false, true);

        verify(mockListener1, timeout(5000)).ownershipChanged(entity1, false, true);
        verify(mockListener2, timeout(5000)).ownershipChanged(entity1, false, true);
        verify(mockListener3, timeout(5000)).ownershipChanged(entity1, false, true);
        assertEquals("# of listener actors", 3, actorContext.children().size());

        // Notify entity2 changed and verify only mockListener1 and mockListener3 are notified.

        support.notifyEntityOwnershipListeners(entity2, false, true);

        verify(mockListener1, timeout(5000)).ownershipChanged(entity2, false, true);
        verify(mockListener3, timeout(5000)).ownershipChanged(entity2, false, true);
        Uninterruptibles.sleepUninterruptibly(300, TimeUnit.MILLISECONDS);
        verify(mockListener2, never()).ownershipChanged(eq(entity2), anyBoolean(), anyBoolean());
        assertEquals("# of listener actors", 3, actorContext.children().size());

        // Notify entity3 changed and verify only mockListener3 is notified.

        support.notifyEntityOwnershipListeners(entity3, false, true);

        verify(mockListener3, timeout(5000)).ownershipChanged(entity3, false, true);
        Uninterruptibles.sleepUninterruptibly(300, TimeUnit.MILLISECONDS);
        verify(mockListener1, never()).ownershipChanged(eq(entity3), anyBoolean(), anyBoolean());
        verify(mockListener2, never()).ownershipChanged(eq(entity3), anyBoolean(), anyBoolean());

        // Notify entity4 changed and verify no listeners are notified.

        support.notifyEntityOwnershipListeners(entity4, false, true);

        Uninterruptibles.sleepUninterruptibly(300, TimeUnit.MILLISECONDS);
        verify(mockListener1, never()).ownershipChanged(eq(entity4), anyBoolean(), anyBoolean());
        verify(mockListener2, never()).ownershipChanged(eq(entity4), anyBoolean(), anyBoolean());
        verify(mockListener3, never()).ownershipChanged(eq(entity4), anyBoolean(), anyBoolean());

        // Notify entity5 changed and verify no listener is notified.

        support.notifyEntityOwnershipListeners(entity5, false, true);

        Uninterruptibles.sleepUninterruptibly(300, TimeUnit.MILLISECONDS);
        verify(mockListener1, never()).ownershipChanged(eq(entity4), anyBoolean(), anyBoolean());
        verify(mockListener2, never()).ownershipChanged(eq(entity4), anyBoolean(), anyBoolean());
        verify(mockListener3, never()).ownershipChanged(eq(entity4), anyBoolean(), anyBoolean());

        reset(mockListener1, mockListener2, mockListener3);

        // Unregister mockListener1 for entity1, issue a change and verify only mockListeners 2 & 3 are notified.

        support.removeEntityOwnershipListener(entity1, mockListener1);
        support.notifyEntityOwnershipListeners(entity1, false, true);

        verify(mockListener2, timeout(5000)).ownershipChanged(entity1, false, true);
        verify(mockListener3, timeout(5000)).ownershipChanged(entity1, false, true);
        Uninterruptibles.sleepUninterruptibly(300, TimeUnit.MILLISECONDS);
        verify(mockListener1, never()).ownershipChanged(eq(entity1), anyBoolean(), anyBoolean());

        // Unregister mockListener3, issue a change for entity1 and verify only mockListeners2 is notified.

        reset(mockListener1, mockListener2, mockListener3);

        support.removeEntityOwnershipListener(entity1.getType(), mockListener3);
        support.notifyEntityOwnershipListeners(entity1, false, true);

        verify(mockListener2, timeout(5000)).ownershipChanged(entity1, false, true);
        Uninterruptibles.sleepUninterruptibly(300, TimeUnit.MILLISECONDS);
        verify(mockListener1, never()).ownershipChanged(eq(entity1), anyBoolean(), anyBoolean());
        verify(mockListener3, never()).ownershipChanged(eq(entity1), anyBoolean(), anyBoolean());

        // Completely unregister all listeners and verify their listener actors are destroyed.

        Iterable<ActorRef> listenerActors = actorContext.children();
        assertEquals("# of listener actors", 2, listenerActors.size());

        List<JavaTestKit> watchers = new ArrayList<>();
        for(Iterator<ActorRef> iter = listenerActors.iterator(); iter.hasNext();) {
            JavaTestKit kit = new JavaTestKit(getSystem());
            kit.watch(iter.next());
            watchers.add(kit);
        }

        support.removeEntityOwnershipListener(entity2, mockListener1);
        support.removeEntityOwnershipListener(entity2, mockListener1); // un-register again - shoild be noop
        support.removeEntityOwnershipListener(entity1, mockListener2);

        Iterator<ActorRef> iter = listenerActors.iterator();
        for(JavaTestKit kit: watchers) {
            kit.expectTerminated(JavaTestKit.duration("3 seconds"), iter.next());
        }

        assertEquals("# of listener actors", 0, actorContext.children().size());

        // Re-register mockListener1 for entity1 and verify it is notified.

        reset(mockListener1, mockListener2);

        support.addEntityOwnershipListener(entity1, mockListener1);

        support.notifyEntityOwnershipListeners(entity1, false, true);

        verify(mockListener1, timeout(5000)).ownershipChanged(entity1, false, true);
        verify(mockListener2, never()).ownershipChanged(eq(entity1), anyBoolean(), anyBoolean());
        verify(mockListener3, never()).ownershipChanged(eq(entity1), anyBoolean(), anyBoolean());

        // Quickly register and unregister mockListener2 - expecting no exceptions.

        support.addEntityOwnershipListener(entity1, mockListener2);
        support.removeEntityOwnershipListener(entity1, mockListener2);
    }
}
