/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors.client;

import org.opendaylight.controller.cluster.access.concepts.Request;

/**
 * A queue of requests which are pending processing on the leader.
 *
 * @author Robert Varga
 */
final class TargetRequestQueue {
    private long expectedSequence = 0;

    void append(final Request<?, ?> request) {
        final long sequence = request.getSequence();
        if (sequence != request.getSequence()) {
            throw new IllegalArgumentException(String.format("Wrong sequence in request %s, expected %s",
                request, Long.toUnsignedString(expectedSequence, 16)));
        }

        expectedSequence = sequence + 1;
        // FIXME: actually enqueue the request
    }

}
