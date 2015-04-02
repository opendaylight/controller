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
import akka.testkit.JavaTestKit;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.cluster.datastore.messages.CloseDataTreeChangeListenerRegistration;
import org.opendaylight.controller.cluster.datastore.messages.CloseDataTreeChangeListenerRegistrationReply;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;

public class DataTreeChangeListenerRegistrationActorTest extends AbstractActorTest {
    private static final InMemoryDOMDataStore store = new InMemoryDOMDataStore("OPER", MoreExecutors.sameThreadExecutor());

    static {
        store.onGlobalContextUpdated(TestModel.createTestContext());
    }

    @Test
    public void testOnReceiveCloseListenerRegistration() throws Exception {
        new JavaTestKit(getSystem()) {{
            final DOMDataTreeChangeListener listenerMock = Mockito.mock(DOMDataTreeChangeListener.class);
            final Props props = DataTreeChangeListenerRegistrationActor.props(store
                    .registerTreeChangeListener(TestModel.TEST_PATH, listenerMock));
            final ActorRef subject = getSystem().actorOf(props, "testCloseListenerRegistration");

            subject.tell(CloseDataTreeChangeListenerRegistration.getInstance(), getRef());

            expectMsgClass(duration("1 second"),
                    CloseDataTreeChangeListenerRegistrationReply.class);
        }};
    }
}
