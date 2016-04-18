/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import akka.actor.ActorRef;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;

abstract class AbstractLocalHistoryRequest extends FrontendRequest<LocalHistoryIdentifier> implements LocalHistoryMessage {
    static abstract class LocalHistoryRequestProxy extends AbstractRequestProxy<LocalHistoryIdentifier> {
        private static final long serialVersionUID = 1L;

        public LocalHistoryRequestProxy() {
            // For Externalizable
        }

        LocalHistoryRequestProxy(final LocalHistoryIdentifier identifier, final ActorRef frontendRef) {
            super(identifier, frontendRef);
        }

        @Override
        protected abstract AbstractLocalHistoryRequest readResolve();
    }
    private static final long serialVersionUID = 1L;

    AbstractLocalHistoryRequest(final ActorRef frontendRef, final LocalHistoryIdentifier historyId) {
        super(historyId, frontendRef);
    }

    @Override
    public final FrontendIdentifier getFrontendIdentifier() {
        return getIdentifier().getFrontendId();
    }

    @Override
    public final LocalHistoryIdentifier getLocalHistoryIdentifier() {
        return getIdentifier();
    }
}
