/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.registry.candidate.command;

import static java.util.Objects.requireNonNull;

import akka.cluster.ddata.ORMap;
import akka.cluster.ddata.ORSet;
import akka.cluster.ddata.typed.javadsl.Replicator.UpdateResponse;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;

public final class InternalUpdateResponse extends CandidateRegistryCommand {
    private final @NonNull UpdateResponse<ORMap<DOMEntity, ORSet<String>>> rsp;

    public InternalUpdateResponse(final UpdateResponse<ORMap<DOMEntity, ORSet<String>>> rsp) {
        this.rsp = requireNonNull(rsp);
    }

    public @NonNull UpdateResponse<ORMap<DOMEntity, ORSet<String>>> getRsp() {
        return rsp;
    }
}
