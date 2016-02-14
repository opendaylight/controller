/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.protobuff.messages.cohort3pc.ThreePhaseCommitCohortMessages;

public class AbortTransaction extends AbstractThreePhaseCommitMessage {
    private static final long serialVersionUID = 1L;

    public AbortTransaction() {
    }

    public AbortTransaction(String transactionID, final short version) {
        super(transactionID, version);
    }

    @Deprecated
    @Override
    protected Object newLegacySerializedInstance() {
        return ThreePhaseCommitCohortMessages.AbortTransaction.newBuilder().
                setTransactionId(getTransactionID()).build();
    }

    public static AbortTransaction fromSerializable(Object serializable) {
        if(serializable instanceof AbortTransaction) {
            return (AbortTransaction)serializable;
        } else {
            return new AbortTransaction(((ThreePhaseCommitCohortMessages.AbortTransaction)serializable).
                    getTransactionId(), DataStoreVersions.LITHIUM_VERSION);
        }
    }

    public static boolean isSerializedType(Object message) {
        return message instanceof AbortTransaction ||
                message instanceof ThreePhaseCommitCohortMessages.AbortTransaction;
    }
}
