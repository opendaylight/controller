/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.owner.checker.command;

import org.apache.pekko.actor.typed.ActorRef;

public final class GetEntitiesRequest extends StateCheckerRequest<GetEntitiesReply> {
    private static final long serialVersionUID = 1L;

    public GetEntitiesRequest(final ActorRef<GetEntitiesReply> replyTo) {
        super(replyTo);
    }

    @Override
    public String toString() {
        return "GetEntitiesRequest{} " + super.toString();
    }
}
