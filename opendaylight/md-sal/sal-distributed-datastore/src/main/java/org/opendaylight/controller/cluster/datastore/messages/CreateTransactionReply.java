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

public class CreateTransactionReply implements Externalizable {
    private static final long serialVersionUID = 1L;

    private transient String transactionPath;
    private transient String transactionId;

    public CreateTransactionReply() {
    }

    public CreateTransactionReply(String transactionPath, String transactionId) {
        this.transactionPath = transactionPath;
        this.transactionId = transactionId;
    }

    public String getTransactionPath() {
        return transactionPath;
    }

    public String getTransactionId() {
        return transactionId;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        transactionId = in.readUTF();
        transactionPath = in.readUTF();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(transactionId);
        out.writeUTF(transactionPath);
    }

}
