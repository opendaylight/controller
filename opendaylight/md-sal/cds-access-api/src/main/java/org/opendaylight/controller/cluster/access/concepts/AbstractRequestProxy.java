/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
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
import java.io.Serial;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.WritableIdentifier;

/**
 * Abstract Externalizable proxy for use with {@link Request} subclasses.
 *
 * @param <T> Target identifier type
 */
public abstract class AbstractRequestProxy<T extends WritableIdentifier, C extends Request<T, C>>
        extends AbstractMessageProxy<T, C> {
    @Serial
    private static final long serialVersionUID = 1L;

    private ActorRef replyTo;

    protected AbstractRequestProxy() {
        // For Externalizable
    }

    protected AbstractRequestProxy(final @NonNull C request) {
        super(request);
        this.replyTo = request.getReplyTo();
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeObject(Serialization.serializedActorPath(replyTo));
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        replyTo = JavaSerializer.currentSystem().value().provider().resolveActorRef((String) in.readObject());
    }

    @Override
    final C createMessage(final T target, final long sequence) {
        return createRequest(target, sequence, replyTo);
    }

    protected abstract @NonNull C createRequest(@NonNull T target, long sequence, @NonNull ActorRef replyToActor);
}
