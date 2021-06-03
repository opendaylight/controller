/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.owner.supervisor.command;

import akka.cluster.ddata.ORMap;
import akka.cluster.ddata.ORSet;
import akka.cluster.ddata.typed.javadsl.Replicator.GetResponse;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;

public final class InitialCandidateSync extends OwnerSupervisorCommand {
    private final @Nullable GetResponse<ORMap<DOMEntity, ORSet<String>>> response;

    public InitialCandidateSync(final GetResponse<ORMap<DOMEntity, ORSet<String>>> response) {
        this.response = response;
    }

    public @Nullable GetResponse<ORMap<DOMEntity, ORSet<String>>> getResponse() {
        return response;
    }
}
