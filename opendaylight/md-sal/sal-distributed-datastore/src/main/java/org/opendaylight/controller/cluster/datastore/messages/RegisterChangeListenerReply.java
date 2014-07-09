/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import akka.actor.ActorPath;
import akka.actor.ActorSystem;
import org.opendaylight.controller.protobuff.messages.registration.ListenerRegistrationMessages;

public class RegisterChangeListenerReply implements SerializableMessage{
  public static final Class SERIALIZABLE_CLASS = ListenerRegistrationMessages.RegisterChangeListenerReply.class;
  private final ActorPath listenerRegistrationPath;

  public RegisterChangeListenerReply(ActorPath listenerRegistrationPath) {
    this.listenerRegistrationPath = listenerRegistrationPath;
  }

  public ActorPath getListenerRegistrationPath() {
    return listenerRegistrationPath;
  }

  @Override
  public ListenerRegistrationMessages.RegisterChangeListenerReply toSerializable() {
    return ListenerRegistrationMessages.RegisterChangeListenerReply.newBuilder()
            .setListenerRegistrationPath(listenerRegistrationPath.toString()).build();
  }

  public static RegisterChangeListenerReply fromSerializable(ActorSystem actorSystem,Object serializable){
    ListenerRegistrationMessages.RegisterChangeListenerReply o = (ListenerRegistrationMessages.RegisterChangeListenerReply) serializable;
    return new RegisterChangeListenerReply(
        actorSystem.actorFor(o.getListenerRegistrationPath()).path()
        );
  }
}
