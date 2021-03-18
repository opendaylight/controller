/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.owner.supervisor.command;

import static java.util.Objects.requireNonNull;

import akka.cluster.ddata.LWWRegister;
import akka.cluster.ddata.typed.javadsl.Replicator.GetResponse;
import org.eclipse.jdt.annotation.NonNull;

public final class InitialOwnerSync extends OwnerSupervisorCommand {
    private final @NonNull GetResponse<LWWRegister<String>> response;

    public InitialOwnerSync(final GetResponse<LWWRegister<String>> response) {
        this.response = requireNonNull(response);
    }

    public @NonNull GetResponse<LWWRegister<String>> getResponse() {
        return response;
    }
}
