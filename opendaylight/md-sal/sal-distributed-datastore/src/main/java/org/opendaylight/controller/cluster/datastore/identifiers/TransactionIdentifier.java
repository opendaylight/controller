/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.identifiers;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class TransactionIdentifier {
    private static final String TX_SEPARATOR = "-txn-";

    private final String memberName;
    private final long counter;
    private String stringRepresentation;

    public TransactionIdentifier(String memberName, long counter) {
        this.memberName = Preconditions.checkNotNull(memberName, "memberName should not be null");
        this.counter = counter;
    }

    public String getChainId() {
        return "";
    }

    public static TransactionIdentifier create(String memberName, long counter, String chainId) {
        if (Strings.isNullOrEmpty(chainId)) {
            return new TransactionIdentifier(memberName, counter);
        } else {
            return new ChainedTransactionIdentifier(memberName, counter, chainId);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TransactionIdentifier that = (TransactionIdentifier) o;

        if (counter != that.counter) {
            return false;
        }
        if (!memberName.equals(that.memberName)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = memberName.hashCode();
        result = 31 * result + (int) (counter ^ (counter >>> 32));
        return result;
    }

    @Override
    public String toString() {
        if(stringRepresentation == null) {
            stringRepresentation = new StringBuilder(memberName.length() + TX_SEPARATOR.length() + 10).
                append(memberName).append(TX_SEPARATOR).append(counter).toString();
        }

        return stringRepresentation;
    }
}
