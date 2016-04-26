/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.RequestFailure;

/**
 * Utility methods for dealing with {@link Request}s.
 */
final class RequestUtil {
    private RequestUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * Send a {@link RequestFailure} message corresponding to a particular request.
     *
     * @param request Original request
     * @param cause Failure cause
     */
    static void sendFailure(final Request<?> request, final RequestException cause) {
        request.getFrontendRef().tell(new RequestFailure<>(request.getIdentifier(), cause), ActorRef.noSender());
    }
}
