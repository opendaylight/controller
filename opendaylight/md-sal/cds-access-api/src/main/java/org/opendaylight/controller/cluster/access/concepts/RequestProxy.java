/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import akka.actor.ActorRef;
import akka.serialization.JavaSerializer;
import akka.serialization.Serialization;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.WritableIdentifier;

/**
 * @author nite
 *
 */
public interface RequestProxy<T extends WritableIdentifier, C extends Request<T, C>> extends MessageProxy<T, C> {
    @Override
    default C readExternal(final ObjectInput in, final T target, final long sequence)
            throws ClassNotFoundException, IOException {
        return readExternal(in, target, sequence,
            JavaSerializer.currentSystem().value().provider().resolveActorRef((String) in.readObject()));
    }

    @NonNull C readExternal(@NonNull ObjectInput in, @NonNull T target, long sequence, @NonNull ActorRef replyTo)
        throws IOException;

    @Override
    default void writeExternal(final ObjectOutput out, final C msg) throws IOException {
        out.writeObject(Serialization.serializedActorPath(msg.getReplyTo()));
    }
}
