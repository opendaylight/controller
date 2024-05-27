/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.pekko.owner.supervisor.command;

import static java.util.Objects.requireNonNull;

import org.apache.pekko.cluster.ddata.LWWRegister;
import org.apache.pekko.cluster.ddata.typed.javadsl.Replicator.UpdateResponse;
import org.eclipse.jdt.annotation.NonNull;

public final class OwnerChanged extends OwnerSupervisorCommand {
    private final @NonNull UpdateResponse<LWWRegister<String>> rsp;

    public OwnerChanged(final UpdateResponse<LWWRegister<String>> rsp) {
        this.rsp = requireNonNull(rsp);
    }

    public @NonNull UpdateResponse<LWWRegister<String>> getResponse() {
        return rsp;
    }
}
