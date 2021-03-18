/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.akka.eos.registry.listener.type.command;

import akka.cluster.ddata.ORMap;
import akka.cluster.ddata.ORSet;
import akka.cluster.ddata.typed.javadsl.Replicator;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;

/**
 * Adapted notification from distributed-data sent to EntityTypeListenerActor when candidates change.
 */
public class CandidatesChanged implements TypeListenerCommand {
    private final Replicator.SubscribeResponse<ORMap<DOMEntity, ORSet<String>>> subscribeResponse;

    public CandidatesChanged(final Replicator.SubscribeResponse<ORMap<DOMEntity, ORSet<String>>> subscribeResponse) {

        this.subscribeResponse = subscribeResponse;
    }

    public Replicator.SubscribeResponse<ORMap<DOMEntity, ORSet<String>>> getResponse() {
        return subscribeResponse;
    }

    @Override
    public String toString() {
        return "CandidatesChanged{"
                + "subscribeResponse=" + subscribeResponse
                + '}';
    }
}
