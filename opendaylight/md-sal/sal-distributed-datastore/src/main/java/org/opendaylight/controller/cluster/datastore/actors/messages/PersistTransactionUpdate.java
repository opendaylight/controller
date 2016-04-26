/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors.messages;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.access.concepts.GlobalTransactionIdentifier;
import org.opendaylight.controller.cluster.access.concepts.Message;
import org.opendaylight.controller.cluster.access.concepts.RequestSuccess;

/**
 * Update message sent from
 * @author user
 *
 */
public final class PersistTransactionUpdate extends RequestSuccess<GlobalTransactionIdentifier> {
    private static final class Proxy extends AbstractSuccessProxy<GlobalTransactionIdentifier> {
        private static final long serialVersionUID = 1L;
        private Message<GlobalTransactionIdentifier, ?> message;

        public Proxy() {
            // For Externalizable
        }

        public Proxy(final GlobalTransactionIdentifier identifier, final Message<GlobalTransactionIdentifier, ?> message) {
            super(identifier);
            this.message = Preconditions.checkNotNull(message);
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            super.writeExternal(out);
            out.writeObject(message);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            super.readExternal(in);
            message = (Message<GlobalTransactionIdentifier, ?>) in.readObject();
        }

        @Override
        protected PersistTransactionUpdate readResolve() {
            return new PersistTransactionUpdate(getIdentifier(), message);
        }
    }

    private static final long serialVersionUID = 1L;
    private final Message<GlobalTransactionIdentifier, ?> message;

    public PersistTransactionUpdate(final GlobalTransactionIdentifier identifier,
            final Message<GlobalTransactionIdentifier, ?> message) {
        super(identifier);
        this.message = Preconditions.checkNotNull(message);
    }

    public Message<GlobalTransactionIdentifier, ?> getMessage() {
        return message;
    }

    @Override
    protected Proxy writeReplace() {
        return new Proxy(getIdentifier(), message);
    }
}
