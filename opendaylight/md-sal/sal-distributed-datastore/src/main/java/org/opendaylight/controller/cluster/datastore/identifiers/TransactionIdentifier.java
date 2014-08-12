/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.identifiers;

import com.google.common.base.Preconditions;

public class TransactionIdentifier {
    private final String memberName;
    private final long counter;


    public TransactionIdentifier(String memberName, long counter) {
        this.memberName = Preconditions.checkNotNull(memberName, "memberName should not be null");
        this.counter = counter;
    }

    public static Builder builder(){
        return new Builder();
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

    @Override public String toString() {
        final StringBuilder sb =
            new StringBuilder();
        sb.append(memberName).append("-txn-").append(counter);
        return sb.toString();
    }

    public static class Builder {
        private String memberName;
        private long counter;

        public TransactionIdentifier build(){
            return new TransactionIdentifier(memberName, counter);
        }

        public Builder memberName(String memberName){
            this.memberName = memberName;
            return this;
        }

        public Builder counter(long counter){
            this.counter = counter;
            return this;
        }
    }
}
