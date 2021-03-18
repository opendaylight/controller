/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.bootstrap.command;

import static java.util.Objects.requireNonNull;

import akka.actor.typed.ActorRef;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.yang.common.Empty;

public final class Terminate extends BootstrapCommand {
    private final @NonNull ActorRef<Empty> replyTo;

    public Terminate(final ActorRef<Empty> replyTo) {
        this.replyTo = requireNonNull(replyTo);
    }

    public @NonNull ActorRef<Empty> getReplyTo() {
        return replyTo;
    }
}
