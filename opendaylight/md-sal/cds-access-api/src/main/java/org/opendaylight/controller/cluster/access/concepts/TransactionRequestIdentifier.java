/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import org.opendaylight.yangtools.concepts.Identifier;

public final class TransactionRequestIdentifier implements Identifier {
    private static final long serialVersionUID = 1L;
    private final GlobalTransactionIdentifier transactionId;
    private final long requestId;

    public TransactionRequestIdentifier(final GlobalTransactionIdentifier transactionId, final long requestId) {
        this.transactionId = Preconditions.checkNotNull(transactionId);
        this.requestId = requestId;
    }

    public GlobalTransactionIdentifier getTransactionId() {
        return transactionId;
    }

    public long getRequestId() {
        return requestId;
    }

    @Override
    public int hashCode() {
        return transactionId.hashCode() * 31 + Long.hashCode(requestId);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TransactionRequestIdentifier)) {
            return false;
        }

        final TransactionRequestIdentifier other = (TransactionRequestIdentifier) o;
        return requestId == other.requestId && transactionId.equals(other.transactionId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(LocalTransactionIdentifier.class).add("transaction", transactionId)
                .add("request", requestId).toString();
    }

}
