/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import akka.actor.ActorRef;
import com.google.common.annotations.Beta;
import org.opendaylight.controller.cluster.access.concepts.RequestException;

/**
 * General error raised when the recipient of a {@link Request} is not the correct backend to talk to. This typically
 * means that the backend processing has moved and the frontend needs to run rediscovery and retry the request.
 *
 * @author Robert Varga
 */
@Beta
public final class NotLeaderException extends RequestException {
    private static final long serialVersionUID = 1L;

    public NotLeaderException(final ActorRef me) {
        super("Actor " + me + " is not the current leader");
    }

    @Override
    public boolean isRetriable() {
        return false;
    }
}
