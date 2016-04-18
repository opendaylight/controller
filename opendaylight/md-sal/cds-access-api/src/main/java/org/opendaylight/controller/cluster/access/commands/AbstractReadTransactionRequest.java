/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import akka.actor.ActorRef;
import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.access.concepts.TransactionRequestIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

@Beta
public abstract class AbstractReadTransactionRequest extends TransactionRequest {
    abstract static class AbstractReadRequestProxy extends AbstractRequestProxy<TransactionRequestIdentifier> {
        private static final long serialVersionUID = 1L;
        private YangInstanceIdentifier path;

        public AbstractReadRequestProxy() {
            // For Externalizable
        }

        AbstractReadRequestProxy(final TransactionRequestIdentifier identifier, final ActorRef replyTo, final YangInstanceIdentifier path) {
            super(identifier, replyTo);
            this.path = Preconditions.checkNotNull(path);
        }

        final YangInstanceIdentifier getPath() {
            return path;
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
        protected abstract AbstractReadTransactionRequest readResolve();
    }

    private static final long serialVersionUID = 1L;
    private final YangInstanceIdentifier path;

    AbstractReadTransactionRequest(final TransactionRequestIdentifier identifier, final ActorRef frontendRef,
            final YangInstanceIdentifier path) {
        super(identifier, frontendRef);
        this.path = Preconditions.checkNotNull(path);
    }

    public final YangInstanceIdentifier getPath() {
        return path;
    }

    @Override
    protected abstract AbstractReadRequestProxy writeReplace();
}
