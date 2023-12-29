/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import org.opendaylight.controller.cluster.datastore.DataStoreVersions;

@Deprecated(since = "9.0.0", forRemoval = true)
public final class AbortTransactionReply extends VersionedExternalizableMessage {
    @java.io.Serial
    private static final long serialVersionUID = 7251132353204199793L;
    private static final AbortTransactionReply INSTANCE = new AbortTransactionReply();

    public AbortTransactionReply() {
    }

    private AbortTransactionReply(final short version) {
        super(version);
    }

    public static AbortTransactionReply instance(final short version) {
        return version == DataStoreVersions.CURRENT_VERSION ? INSTANCE : new AbortTransactionReply(version);
    }

    public static boolean isSerializedType(final Object message) {
        return message instanceof AbortTransactionReply;
    }
}
