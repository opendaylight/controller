/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages;

public class CloseTransaction extends VersionedExternalizableMessage {
    private static final long serialVersionUID = 1L;

    @Deprecated
    private static final Object SERIALIZED_INSTANCE =
            ShardTransactionMessages.CloseTransaction.newBuilder().build();

    public CloseTransaction() {
    }

    public CloseTransaction(short version) {
        super(version);
    }

    @Deprecated
    @Override
    protected Object newLegacySerializedInstance() {
        return SERIALIZED_INSTANCE;
    }

    public static boolean isSerializedType(Object message) {
        return message instanceof CloseTransaction || message instanceof ShardTransactionMessages.CloseTransaction;
    }
}
