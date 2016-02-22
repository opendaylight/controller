/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.entityownership;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.TestActorFactory;
import org.opendaylight.controller.cluster.raft.utils.DoNothingActor;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipChange;
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
public class EntityOwnershipListenerSupportTest extends AbstractEntityOwnershipTest {
    private final TestActorFactory actorFactory = new TestActorFactory(getSystem());
    private ActorContext actorContext;

    @Before
    public void setup() {
        TestActorRef<DoNothingActor> actor = actorFactory.createTestActor(
                Props.create(DoNothingActor.class), actorFactory.generateActorId("test"));

        actorContext = actor.underlyingActor().getContext();
    }

    @After
    public void tearDown() {
        actorFactory.close();
    }

    @Test
    public void testNotifyEntityOwnershipListeners() {
        EntityOwnershipListenerSupport support = new EntityOwnershipListenerSupport(actorContext, "test");

        EntityOwnershipListener mockListener1 = mock(EntityOwnershipListener.class, "EntityOwnershipListener1");
        EntityOwnershipListener mockListener2 = mock(EntityOwnershipListener.class, "EntityOwnershipListener2");
        EntityOwnershipListener mockListener1_2 = mock(EntityOwnershipListener.class, "EntityOwnershipListener1_2");
        String entityType1 = "type1";
        String entityType2 = "type2";
        Entity entity1 = new Entity(entityType1, YangInstanceIdentifier.of(QName.create("test", "id1")));
        Entity entity2 = new Entity(entityType2, YangInstanceIdentifier.of(QName.create("test", "id2")));
        Entity entity3 = new Entity("noListener", YangInstanceIdentifier.of(QName.create("test", "id5")));

        // Add EntityOwnershipListener registrations.

        support.addEntityOwnershipListener(entityType1, mockListener1);
        support.addEntityOwnershipListener(entityType1, mockListener1); // register again - should be noop
        support.addEntityOwnershipListener(entityType1, mockListener1_2);
        support.addEntityOwnershipListener(entityType2, mockListener2);

        // Notify entity1 changed and verify appropriate listeners are notified.

        support.notifyEntityOwnershipListeners(entity1, false, true, true);

        verify(mockListener1, timeout(5000)).ownershipChanged(ownershipChange(entity1, false, true, true));
        verify(mockListener1_2, timeout(5000)).ownershipChanged(ownershipChange(entity1, false, true, true));
        Uninterruptibles.sleepUninterruptibly(300, TimeUnit.MILLISECONDS);
        verify(mockListener2, never()).ownershipChanged(any(EntityOwnershipChange.class));
        assertEquals("# of listener actors", 2, actorContext.children().size());
        reset(mockListener1, mockListener2, mockListener1_2);

        // Notify entity2 changed and verify appropriate listeners are notified.

        support.notifyEntityOwnershipListeners(entity2, false, true, true);

        verify(mockListener2, timeout(5000)).ownershipChanged(ownershipChange(entity2, false, true, true));
        Uninterruptibles.sleepUninterruptibly(300, TimeUnit.MILLISECONDS);
        verify(mockListener1, never()).ownershipChanged(any(EntityOwnershipChange.class));
        verify(mockListener1_2, never()).ownershipChanged(any(EntityOwnershipChange.class));
        assertEquals("# of listener actors", 3, actorContext.children().size());
        reset(mockListener1, mockListener2, mockListener1_2);

        // Notify entity3 changed and verify no listeners are notified.

        support.notifyEntityOwnershipListeners(entity3, true, false, true);

        Uninterruptibles.sleepUninterruptibly(300, TimeUnit.MILLISECONDS);
        verify(mockListener1, never()).ownershipChanged(any(EntityOwnershipChange.class));
        verify(mockListener2, never()).ownershipChanged(any(EntityOwnershipChange.class));
        verify(mockListener1_2, never()).ownershipChanged(any(EntityOwnershipChange.class));
        reset(mockListener1, mockListener2, mockListener1_2);

        Iterable<ActorRef> listenerActors = actorContext.children();
        assertEquals("# of listener actors", 3, listenerActors.size());

        // Unregister mockListener1, issue a change for entity1 and verify only remaining listeners are notified.

        support.removeEntityOwnershipListener(entityType1, mockListener1);
        support.notifyEntityOwnershipListeners(entity1, true, false, true);

        verify(mockListener1_2, timeout(5000)).ownershipChanged(ownershipChange(entity1, true, false, true));
        Uninterruptibles.sleepUninterruptibly(300, TimeUnit.MILLISECONDS);
        verify(mockListener1, never()).ownershipChanged(any(EntityOwnershipChange.class));
        reset(mockListener1, mockListener2, mockListener1_2);

        // Unregister all listeners and verify their listener actors are destroyed.

        List<JavaTestKit> watchers = new ArrayList<>();
        for(Iterator<ActorRef> iter = listenerActors.iterator(); iter.hasNext();) {
            JavaTestKit kit = new JavaTestKit(getSystem());
            kit.watch(iter.next());
            watchers.add(kit);
        }

        support.removeEntityOwnershipListener(entityType1, mockListener1_2);
        support.removeEntityOwnershipListener(entityType1, mockListener1_2); // un-register again - should be noop
        support.removeEntityOwnershipListener(entityType2, mockListener2);

        Iterator<ActorRef> iter = listenerActors.iterator();
        for(JavaTestKit kit: watchers) {
            kit.expectTerminated(JavaTestKit.duration("3 seconds"), iter.next());
        }

        assertEquals("# of listener actors", 0, actorContext.children().size());

        // Re-register mockListener1 and verify it is notified.

        reset(mockListener1, mockListener2);

        support.addEntityOwnershipListener(entityType1, mockListener1);
        support.notifyEntityOwnershipListeners(entity1, false, false, true);

        verify(mockListener1, timeout(5000)).ownershipChanged(ownershipChange(entity1, false, false, true));
        verify(mockListener1_2, never()).ownershipChanged(any(EntityOwnershipChange.class));
        verify(mockListener2, never()).ownershipChanged(any(EntityOwnershipChange.class));

        // Quickly register and unregister mockListener2 - expecting no exceptions.

        support.addEntityOwnershipListener(entityType1, mockListener2);
        support.removeEntityOwnershipListener(entityType1, mockListener2);
    }

    @Test
    public void testHasCandidateForEntity() {
        EntityOwnershipListenerSupport support = new EntityOwnershipListenerSupport(actorContext, "test");
        Entity entity = new Entity("type", YangInstanceIdentifier.of(QName.create("test", "id")));

        assertEquals("hasCandidateForEntity", false, support.hasCandidateForEntity(entity));

        support.setHasCandidateForEntity(entity);
        support.setHasCandidateForEntity(entity); // set again - should be noop
        assertEquals("hasCandidateForEntity", true, support.hasCandidateForEntity(entity));

        support.unsetHasCandidateForEntity(entity);
        assertEquals("hasCandidateForEntity", false, support.hasCandidateForEntity(entity));

        support.unsetHasCandidateForEntity(entity); // unset again - should be noop
        assertEquals("hasCandidateForEntity", false, support.hasCandidateForEntity(entity));

        support.setHasCandidateForEntity(entity);
        assertEquals("hasCandidateForEntity", true, support.hasCandidateForEntity(entity));
    }
}
