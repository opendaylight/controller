/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionChainMessages;

public class CloseTransactionChain extends VersionedExternalizableMessage {
    private static final long serialVersionUID = 1L;

    private String transactionChainId;

    public CloseTransactionChain() {
    }

    public CloseTransactionChain(final String transactionChainId, final short version) {
        super(version);
        this.transactionChainId = transactionChainId;
    }

    public String getTransactionChainId() {
        return transactionChainId;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        transactionChainId = in.readUTF();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeUTF(transactionChainId);
    }

    @Deprecated
    @Override
    protected Object newLegacySerializedInstance() {
        return ShardTransactionChainMessages.CloseTransactionChain.newBuilder().setTransactionChainId(transactionChainId)
                .build();
    }

    public static CloseTransactionChain fromSerializable(final Object serializable){
        if(serializable instanceof CloseTransactionChain) {
            return (CloseTransactionChain)serializable;
        } else {
            ShardTransactionChainMessages.CloseTransactionChain closeTransactionChain =
                    (ShardTransactionChainMessages.CloseTransactionChain) serializable;
            return new CloseTransactionChain(closeTransactionChain.getTransactionChainId(),
                    DataStoreVersions.LITHIUM_VERSION);
        }
    }

    public static boolean isSerializedType(Object message) {
        return message instanceof CloseTransactionChain ||
                message instanceof ShardTransactionChainMessages.CloseTransactionChain;
    }
}
