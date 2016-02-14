/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.protobuff.messages.cohort3pc.ThreePhaseCommitCohortMessages;

public class AbortTransactionReply extends VersionedExternalizableMessage {
    @Deprecated
    private static final Object SERIALIZED_INSTANCE =
            ThreePhaseCommitCohortMessages.AbortTransactionReply.newBuilder().build();

    private static final AbortTransactionReply INSTANCE = new AbortTransactionReply();

    public AbortTransactionReply() {
    }

    private AbortTransactionReply(short version) {
        super(version);
    }

    @Deprecated
    @Override
    protected Object newLegacySerializedInstance() {
        return SERIALIZED_INSTANCE;
    }

    public static AbortTransactionReply instance(short version) {
        return version == DataStoreVersions.CURRENT_VERSION ? INSTANCE : new AbortTransactionReply(version);
    }

    public static boolean isSerializedType(Object message) {
        return message instanceof AbortTransactionReply ||
                message instanceof ThreePhaseCommitCohortMessages.AbortTransactionReply;
    }
}
