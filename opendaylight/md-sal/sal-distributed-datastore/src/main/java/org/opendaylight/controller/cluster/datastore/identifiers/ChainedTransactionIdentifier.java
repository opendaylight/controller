/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.identifiers;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

/**
 * A TransactionIdentifier which is tied to a backend transaction chain.
 */
public class ChainedTransactionIdentifier extends TransactionIdentifier {
    private final String chainId;
    private Supplier<String> stringRepresentation;

    public ChainedTransactionIdentifier(final TransactionChainIdentifier chainId, final long txnCounter) {
        super(chainId.getMemberName(), txnCounter);
        Preconditions.checkNotNull(chainId);
        this.chainId = chainId.toString();
        stringRepresentation = Suppliers.memoize(new Supplier<String>() {
            @Override
            public String get() {
                return new StringBuilder(chainId.toString().length() + TX_SEPARATOR.length() + 21).
                        append(chainId).append(TX_SEPARATOR).append(getCounter()).append('-').
                        append(getTimestamp()).toString();
            }
        });
    }


    @Override
    public String getChainId() {
        return chainId;
    }

    @Override
    public String toString() {
        return stringRepresentation.get();
    }

}
