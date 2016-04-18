/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cds.access;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.yangtools.concepts.Identifier;

public final class GlobalTransactionIdentifier implements Identifier {

    private static final class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;
        private FrontendIdentifier frontendId;
        private LocalTransactionIdentifier transactionId;

        public Proxy() {
            // For Externalizable
        }

        Proxy(final FrontendIdentifier frontendId, final LocalTransactionIdentifier transactionId) {
            this.frontendId = Preconditions.checkNotNull(frontendId);
            this.transactionId = Preconditions.checkNotNull(transactionId);
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            // FIXME: inline these to save space
            out.writeObject(frontendId);
            out.writeObject(transactionId);
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            frontendId = (FrontendIdentifier) in.readObject();
            transactionId = (LocalTransactionIdentifier) in.readObject();
        }

        @SuppressWarnings("unused")
        private GlobalTransactionIdentifier readResolve() {
            return new GlobalTransactionIdentifier(frontendId, transactionId);
        }
    }

    private static final long serialVersionUID = 1L;
    private final FrontendIdentifier frontendId;
    private final LocalTransactionIdentifier transactionId;

    public GlobalTransactionIdentifier(final FrontendIdentifier frontendId,
            final LocalTransactionIdentifier transactionId) {
        this.frontendId = Preconditions.checkNotNull(frontendId);
        this.transactionId = Preconditions.checkNotNull(transactionId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(LocalTransactionIdentifier.class).add("frontend", frontendId)
                .add("transaction", transactionId).toString();
    }

    @SuppressWarnings("unused")
    private Proxy writeReplace() {
        return new Proxy(frontendId, transactionId);
    }
}
