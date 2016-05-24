/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
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
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

/**
 * Base class for a 3PC message.
 *
 * @author Thomas Pantelis
 */
public abstract class AbstractThreePhaseCommitMessage extends VersionedExternalizableMessage {
    private static final long serialVersionUID = 1L;

    private TransactionIdentifier transactionID;

    protected AbstractThreePhaseCommitMessage() {
    }

    protected AbstractThreePhaseCommitMessage(final TransactionIdentifier transactionID, final short version) {
        super(version);
        this.transactionID = Preconditions.checkNotNull(transactionID);
    }

    public TransactionIdentifier getTransactionID() {
        return transactionID;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        transactionID = (TransactionIdentifier) in.readObject();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeObject(transactionID);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [transactionID=" + transactionID + ", version=" + getVersion() + "]";
    }
}
