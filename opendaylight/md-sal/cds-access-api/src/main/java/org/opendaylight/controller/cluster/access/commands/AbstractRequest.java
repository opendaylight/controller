/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import akka.actor.ActorRef;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.yangtools.concepts.Identifier;

abstract class AbstractRequest<T extends Identifier> extends AbstractMessage<T, Request<T>> implements Request<T> {
    static abstract class AbstractRequestProxy<T extends Identifier> extends AbstractProxy<T, Request<T>> {
        private static final long serialVersionUID = 1L;
        private ActorRef frontendRef;

        public AbstractRequestProxy() {
            // For Externalizable
        }

        AbstractRequestProxy(final T identifier, final ActorRef frontendRef) {
            super(identifier);
            this.frontendRef = Preconditions.checkNotNull(frontendRef);
        }

        final ActorRef getFrontendRef() {
            return frontendRef;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            super.writeExternal(out);
            out.writeObject(frontendRef);
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            super.readExternal(in);
            frontendRef = (ActorRef) in.readObject();
        }

        @Override
        abstract AbstractRequest<?> readResolve();
    }

    private static final long serialVersionUID = 1L;
    private final ActorRef frontendRef;

    AbstractRequest(final T identifier, final ActorRef frontendRef) {
        super(identifier);
        this.frontendRef = Preconditions.checkNotNull(frontendRef);
    }

    @Override
    public final ActorRef getFrontendRef() {
        return frontendRef;
    }

    @Override
    ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return super.addToStringAttributes(toStringHelper).add("frontendRef", frontendRef);
    }
}
