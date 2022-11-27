/*
 * Copyright (c) 2016, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import static java.util.Objects.requireNonNull;

import akka.actor.ActorRef;
import akka.serialization.JavaSerializer;
import akka.serialization.Serialization;
import com.google.common.base.MoreObjects.ToStringHelper;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serial;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.yangtools.concepts.WritableIdentifier;

/**
 * A request message concept. Upon receipt of this message, the recipient will respond with either
 * a {@link RequestSuccess} or a {@link RequestFailure} message.
 *
 * @param <T> Target identifier type
 * @param <C> Message type
 */
public abstract class Request<T extends WritableIdentifier, C extends Request<T, C>> extends Message<T, C> {
    protected interface SerialForm<T extends WritableIdentifier, C extends Request<T, C>>
            extends Message.SerialForm<T, C> {
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

    @Serial
    private static final long serialVersionUID = 1L;

    private final @NonNull ActorRef replyTo;

    protected Request(final @NonNull T target, final long sequence, final @NonNull ActorRef replyTo) {
        super(target, sequence);
        this.replyTo = requireNonNull(replyTo);
    }

    protected Request(final @NonNull C request, final @NonNull ABIVersion version) {
        super(request, version);
        this.replyTo = requireNonNull(request.getReplyTo());
    }

    /**
     * Return the return address where responses to this request should be directed to.
     *
     * @return Original requestor
     */
    public final @NonNull ActorRef getReplyTo() {
        return replyTo;
    }

    /**
     * Return a {@link RequestFailure} for this request, caused by a {@link RequestException}.
     *
     * @param cause Failure cause
     * @return {@link RequestFailure} corresponding to this request
     */
    public abstract @NonNull RequestFailure<T, ?> toRequestFailure(@NonNull RequestException cause);

    @Override
    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return super.addToStringAttributes(toStringHelper).add("replyTo", replyTo);
    }

    @Override
    protected abstract SerialForm<T, C> externalizableProxy(ABIVersion version);
}
