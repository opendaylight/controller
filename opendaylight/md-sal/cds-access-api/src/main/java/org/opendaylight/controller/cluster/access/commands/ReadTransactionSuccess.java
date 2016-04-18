/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.access.concepts.RequestSuccess;
import org.opendaylight.controller.cluster.access.concepts.TransactionRequestIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

@Beta
public final class ReadTransactionSuccess extends TransactionSuccess {
    private static final class Proxy extends AbstractSuccessProxy<TransactionRequestIdentifier> {
        private Optional<NormalizedNode<?, ?>> data;

        public Proxy() {
            // For Externalizable
        }

        Proxy(final TransactionRequestIdentifier identifier, final Optional<NormalizedNode<?, ?>> data) {
            super(identifier);
            this.data = Preconditions.checkNotNull(data);
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            super.writeExternal(out);
            out.writeObject(data);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            super.readExternal(in);
            data = (Optional<NormalizedNode<?, ?>>) in.readObject();
        }

        @Override
        protected RequestSuccess<TransactionRequestIdentifier> readResolve() {
            return new ReadTransactionSuccess(getIdentifier(), data);
        }
    }

    private static final long serialVersionUID = 1L;
    private final Optional<NormalizedNode<?, ?>> data;

    public ReadTransactionSuccess(final TransactionRequestIdentifier identifier, final Optional<NormalizedNode<?, ?>> data) {
        super(identifier);
        this.data = Preconditions.checkNotNull(data);
    }

    public Optional<NormalizedNode<?, ?>> getData() {
        return data;
    }

    @Override
    protected Proxy writeReplace() {
        // TODO Auto-generated method stub
        return new Proxy(getIdentifier(), data);
    }
}
