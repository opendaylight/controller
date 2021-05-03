/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.owner.supervisor.command;

import akka.actor.typed.ActorRef;
import java.io.Serializable;
import org.eclipse.jdt.annotation.Nullable;

public final class ActivateDataCenter extends OwnerSupervisorCommand implements Serializable {
    private static final long serialVersionUID = 1L;

    private final ActorRef<OwnerSupervisorReply> replyTo;

    public ActivateDataCenter(final @Nullable ActorRef<OwnerSupervisorReply> replyTo) {
        this.replyTo = replyTo;
    }

    public ActorRef<OwnerSupervisorReply> getReplyTo() {
        return replyTo;
    }
}
