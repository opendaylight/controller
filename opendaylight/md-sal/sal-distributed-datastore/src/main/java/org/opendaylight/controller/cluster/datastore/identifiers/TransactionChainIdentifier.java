/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.identifiers;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import java.util.concurrent.atomic.AtomicLong;
import org.opendaylight.controller.cluster.access.concepts.MemberName;

public class TransactionChainIdentifier {

    protected static final String CHAIN_SEPARATOR = "-chn-";

    private final AtomicLong txnCounter = new AtomicLong();
    private final Supplier<String> stringRepresentation;
    private final MemberName memberName;

    public TransactionChainIdentifier(final MemberName memberName, final long counter) {
        this.memberName = memberName;
        stringRepresentation = Suppliers.memoize(() -> {
            final StringBuilder sb = new StringBuilder();
            sb.append(memberName.getName()).append(CHAIN_SEPARATOR);
            sb.append(counter);
            return sb.toString();
        });
    }
    @Override
    public String toString() {
        return stringRepresentation.get();
    }

    public TransactionIdentifier newTransactionIdentifier(){
        return new ChainedTransactionIdentifier(this, txnCounter.incrementAndGet());
    }

    public MemberName getMemberName() {
        return memberName;
    }
}
