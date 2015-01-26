/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import org.opendaylight.controller.protobuff.messages.cohort3pc.ThreePhaseCommitCohortMessages;

public class AbortTransactionReply implements SerializableMessage {
    public static final Class<ThreePhaseCommitCohortMessages.AbortTransactionReply> SERIALIZABLE_CLASS =
            ThreePhaseCommitCohortMessages.AbortTransactionReply.class;

    private static final Object SERIALIZED_INSTANCE =
            ThreePhaseCommitCohortMessages.AbortTransactionReply.newBuilder().build();

    public static final AbortTransactionReply INSTANCE = new AbortTransactionReply();

    @Override
    public Object toSerializable() {
        return SERIALIZED_INSTANCE;
    }
}
