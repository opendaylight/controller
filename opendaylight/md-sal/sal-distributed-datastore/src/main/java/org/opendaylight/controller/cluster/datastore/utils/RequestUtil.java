/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import akka.actor.ActorRef;
import org.opendaylight.controller.cluster.access.commands.FrontendRequest;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.RequestFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for dealing with {@link Request}s.
 */
public final class RequestUtil {
    private static final Logger LOG = LoggerFactory.getLogger(RequestUtil.class);

    private RequestUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * Send a {@link RequestFailure} message corresponding to a particular request.
     *
     * @param request Original request
     * @param cause Failure cause
     */
    public static void sendFailure(final Request<?> request, final RequestException cause) {
        request.getReplyTo().tell(new RequestFailure<>(request.getIdentifier(), cause), ActorRef.noSender());
    }

    /**
     * Check if the request's {@link FrontendIdentifier} matches an expected value. If it does not, forward the message
     * to the specified actor. Return value indicates whether the request was forwarded or not.
     *
     * This is has side-effects and is implementation-specific.
     *
     * @param expected Expected frontend sender
     * @param request Request being processed
     * @param sendTo An {@link ActorRef} of the target actor. This should usually be the parent of the actor invoking
     *               this method.
     * @return True if a mismatch has been detected and the request was forwarded to the specified actor. Callers are
     *              expected to stop processing the request, as that would lead to twice-processed requests possible.
     */
    public static boolean checkRequestFrontend(final FrontendIdentifier expected, final FrontendRequest<?> request,
            final ActorRef forwardTo) {
        if (!expected.equals(request.getFrontendIdentifier())) {
            LOG.trace("Expected frontend {} got {}, forwarding request {} to {}", expected, request.getIdentifier());
            forwardTo.tell(request, ActorRef.noSender());
            return true;
        } else {
            LOG.trace("Frontend {} matched request {}", expected, request.getIdentifier());
            return false;
        }
    }
}
