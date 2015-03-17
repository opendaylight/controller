/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import com.google.common.base.Preconditions;
import akka.actor.ActorPath;
import akka.actor.ActorSystem;
import org.opendaylight.controller.cluster.datastore.util.InstanceIdentifierUtils;
import org.opendaylight.controller.protobuff.messages.registration.ListenerRegistrationMessages;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public final class RegisterTreeChangeListener implements SerializableMessage {
    public static final Class<ListenerRegistrationMessages.RegisterTreeChangeListener> SERIALIZABLE_CLASS =
            ListenerRegistrationMessages.RegisterTreeChangeListener.class;

    private final ActorPath dataTreeChangeListenerPath;
    private final YangInstanceIdentifier path;

    public RegisterTreeChangeListener(YangInstanceIdentifier path, ActorPath dataTreeChangeListenerPath) {
        this.path = Preconditions.checkNotNull(path);
        this.dataTreeChangeListenerPath = Preconditions.checkNotNull(dataTreeChangeListenerPath);
    }

    public YangInstanceIdentifier getPath() {
        return path;
    }

    public ActorPath getDataTreeChangeListenerPath() {
        return dataTreeChangeListenerPath;
    }

    @Override
    public Object toSerializable() {
        return ListenerRegistrationMessages.RegisterChangeListener.newBuilder()
                .setInstanceIdentifierPath(InstanceIdentifierUtils.toSerializable(path))
                .setDataChangeListenerActorPath(dataTreeChangeListenerPath.toString()).build();
    }

    public static RegisterTreeChangeListener fromSerializable(final ActorSystem actorSystem, final Object serializable) {
        ListenerRegistrationMessages.RegisterTreeChangeListener o = (ListenerRegistrationMessages.RegisterTreeChangeListener) serializable;
        return new RegisterTreeChangeListener(InstanceIdentifierUtils.fromSerializable(o.getInstanceIdentifierPath()),
            actorSystem.actorSelection(o.getDataTreeChangeListenerActorPath()).anchorPath());
    }
}
