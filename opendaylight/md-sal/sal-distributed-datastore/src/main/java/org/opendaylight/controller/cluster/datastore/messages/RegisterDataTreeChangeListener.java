/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import akka.actor.ActorRef;
import com.google.common.base.Preconditions;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.datastore.utils.SerializationUtils;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Request a {@link org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener} registration be made on the shard
 * leader.
 */
public final class RegisterDataTreeChangeListener implements Externalizable, ListenerRegistrationMessage {
    private static final long serialVersionUID = 1L;
    private ActorRef dataTreeChangeListenerPath;
    private YangInstanceIdentifier path;
    private boolean registerOnAllInstances;

    public RegisterDataTreeChangeListener(final YangInstanceIdentifier path, final ActorRef dataTreeChangeListenerPath,
            final boolean registerOnAllInstances) {
        this.path = Preconditions.checkNotNull(path);
        this.dataTreeChangeListenerPath = Preconditions.checkNotNull(dataTreeChangeListenerPath);
        this.registerOnAllInstances = registerOnAllInstances;
    }

    @Override
    public YangInstanceIdentifier getPath() {
        return path;
    }

    public ActorRef getDataTreeChangeListenerPath() {
        return dataTreeChangeListenerPath;
    }

    @Override
    public boolean isRegisterOnAllInstances() {
        return registerOnAllInstances;
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeObject(dataTreeChangeListenerPath);
        SerializationUtils.serializePath(path, out);
        out.writeBoolean(registerOnAllInstances);
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        dataTreeChangeListenerPath = (ActorRef) in.readObject();
        path = SerializationUtils.deserializePath(in);
        registerOnAllInstances = in.readBoolean();
    }
}
