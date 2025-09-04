/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import java.util.List;
import org.apache.pekko.actor.ActorRef;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.messages.DataTreeChanged;
import org.opendaylight.controller.cluster.raft.MessageCollectorActor;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;

public class ForwardingDataTreeChangeListenerTest extends AbstractActorTest {
    @Test
    public void testOnDataChanged() {
        final ActorRef actorRef = getSystem().actorOf(MessageCollectorActor.props());

        ForwardingDataTreeChangeListener forwardingListener = new ForwardingDataTreeChangeListener(
                getSystem().actorSelection(actorRef.path()), ActorRef.noSender());

        List<DataTreeCandidate> expected = List.of(mock(DataTreeCandidate.class));
        forwardingListener.onDataTreeChanged(expected);

        DataTreeChanged actual = MessageCollectorActor.expectFirstMatching(actorRef, DataTreeChanged.class, 5000);
        assertSame(expected, actual.changes());
    }
}
