/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.Props;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.cluster.datastore.messages.DataTreeChanged;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;

public class ForwardingDataTreeChangeListenerTest extends AbstractActorTest {

    @Test
    public void testOnDataChanged() throws Exception {
        final Props props = Props.create(MessageCollectorActor.class);
        final ActorRef actorRef = getSystem().actorOf(props);

        ForwardingDataTreeChangeListener forwardingListener = new ForwardingDataTreeChangeListener(
                getSystem().actorSelection(actorRef.path()));

        Collection<DataTreeCandidate> expected = Arrays.asList(Mockito.mock(DataTreeCandidate.class));
        forwardingListener.onDataTreeChanged(expected);

        DataTreeChanged actual = MessageCollectorActor.expectFirstMatching(actorRef, DataTreeChanged.class, 5000);
        Assert.assertSame(expected, actual.getChanges());
    }
}
