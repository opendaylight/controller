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
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;

@Beta
public final class DestroyLocalHistoryRequest extends AbstractLocalHistoryRequest {
    private static final class Proxy extends LocalHistoryRequestProxy {
        private static final long serialVersionUID = 1L;

        public Proxy() {
            // For Externalizable
        }

        Proxy(final LocalHistoryIdentifier identifier, final ActorRef replyTo) {
            super(identifier, replyTo);
        }

        @Override
        protected DestroyLocalHistoryRequest readResolve() {
            return new DestroyLocalHistoryRequest(getReplyTo(), getIdentifier());
        }
    }

    private static final long serialVersionUID = 1L;

    public DestroyLocalHistoryRequest(final ActorRef frontendRef, final LocalHistoryIdentifier historyId) {
        super(frontendRef, historyId);
    }

    @Override
    protected Proxy writeReplace() {
        return new Proxy(getLocalHistoryIdentifier(), getReplyTo());
    }
}
