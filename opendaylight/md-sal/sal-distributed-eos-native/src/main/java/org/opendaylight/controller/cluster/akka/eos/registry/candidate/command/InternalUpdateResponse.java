/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.akka.eos.registry.candidate.command;

import akka.cluster.ddata.ORMap;
import akka.cluster.ddata.ORSet;
import akka.cluster.ddata.typed.javadsl.Replicator;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;

public class InternalUpdateResponse implements CandidateRegistryCommand {

    final Replicator.UpdateResponse<ORMap<DOMEntity, ORSet<String>>> rsp;

    public InternalUpdateResponse(final Replicator.UpdateResponse<ORMap<DOMEntity, ORSet<String>>> rsp) {
        this.rsp = rsp;
    }

    public Replicator.UpdateResponse<ORMap<DOMEntity, ORSet<String>>> getRsp() {
        return rsp;
    }
}
