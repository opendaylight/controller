/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import com.google.common.annotations.Beta;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.access.concepts.RequestSuccess;
import org.opendaylight.controller.cluster.access.concepts.TransactionRequestIdentifier;

@Beta
public final class ExistsTransactionSuccess extends TransactionSuccess {
    private static final class Proxy extends AbstractSuccessProxy<TransactionRequestIdentifier> {
        private boolean exists;

        public Proxy() {
            // For Externalizable
        }

        Proxy(final TransactionRequestIdentifier identifier, final boolean exists) {
            super(identifier);
            this.exists = exists;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            super.writeExternal(out);
            out.writeBoolean(exists);
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            super.readExternal(in);
            exists = in.readBoolean();
        }

        @Override
        protected RequestSuccess<TransactionRequestIdentifier> readResolve() {
            return new ExistsTransactionSuccess(getIdentifier(), exists);
        }
    }

    private static final long serialVersionUID = 1L;
    private final boolean exists;

    public ExistsTransactionSuccess(final TransactionRequestIdentifier identifier, final boolean exists) {
        super(identifier);
        this.exists = exists;
    }

    public boolean getExists() {
        return exists;
    }

    @Override
    protected Proxy writeReplace() {
        return new Proxy(getIdentifier(), exists);
    }
}
