/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.registry.listener.type.command;

import static java.util.Objects.requireNonNull;

import akka.cluster.ddata.ORMap;
import akka.cluster.ddata.ORSet;
import akka.cluster.ddata.typed.javadsl.Replicator.SubscribeResponse;
import com.google.common.base.MoreObjects;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;

/**
 * Adapted notification from distributed-data sent to EntityTypeListenerActor when candidates change.
 */
public final class CandidatesChanged extends TypeListenerCommand {
    private final @NonNull SubscribeResponse<ORMap<DOMEntity, ORSet<String>>> response;

    public CandidatesChanged(final SubscribeResponse<ORMap<DOMEntity, ORSet<String>>> response) {
        this.response = requireNonNull(response);
    }

    public @NonNull SubscribeResponse<ORMap<DOMEntity, ORSet<String>>> getResponse() {
        return response;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("response", response).toString();
    }
}
