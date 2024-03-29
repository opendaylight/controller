/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.yangtools.concepts.Identifiable;

@Deprecated(since = "9.0.0", forRemoval = true)
public final class CloseTransactionChain extends VersionedExternalizableMessage
        implements Identifiable<LocalHistoryIdentifier> {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private LocalHistoryIdentifier transactionChainId;

    public CloseTransactionChain() {
    }

    public CloseTransactionChain(final LocalHistoryIdentifier transactionChainId, final short version) {
        super(version);
        this.transactionChainId = requireNonNull(transactionChainId);
    }

    @Override
    public LocalHistoryIdentifier getIdentifier() {
        return transactionChainId;
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        transactionChainId = LocalHistoryIdentifier.readFrom(in);
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        super.writeExternal(out);
        transactionChainId.writeTo(out);
    }

    public static CloseTransactionChain fromSerializable(final Object serializable) {
        checkArgument(serializable instanceof CloseTransactionChain);
        return (CloseTransactionChain)serializable;
    }

    public static boolean isSerializedType(final Object message) {
        return message instanceof CloseTransactionChain;
    }
}
