/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import org.eclipse.jdt.annotation.NonNull;

/**
 * Support class that handles persistence recovery for a RaftActor.
 *
 * @author Thomas Pantelis
 */
class RaftActorRecoverySupport {
    private final @NonNull RaftActorContext context;
    private final @NonNull RaftActorRecoveryCohort cohort;
    private final @NonNull RaftActor<?> actor;

    RaftActorRecoverySupport(final @NonNull RaftActor<?> actor, final RaftActorContext context,
            final RaftActorRecoveryCohort cohort) {
        this.actor = requireNonNull(actor);
        this.context = requireNonNull(context);
        this.cohort = requireNonNull(cohort);
    }

    @NonNull RaftActorRecovery recoverToPersistent() throws IOException {
        return new RaftActorRecovery(actor, context, cohort);
    }

    @NonNull RaftActorRecovery recoverToTransient() throws IOException {
        return new RaftActorRecovery.ToTransient(actor, context, cohort);
    }
}
