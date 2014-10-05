/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import org.opendaylight.controller.protobuff.messages.cohort3pc.ThreePhaseCommitCohortMessages;

public class AbortTransaction implements SerializableMessage {
    public static final Class<ThreePhaseCommitCohortMessages.AbortTransaction> SERIALIZABLE_CLASS =
            ThreePhaseCommitCohortMessages.AbortTransaction.class;

    private final String transactionID;

    public AbortTransaction(String transactionID) {
        this.transactionID = transactionID;
    }

    public String getTransactionID() {
        return transactionID;
    }

    @Override
    public Object toSerializable() {
        return ThreePhaseCommitCohortMessages.AbortTransaction.newBuilder().
                setTransactionId(transactionID).build();
    }

    public static AbortTransaction fromSerializable(Object message) {
        return new AbortTransaction(((ThreePhaseCommitCohortMessages.AbortTransaction)message).
                getTransactionId());
    }
}
