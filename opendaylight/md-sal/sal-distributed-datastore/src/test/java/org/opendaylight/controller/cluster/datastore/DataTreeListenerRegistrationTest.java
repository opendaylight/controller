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
import org.opendaylight.controller.cluster.datastore.messages.CloseDataTreeChangeListenerRegistration;
import org.opendaylight.controller.cluster.datastore.messages.CloseDataTreeChangeListenerRegistrationReply;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;

import javax.annotation.Nonnull;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

public class DataTreeListenerRegistrationTest extends AbstractActorTest {
    private static final InMemoryDOMDataStore store = new InMemoryDOMDataStore("OPER", MoreExecutors.sameThreadExecutor());

    static {
        store.onGlobalContextUpdated(TestModel.createTestContext());
    }

    @Test
    public void testOnReceiveCloseListenerRegistration() throws Exception {
        new JavaTestKit(getSystem()) {{
            final Props props = DataTreeChangeListenerRegistrationActor.props(store
                    .registerTreeChangeListener(TestModel.TEST_PATH, noOpDataTreeChangeListener()));
            final ActorRef subject = getSystem().actorOf(props, "testCloseListenerRegistration");

            new Within(duration("1 seconds")) {
                @Override
                protected void run() {

                    subject.tell(CloseDataTreeChangeListenerRegistration.getInstance(), getRef());

                    final String out = new ExpectMsg<String>(duration("1 seconds"), "match hint") {
                        // do not put code outside this method, will run afterwards
                        @Override
                        protected String match(final Object in) {
                            if (in.getClass().equals(CloseDataTreeChangeListenerRegistrationReply.class)) {
                                return "match";
                            } else {
                                throw noMatch();
                            }
                        }
                    }.get(); // this extracts the received message

                    assertEquals("match", out);

                    expectNoMsg();
                }
            };
        }};
    }

    private DOMDataTreeChangeListener noOpDataTreeChangeListener() {
        return new DOMDataTreeChangeListener() {

            @Override
            public void onDataTreeChanged(@Nonnull final Collection<DataTreeCandidate> changes) {

            }
        };
    }
}
