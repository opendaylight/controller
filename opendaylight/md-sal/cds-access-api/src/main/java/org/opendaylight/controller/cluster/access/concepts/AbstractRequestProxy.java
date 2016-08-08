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
import com.google.common.annotations.Beta;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.concepts.WritableIdentifier;
import org.opendaylight.yangtools.concepts.WritableObjects;

/**
 * Abstract Externalizable proxy for use with {@link Request} subclasses.
 *
 * @author Robert Varga
 *
 * @param <T> Target identifier type
 */
@Beta
public abstract class AbstractRequestProxy<T extends WritableIdentifier, C extends Request<T, C>>
        extends AbstractMessageProxy<T, C> {
    private static final long serialVersionUID = 1L;
    private ActorRef replyTo;
    private long sequence;

    protected AbstractRequestProxy() {
        // For Externalizable
    }

    protected AbstractRequestProxy(final @Nonnull C request) {
        super(request);
        this.replyTo = request.getReplyTo();
        this.sequence = request.getSequence();
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        super.writeExternal(out);
        WritableObjects.writeLong(out, sequence);
        out.writeObject(Serialization.serializedActorPath(replyTo));
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        sequence = WritableObjects.readLong(in);
        replyTo = JavaSerializer.currentSystem().value().provider().resolveActorRef((String) in.readObject());
    }

    @Override
    final @Nonnull C createMessage(@Nonnull final T target) {
        return createRequest(target, sequence, replyTo);
    }

    protected abstract @Nonnull C createRequest(@Nonnull T target, long sequence, @Nonnull ActorRef replyTo);
}