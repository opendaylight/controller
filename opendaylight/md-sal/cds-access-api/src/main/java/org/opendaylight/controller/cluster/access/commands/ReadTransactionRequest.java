/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import akka.actor.ActorRef;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.TransactionRequestIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public final class ReadTransactionRequest extends TransactionRequest {
    private static final class Proxy extends AbstractRequestProxy<TransactionRequestIdentifier> {
        private static final long serialVersionUID = 1L;
        private YangInstanceIdentifier path;

        public Proxy() {
            // For Externalizable
        }

        Proxy(final TransactionRequestIdentifier identifier, final ActorRef replyTo, final YangInstanceIdentifier path) {
            super(identifier, replyTo);
            this.path = Preconditions.checkNotNull(path);
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            super.writeExternal(out);
            out.writeObject(path);
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            super.readExternal(in);
            path = (YangInstanceIdentifier) in.readObject();
        }

        @Override
        protected ReadTransactionRequest readResolve() {
            return new ReadTransactionRequest(getIdentifier(), getReplyTo(), path);
        }
    }

    private static final long serialVersionUID = 1L;
    private final YangInstanceIdentifier path;

    ReadTransactionRequest(final TransactionRequestIdentifier identifier, final ActorRef frontendRef,
            final YangInstanceIdentifier path) {
        super(identifier, frontendRef);
        this.path = Preconditions.checkNotNull(path);
    }

    public YangInstanceIdentifier getPath() {
        return path;
    }

    @Override
    public FrontendIdentifier getFrontendIdentifier() {
        return getIdentifier().getTransactionId().getFrontendId();
    }

    @Override
    protected Proxy writeReplace() {
        return new Proxy(getIdentifier(), getReplyTo(), path);
    }
}
