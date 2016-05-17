/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;

public class CloseTransactionChain extends VersionedExternalizableMessage {
    private static final long serialVersionUID = 1L;

    private LocalHistoryIdentifier<?> transactionChainId;

    public CloseTransactionChain() {
    }

    public CloseTransactionChain(final LocalHistoryIdentifier<?> transactionChainId, final short version) {
        super(version);
        this.transactionChainId = Preconditions.checkNotNull(transactionChainId);
    }

    public LocalHistoryIdentifier<?> getTransactionChainId() {
        return transactionChainId;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        transactionChainId = (LocalHistoryIdentifier<?>) in.readObject();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeObject(transactionChainId);
    }

    public static CloseTransactionChain fromSerializable(final Object serializable){
        Preconditions.checkArgument(serializable instanceof CloseTransactionChain);
        return (CloseTransactionChain)serializable;
    }

    public static boolean isSerializedType(Object message) {
        return message instanceof CloseTransactionChain;
    }
}
