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
 * Externalizable proxy for use with {@link DestroyLocalHistoryRequest}. It implements the initial (Boron) serialization
 * format.
 *
 * @author Robert Varga
 */
final class DestroyLocalHistoryRequestProxyV1 extends AbstractLocalHistoryRequestProxy<DestroyLocalHistoryRequest> {
    private static final long serialVersionUID = 1L;

    public DestroyLocalHistoryRequestProxyV1() {
        // For Externalizable
    }

    DestroyLocalHistoryRequestProxyV1(final DestroyLocalHistoryRequest request) {
        super(request);
    }

    @Override
    protected DestroyLocalHistoryRequest createRequest(final LocalHistoryIdentifier target, final ActorRef replyTo) {
        return new DestroyLocalHistoryRequest(target, replyTo);
    }
}
