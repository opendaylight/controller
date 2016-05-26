/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import akka.actor.ActorRef;
import com.google.common.annotations.Beta;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.concepts.Identifier;

/**
 * Abstract Externalizable proxy for use with {@link Request} subclasses.
 *
 * @author Robert Varga
 *
 * @param <T> Target identifier type
 */
@Beta
public abstract class AbstractRequestProxy<T extends Identifier & WritableObject, C extends Request<T, C>>
        extends AbstractMessageProxy<T, C> {
    private static final long serialVersionUID = 1L;
    private ActorRef replyTo;

    public AbstractRequestProxy() {
        // For Externalizable
    }

    protected AbstractRequestProxy(final @Nonnull C request) {
        super(request);
        this.replyTo = request.getReplyTo();
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        super.writeExternal(out);
        // TODO: http://doc.akka.io/docs/akka/current/java/serialization.html#Serializing_ActorRefs seems to be
        //       a more efficient and complete way of doing serialization.
        out.writeObject(replyTo);
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        replyTo = (ActorRef) in.readObject();
    }

    @Override
    protected final C createMessage(final T target, final long sequence) {
        return createRequest(target, sequence, replyTo);
    }

    protected abstract @Nonnull C createRequest(@Nonnull T target, long sequence, @Nonnull ActorRef replyTo);
}