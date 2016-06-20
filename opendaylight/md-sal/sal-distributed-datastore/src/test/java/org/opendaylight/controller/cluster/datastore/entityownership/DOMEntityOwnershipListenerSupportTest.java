/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
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
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.TestActorFactory;
import org.opendaylight.controller.cluster.raft.utils.DoNothingActor;
import org.opendaylight.mdsal.dom.api.clustering.DOMEntity;
import org.opendaylight.mdsal.dom.api.clustering.DOMEntityOwnershipChange;
import org.opendaylight.mdsal.dom.api.clustering.DOMEntityOwnershipListener;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import scala.collection.Iterator;
import scala.collection.immutable.Iterable;

/**
 * Unit tests for EntityOwnershipListenerSupport.
 *
 */
public class DOMEntityOwnershipListenerSupportTest extends AbstractEntityOwnershipTest {

    private final TestActorFactory actorFactory = new TestActorFactory(getSystem());
    private ActorContext actorContext;

    @Before
    public void setup() {
        final TestActorRef<DoNothingActor> actor = actorFactory.createTestActor(Props.create(DoNothingActor.class),
                actorFactory.generateActorId("test"));

        actorContext = actor.underlyingActor().getContext();
    }

    @After
    public void tearDown() {
        actorFactory.close();
    }

    @Test
    public void testNotifyEntityOwnershipListeners() {
        final DOMEntityOwnershipListenerSupport support = new DOMEntityOwnershipListenerSupport(actorContext, "test");

        final DOMEntityOwnershipListener mockListener1 = mock(DOMEntityOwnershipListener.class,
                "DOMEntityOwnershipListener1");
        final DOMEntityOwnershipListener mockListener2 = mock(DOMEntityOwnershipListener.class,
                "DOMEntityOwnershipListener2");
        final DOMEntityOwnershipListener mockListener1_2 = mock(DOMEntityOwnershipListener.class,
                "DOMEntityOwnershipListener1_2");
        final String entityType1 = "type1";
        final String entityType2 = "type2";
        final DOMEntity entity1 = new DOMEntity(entityType1, YangInstanceIdentifier.of(QName.create("test", "id1")));
        final DOMEntity entity2 = new DOMEntity(entityType2, YangInstanceIdentifier.of(QName.create("test", "id2")));
        final DOMEntity entity3 = new DOMEntity("noListener", YangInstanceIdentifier.of(QName.create("test", "id5")));

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
        verify(mockListener2, never()).ownershipChanged(any(DOMEntityOwnershipChange.class));
        assertEquals("# of listener actors", 2, actorContext.children().size());
        reset(mockListener1, mockListener2, mockListener1_2);

        // Notify entity2 changed and verify appropriate listeners are notified.

        support.notifyEntityOwnershipListeners(entity2, false, true, true);

        verify(mockListener2, timeout(5000)).ownershipChanged(ownershipChange(entity2, false, true, true));
        Uninterruptibles.sleepUninterruptibly(300, TimeUnit.MILLISECONDS);
        verify(mockListener1, never()).ownershipChanged(any(DOMEntityOwnershipChange.class));
        verify(mockListener1_2, never()).ownershipChanged(any(DOMEntityOwnershipChange.class));
        assertEquals("# of listener actors", 3, actorContext.children().size());
        reset(mockListener1, mockListener2, mockListener1_2);

        // Notify entity3 changed and verify no listeners are notified.

        support.notifyEntityOwnershipListeners(entity3, true, false, true);

        Uninterruptibles.sleepUninterruptibly(300, TimeUnit.MILLISECONDS);
        verify(mockListener1, never()).ownershipChanged(any(DOMEntityOwnershipChange.class));
        verify(mockListener2, never()).ownershipChanged(any(DOMEntityOwnershipChange.class));
        verify(mockListener1_2, never()).ownershipChanged(any(DOMEntityOwnershipChange.class));
        reset(mockListener1, mockListener2, mockListener1_2);

        final Iterable<ActorRef> listenerActors = actorContext.children();
        assertEquals("# of listener actors", 3, listenerActors.size());

        // Unregister mockListener1, issue a change for entity1 and verify only remaining listeners are notified.

        support.removeEntityOwnershipListener(entityType1, mockListener1);
        support.notifyEntityOwnershipListeners(entity1, true, false, true);

        verify(mockListener1_2, timeout(5000)).ownershipChanged(ownershipChange(entity1, true, false, true));
        Uninterruptibles.sleepUninterruptibly(300, TimeUnit.MILLISECONDS);
        verify(mockListener1, never()).ownershipChanged(any(DOMEntityOwnershipChange.class));
        reset(mockListener1, mockListener2, mockListener1_2);

        // Unregister all listeners and verify their listener actors are destroyed.

        final List<JavaTestKit> watchers = new ArrayList<>();
        for (final Iterator<ActorRef> iter = listenerActors.iterator(); iter.hasNext();) {
            final JavaTestKit kit = new JavaTestKit(getSystem());
            kit.watch(iter.next());
            watchers.add(kit);
        }

        support.removeEntityOwnershipListener(entityType1, mockListener1_2);
        support.removeEntityOwnershipListener(entityType1, mockListener1_2); // un-register again - should be noop
        support.removeEntityOwnershipListener(entityType2, mockListener2);

        final Iterator<ActorRef> iter = listenerActors.iterator();
        for (final JavaTestKit kit : watchers) {
            kit.expectTerminated(JavaTestKit.duration("3 seconds"), iter.next());
        }

        assertEquals("# of listener actors", 0, actorContext.children().size());

        // Re-register mockListener1 and verify it is notified.

        reset(mockListener1, mockListener2);

        support.addEntityOwnershipListener(entityType1, mockListener1);
        support.notifyEntityOwnershipListeners(entity1, false, false, true);

        verify(mockListener1, timeout(5000)).ownershipChanged(ownershipChange(entity1, false, false, true));
        verify(mockListener1_2, never()).ownershipChanged(any(DOMEntityOwnershipChange.class));
        verify(mockListener2, never()).ownershipChanged(any(DOMEntityOwnershipChange.class));

        // Quickly register and unregister mockListener2 - expecting no exceptions.

        support.addEntityOwnershipListener(entityType1, mockListener2);
        support.removeEntityOwnershipListener(entityType1, mockListener2);
    }

    @Test
    public void testHasCandidateForEntity() {
        final DOMEntityOwnershipListenerSupport support = new DOMEntityOwnershipListenerSupport(actorContext, "test");
        final DOMEntity entity = new DOMEntity("type", YangInstanceIdentifier.of(QName.create("test", "id")));

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
