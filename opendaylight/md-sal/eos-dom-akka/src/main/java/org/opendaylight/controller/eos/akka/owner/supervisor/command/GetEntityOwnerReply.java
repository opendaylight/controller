/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.owner.supervisor.command;

import java.io.Serializable;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.GetEntityOwnerOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.GetEntityOwnerOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.NodeName;

public final class GetEntityOwnerReply extends OwnerSupervisorReply implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String owner;

    public GetEntityOwnerReply(final @Nullable String owner) {
        this.owner = owner;
    }

    public @NonNull GetEntityOwnerOutput toOutput() {
        final GetEntityOwnerOutputBuilder builder = new GetEntityOwnerOutputBuilder();
        if (owner != null) {
            builder.setOwnerNode(new NodeName(owner));
        }
        return builder.build();
    }
}
