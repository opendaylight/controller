/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import org.opendaylight.controller.protobuff.messages.cohort3pc.ThreePhaseCommitCohortMessages;

public class CanCommitTransactionReply implements SerializableMessage {
    public static final Class<ThreePhaseCommitCohortMessages.CanCommitTransactionReply> SERIALIZABLE_CLASS =
            ThreePhaseCommitCohortMessages.CanCommitTransactionReply.class;

    public static final CanCommitTransactionReply YES = new CanCommitTransactionReply(true);
    public static final CanCommitTransactionReply NO = new CanCommitTransactionReply(false);

    private static final ThreePhaseCommitCohortMessages.CanCommitTransactionReply YES_SERIALIZED =
            ThreePhaseCommitCohortMessages.CanCommitTransactionReply.newBuilder().setCanCommit(true).build();

    private static final ThreePhaseCommitCohortMessages.CanCommitTransactionReply NO_SERIALIZED =
            ThreePhaseCommitCohortMessages.CanCommitTransactionReply.newBuilder().setCanCommit(false).build();

    private final boolean canCommit;

    private CanCommitTransactionReply(final boolean canCommit) {
        this.canCommit = canCommit;
    }

    public boolean getCanCommit() {
        return canCommit;
    }

    @Override
    public Object toSerializable() {
        return canCommit ? YES_SERIALIZED : NO_SERIALIZED;
    }

    public static CanCommitTransactionReply fromSerializable(final Object message) {
        ThreePhaseCommitCohortMessages.CanCommitTransactionReply serialized =
                (ThreePhaseCommitCohortMessages.CanCommitTransactionReply) message;
        return serialized.getCanCommit() ? YES : NO;
    }
}
