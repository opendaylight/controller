/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.owner.checker.command;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import org.apache.pekko.actor.typed.ActorRef;
import org.eclipse.jdt.annotation.NonNull;

public abstract class StateCheckerRequest<T extends StateCheckerReply> extends StateCheckerCommand
        implements Serializable {
    private static final long serialVersionUID = 1L;

    private final @NonNull ActorRef<T> replyTo;

    StateCheckerRequest(final ActorRef<T> replyTo) {
        this.replyTo = requireNonNull(replyTo);
    }

    public final @NonNull ActorRef<T> getReplyTo() {
        return replyTo;
    }
}
