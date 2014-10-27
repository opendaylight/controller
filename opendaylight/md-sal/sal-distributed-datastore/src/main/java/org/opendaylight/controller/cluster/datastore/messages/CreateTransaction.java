/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;


import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import com.google.common.base.Preconditions;

public class CreateTransaction implements Externalizable {
    private static final long serialVersionUID = 1L;

    private transient String transactionId;
    private transient int transactionType;
    private transient String transactionChainId;

    public CreateTransaction() {
    }

    public CreateTransaction(String transactionId, int transactionType) {
        this(transactionId, transactionType, "");
    }

    public CreateTransaction(String transactionId, int transactionType, String transactionChainId) {
        this.transactionId = Preconditions.checkNotNull(transactionId);
        this.transactionType = Preconditions.checkNotNull(transactionType);
        this.transactionChainId = Preconditions.checkNotNull(transactionChainId);
    }

    public String getTransactionId() {
        return transactionId;
    }

    public int getTransactionType() {
        return transactionType;
    }

    public String getTransactionChainId() {
        return transactionChainId;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        transactionId = in.readUTF();
        transactionType = in.readInt();
        transactionChainId = in.readUTF();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(transactionId);
        out.writeInt(transactionType);
        out.writeUTF(transactionChainId);
    }
}
