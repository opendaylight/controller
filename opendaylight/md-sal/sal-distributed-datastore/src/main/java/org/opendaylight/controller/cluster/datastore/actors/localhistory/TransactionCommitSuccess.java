/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors.localhistory;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.access.concepts.GlobalTransactionIdentifier;
import org.opendaylight.controller.cluster.access.concepts.RequestSuccess;

public final class TransactionCommitSuccess extends RequestSuccess<GlobalTransactionIdentifier> {
    private static final class Proxy extends AbstractSuccessProxy<GlobalTransactionIdentifier> {
        private static final long serialVersionUID = 1L;
        private long commitedTransactionId;

        public Proxy(final GlobalTransactionIdentifier identifier, final long committedTransactionId) {
            super(identifier);
            this.commitedTransactionId = committedTransactionId;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            super.writeExternal(out);
            out.writeLong(commitedTransactionId);
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            super.readExternal(in);
            commitedTransactionId = in.readLong();
        }

        @Override
        protected TransactionCommitSuccess readResolve() {
            return new TransactionCommitSuccess(getIdentifier(), commitedTransactionId);
        }
    }

    private static final long serialVersionUID = 1L;
    private final long committedTransactionId;

    public TransactionCommitSuccess(final GlobalTransactionIdentifier identifier, final long committedTransactionId) {
        super(identifier);
        this.committedTransactionId = committedTransactionId;
    }

    public long getCommittedTransactionId() {
        return committedTransactionId;
    }

    @Override
    protected Proxy writeReplace() {
        return new Proxy(getIdentifier(), committedTransactionId);
    }
}
