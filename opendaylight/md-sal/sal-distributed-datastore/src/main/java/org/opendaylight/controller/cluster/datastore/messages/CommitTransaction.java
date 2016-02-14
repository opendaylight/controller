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

public class CommitTransaction extends AbstractThreePhaseCommitMessage {
    private static final long serialVersionUID = 1L;

    public CommitTransaction() {
    }

    public CommitTransaction(String transactionID, final short version) {
        super(transactionID, version);
    }

    @Deprecated
    @Override
    protected Object newLegacySerializedInstance() {
        return ThreePhaseCommitCohortMessages.CommitTransaction.newBuilder().setTransactionId(
                getTransactionID()).build();
    }

    public static CommitTransaction fromSerializable(Object serializable) {
        if(serializable instanceof CommitTransaction) {
            return (CommitTransaction)serializable;
        } else {
            return new CommitTransaction(((ThreePhaseCommitCohortMessages.CommitTransaction)serializable).
                    getTransactionId(), DataStoreVersions.LITHIUM_VERSION);
        }
    }

    public static boolean isSerializedType(Object message) {
        return message instanceof CommitTransaction ||
                message instanceof ThreePhaseCommitCohortMessages.CommitTransaction;
    }
}
