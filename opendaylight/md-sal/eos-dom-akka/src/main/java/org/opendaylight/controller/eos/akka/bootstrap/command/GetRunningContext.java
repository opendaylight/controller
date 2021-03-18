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

public final class GetRunningContext extends BootstrapCommand {
    private final ActorRef<RunningContext> replyTo;

    public GetRunningContext(final ActorRef<RunningContext> replyTo) {
        this.replyTo = requireNonNull(replyTo);
    }

    public ActorRef<RunningContext> getReplyTo() {
        return replyTo;
    }
}
