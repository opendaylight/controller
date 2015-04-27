/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.identifiers;

import com.google.common.base.Preconditions;

/**
 * A TransactionIdentifier which is tied to a backend transaction chain.
 */
public class ChainedTransactionIdentifier extends TransactionIdentifier {
    private final String chainId;

    public ChainedTransactionIdentifier(final String memberName, final long counter, final String chainId) {
        super(memberName, counter);
        this.chainId = Preconditions.checkNotNull(chainId);
    }

    @Override
    public String getChainId() {
        return chainId;
    }
}
