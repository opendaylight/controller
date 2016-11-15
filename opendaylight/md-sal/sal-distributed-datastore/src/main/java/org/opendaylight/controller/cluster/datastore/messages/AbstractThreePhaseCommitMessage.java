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

    private TransactionIdentifier transactionId;

    protected AbstractThreePhaseCommitMessage() {
    }

    protected AbstractThreePhaseCommitMessage(final TransactionIdentifier transactionId, final short version) {
        super(version);
        this.transactionId = Preconditions.checkNotNull(transactionId);
    }

    public TransactionIdentifier getTransactionId() {
        return transactionId;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        transactionId = TransactionIdentifier.readFrom(in);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        transactionId.writeTo(out);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [transactionId=" + transactionId + ", version=" + getVersion() + "]";
    }
}
