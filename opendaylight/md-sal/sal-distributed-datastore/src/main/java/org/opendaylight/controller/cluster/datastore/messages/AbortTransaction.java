/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

@Deprecated(since = "9.0.0", forRemoval = true)
public final class AbortTransaction extends AbstractThreePhaseCommitMessage {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public AbortTransaction() {
    }

    public AbortTransaction(final TransactionIdentifier transactionID, final short version) {
        super(transactionID, version);
    }

    public static AbortTransaction fromSerializable(final Object serializable) {
        Preconditions.checkArgument(serializable instanceof AbortTransaction);
        return (AbortTransaction)serializable;
    }

    public static boolean isSerializedType(final Object message) {
        return message instanceof AbortTransaction;
    }
}
