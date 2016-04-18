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

public final class CreateLocalHistoryRequest extends AbstractLocalHistoryRequest {
    private static final class Proxy extends LocalHistoryRequestProxy {
        private static final long serialVersionUID = 1L;

        public Proxy() {
            // For Externalizable
        }

        Proxy(final LocalHistoryIdentifier identifier, final ActorRef frontendRef) {
            super(identifier, frontendRef);
        }

        @Override
        CreateLocalHistoryRequest readResolve() {
            return new CreateLocalHistoryRequest(getFrontendRef(), getHistoryId());
        }
    }

    private static final long serialVersionUID = 1L;

    public CreateLocalHistoryRequest(final ActorRef frontendRef, final LocalHistoryIdentifier historyId) {
        super(frontendRef, historyId);
    }

    @Override
    Proxy writeReplace() {
        return new Proxy(getLocalHistoryIdentifier(), getFrontendRef());
    }
}
