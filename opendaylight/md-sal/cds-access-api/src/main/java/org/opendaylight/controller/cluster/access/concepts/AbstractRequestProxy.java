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
import org.opendaylight.yangtools.concepts.Identifier;

/**
 * Abstract Externalizable proxy for use with {@link Request} subclasses.
 *
 * @author Robert Varga
 *
 * @param <T> Target identifier type
 */
@Beta
public abstract class AbstractRequestProxy<T extends Identifier & WritableObject> extends AbstractMessageProxy<T, Request<T>> {
    private static final long serialVersionUID = 1L;
    private ActorRef replyTo;

    public AbstractRequestProxy() {
        // For Externalizable
    }

    protected AbstractRequestProxy(final Request<T> request) {
        super(request);
        this.replyTo = request.getReplyTo();
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeObject(replyTo);
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        replyTo = (ActorRef) in.readObject();
    }

    @Override
    protected final Message<T, Request<T>> createMessage(final T target, final long sequence) {
        return createMessage(target, sequence, replyTo);
    }

    protected abstract Message<T, Request<T>> createMessage(T target, long sequence, ActorRef replyTo);
}