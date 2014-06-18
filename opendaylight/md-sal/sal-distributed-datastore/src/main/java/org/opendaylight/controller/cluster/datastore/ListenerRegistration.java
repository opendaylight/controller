/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import org.opendaylight.controller.cluster.datastore.messages.CloseListenerRegistration;
import org.opendaylight.controller.cluster.datastore.messages.CloseListenerRegistrationReply;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class ListenerRegistration extends UntypedActor{

  private final org.opendaylight.yangtools.concepts.ListenerRegistration<AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>>> registration;

  public ListenerRegistration(org.opendaylight.yangtools.concepts.ListenerRegistration<AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>>> registration) {
    this.registration = registration;
  }

  @Override
  public void onReceive(Object message) throws Exception {
    if(message instanceof CloseListenerRegistration){
      closeListenerRegistration((CloseListenerRegistration) message);
    }
  }

  public static Props props(final org.opendaylight.yangtools.concepts.ListenerRegistration<AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>>> registration){
    return Props.create(new Creator<ListenerRegistration>(){

      @Override
      public ListenerRegistration create() throws Exception {
        return new ListenerRegistration(registration);
      }
    });
  }

  private void closeListenerRegistration(CloseListenerRegistration message){
    registration.close();
    getSender().tell(new CloseListenerRegistrationReply(), getSelf());
  }
}
