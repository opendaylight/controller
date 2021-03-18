/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.akka.eos.bootstrap.command;

import akka.actor.typed.ActorRef;

public class GetRunningContext implements BootstrapCommand {

    private final ActorRef<RunningContext> replyTo;

    public GetRunningContext(final ActorRef<RunningContext> replyTo) {
        this.replyTo = replyTo;
    }

    public ActorRef<RunningContext> getReplyTo() {
        return replyTo;
    }
}
