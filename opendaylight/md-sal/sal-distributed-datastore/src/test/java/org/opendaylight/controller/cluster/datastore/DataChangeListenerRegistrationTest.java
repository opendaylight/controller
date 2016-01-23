/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.messages.CloseDataChangeListenerRegistration;
import org.opendaylight.controller.cluster.datastore.messages.CloseDataChangeListenerRegistrationReply;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class DataChangeListenerRegistrationTest extends AbstractActorTest {
  private static final InMemoryDOMDataStore store = new InMemoryDOMDataStore("OPER", MoreExecutors.newDirectExecutorService());

  static {
    store.onGlobalContextUpdated(TestModel.createTestContext());
  }


  @Test
  public void testOnReceiveCloseListenerRegistration() throws Exception {
    new JavaTestKit(getSystem()) {{
      final Props props = DataChangeListenerRegistrationActor.props(store
          .registerChangeListener(TestModel.TEST_PATH, noOpDataChangeListener(),
              AsyncDataBroker.DataChangeScope.BASE));
      final ActorRef subject = getSystem().actorOf(props, "testCloseListenerRegistration");

      new Within(duration("1 seconds")) {
        @Override
        protected void run() {

          subject.tell(CloseDataChangeListenerRegistration.INSTANCE, getRef());

          final String out = new ExpectMsg<String>(duration("1 seconds"), "match hint") {
            // do not put code outside this method, will run afterwards
            @Override
            protected String match(final Object in) {
              if (in.getClass().equals(CloseDataChangeListenerRegistrationReply.class)) {
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

  private static AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>> noOpDataChangeListener(){
    return new AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>() {
      @Override
      public void onDataChanged(final AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change) {

      }
    };
  }

}
