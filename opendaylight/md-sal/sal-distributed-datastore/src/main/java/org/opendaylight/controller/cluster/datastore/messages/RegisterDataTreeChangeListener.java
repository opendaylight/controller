/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import static java.util.Objects.requireNonNull;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.apache.pekko.actor.ActorPath;
import org.apache.pekko.actor.ActorRef;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.SerializationUtils;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Request a {@link org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener} registration be made on the shard leader.
 */
public final class RegisterDataTreeChangeListener implements Externalizable {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private ActorRef dataTreeChangeListenerPath;
    private YangInstanceIdentifier path;
    private boolean registerOnAllInstances;

    public RegisterDataTreeChangeListener() {
        // For Externalizable
    }

    public RegisterDataTreeChangeListener(final YangInstanceIdentifier path, final ActorRef dataTreeChangeListenerPath,
            final boolean registerOnAllInstances) {
        this.path = requireNonNull(path);
        this.dataTreeChangeListenerPath = requireNonNull(dataTreeChangeListenerPath);
        this.registerOnAllInstances = registerOnAllInstances;
    }

    public YangInstanceIdentifier getPath() {
        return path;
    }

    public ActorPath getListenerActorPath() {
        return dataTreeChangeListenerPath.path();
    }

    public boolean isRegisterOnAllInstances() {
        return registerOnAllInstances;
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeObject(dataTreeChangeListenerPath);
        SerializationUtils.writePath(out, path);
        out.writeBoolean(registerOnAllInstances);
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        dataTreeChangeListenerPath = (ActorRef) in.readObject();
        path = SerializationUtils.readPath(in);
        registerOnAllInstances = in.readBoolean();
    }

    @Override
    public String toString() {
        return "RegisterDataTreeChangeListener [path=" + path + ", registerOnAllInstances=" + registerOnAllInstances
                + ", dataTreeChangeListenerPath=" + dataTreeChangeListenerPath + "]";
    }
}
