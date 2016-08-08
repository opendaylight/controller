/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import akka.actor.ActorRef;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;

/**
 * Externalizable proxy for use with {@link PurgeLocalHistoryRequest}. It implements the initial (Boron) serialization
 * format.
 *
 * @author Robert Varga
 */
final class PurgeLocalHistoryRequestProxyV1 extends AbstractLocalHistoryRequestProxy<PurgeLocalHistoryRequest> {
    private static final long serialVersionUID = 1L;

    public PurgeLocalHistoryRequestProxyV1() {
        // For Externalizable
    }

    PurgeLocalHistoryRequestProxyV1(final PurgeLocalHistoryRequest request) {
        super(request);
    }

    @Override
    protected PurgeLocalHistoryRequest createRequest(final LocalHistoryIdentifier target, final long sequence,
            final ActorRef replyTo) {
        return new PurgeLocalHistoryRequest(target, sequence, replyTo);
    }
}
