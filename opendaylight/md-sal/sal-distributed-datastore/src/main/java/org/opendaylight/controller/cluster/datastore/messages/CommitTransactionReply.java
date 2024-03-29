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
public final class CommitTransactionReply extends VersionedExternalizableMessage {
    @java.io.Serial
    private static final long serialVersionUID = -8342450250867395000L;

    public static final CommitTransactionReply INSTANCE = new CommitTransactionReply();

    public CommitTransactionReply() {
    }

    private CommitTransactionReply(final short version) {
        super(version);
    }

    public static CommitTransactionReply instance(final short version) {
        return version == DataStoreVersions.CURRENT_VERSION ? INSTANCE : new CommitTransactionReply(version);
    }

    public static boolean isSerializedType(final Object message) {
        return message instanceof CommitTransactionReply;
    }
}
