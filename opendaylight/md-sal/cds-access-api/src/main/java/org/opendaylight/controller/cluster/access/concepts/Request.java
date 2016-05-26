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
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.yangtools.concepts.Identifier;

@Beta
public abstract class Request<T extends Identifier> extends Message<T, Request<T>> {
    protected static abstract class AbstractRequestProxy<T extends Identifier> extends AbstractProxy<T, Request<T>> {
        private static final long serialVersionUID = 1L;
        private ActorRef replyTo;

        public AbstractRequestProxy() {
            // For Externalizable
        }

        protected AbstractRequestProxy(final T identifier, final ActorRef replyTo) {
            super(identifier);
            this.replyTo = Preconditions.checkNotNull(replyTo);
        }

        protected final ActorRef getReplyTo() {
            return replyTo;
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
        protected abstract Request<T> readResolve();
    }

    private static final long serialVersionUID = 1L;
    private final ActorRef replyTo;

    protected Request(final T identifier, final ActorRef replyTo) {
        super(identifier);
        this.replyTo = Preconditions.checkNotNull(replyTo);
    }

    public final ActorRef getReplyTo() {
        return replyTo;
    }

    @Override
    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return super.addToStringAttributes(toStringHelper).add("replyTo", replyTo);
    }

    @Override
    protected abstract AbstractRequestProxy<T> writeReplace();
}
