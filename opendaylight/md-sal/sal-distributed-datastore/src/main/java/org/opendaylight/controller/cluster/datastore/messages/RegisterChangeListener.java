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
import org.opendaylight.controller.cluster.datastore.utils.InstanceIdentifierUtils;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.protobuff.messages.registration.ListenerRegistrationMessages;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class RegisterChangeListener implements SerializableMessage {
  public static final Class SERIALIZABLE_CLASS = ListenerRegistrationMessages.RegisterChangeListener.class;
    private final YangInstanceIdentifier path;
    private final ActorPath dataChangeListenerPath;
    private final AsyncDataBroker.DataChangeScope scope;


    public RegisterChangeListener(YangInstanceIdentifier path,
        ActorPath dataChangeListenerPath,
        AsyncDataBroker.DataChangeScope scope) {
        this.path = path;
        this.dataChangeListenerPath = dataChangeListenerPath;
        this.scope = scope;
    }

    public YangInstanceIdentifier getPath() {
        return path;
    }


    public AsyncDataBroker.DataChangeScope getScope() {
        return scope;
    }

    public ActorPath getDataChangeListenerPath() {
        return dataChangeListenerPath;
    }


    @Override
    public ListenerRegistrationMessages.RegisterChangeListener toSerializable() {
      return ListenerRegistrationMessages.RegisterChangeListener.newBuilder()
          .setInstanceIdentifierPath(InstanceIdentifierUtils.toSerializable(path))
          .setDataChangeListenerActorPath(dataChangeListenerPath.toString())
          .setDataChangeScope(scope.ordinal()).build();
    }

  public static RegisterChangeListener fromSerializable(ActorSystem actorSystem,Object serializable){
    ListenerRegistrationMessages.RegisterChangeListener o = (ListenerRegistrationMessages.RegisterChangeListener) serializable;
    return new RegisterChangeListener(InstanceIdentifierUtils.fromSerializable(o.getInstanceIdentifierPath()),
                                                actorSystem.actorFor(o.getDataChangeListenerActorPath()).path(),
                                              AsyncDataBroker.DataChangeScope.values()[o.getDataChangeScope()]);
  }


}
