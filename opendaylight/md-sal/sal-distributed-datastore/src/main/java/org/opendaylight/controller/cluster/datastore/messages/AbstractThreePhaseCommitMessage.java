/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import static java.util.Objects.requireNonNull;

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
        this.transactionId = requireNonNull(transactionId);
    }

    public TransactionIdentifier getTransactionId() {
        return transactionId;
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        transactionId = TransactionIdentifier.readFrom(in);
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        super.writeExternal(out);
        transactionId.writeTo(out);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [transactionId=" + transactionId + ", version=" + getVersion() + "]";
    }
}
