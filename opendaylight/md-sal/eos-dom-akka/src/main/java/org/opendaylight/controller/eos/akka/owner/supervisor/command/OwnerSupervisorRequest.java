/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.owner.supervisor.command;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.pattern.StatusReply;
import org.eclipse.jdt.annotation.NonNull;

public abstract class OwnerSupervisorRequest<T extends OwnerSupervisorReply> extends OwnerSupervisorCommand
        implements Serializable {
    private static final long serialVersionUID = 1L;

    private final @NonNull ActorRef<StatusReply<T>> replyTo;

    OwnerSupervisorRequest(final ActorRef<StatusReply<T>> replyTo) {
        this.replyTo = requireNonNull(replyTo);
    }

    public final @NonNull ActorRef<StatusReply<T>> getReplyTo() {
        return replyTo;
    }
}
