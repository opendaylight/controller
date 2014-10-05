/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import org.opendaylight.controller.protobuff.messages.cohort3pc.ThreePhaseCommitCohortMessages;

public class CanCommitTransaction implements SerializableMessage {
    public static final Class<?> SERIALIZABLE_CLASS = ThreePhaseCommitCohortMessages.CanCommitTransaction.class;

    private final String transactionID;

    public CanCommitTransaction(String transactionID) {
        this.transactionID = transactionID;
    }

    public String getTransactionID() {
        return transactionID;
    }

    @Override
    public Object toSerializable() {
        return ThreePhaseCommitCohortMessages.CanCommitTransaction.newBuilder().
                setTransactionId(transactionID).build();
    }

    public static CanCommitTransaction fromSerializable(Object message) {
        return new CanCommitTransaction(((ThreePhaseCommitCohortMessages.CanCommitTransaction)message).
                getTransactionId());
    }
}
