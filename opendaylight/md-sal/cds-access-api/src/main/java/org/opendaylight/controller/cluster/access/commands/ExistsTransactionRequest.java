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
import org.opendaylight.controller.cluster.access.concepts.TransactionRequestIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

@Beta
public final class ExistsTransactionRequest extends AbstractReadTransactionRequest {
    private static final class Proxy extends AbstractReadRequestProxy {
        private static final long serialVersionUID = 1L;

        public Proxy() {
            // For Externalizable
        }

        Proxy(final TransactionRequestIdentifier identifier, final ActorRef replyTo, final YangInstanceIdentifier path) {
            super(identifier, replyTo, path);
        }

        @Override
        protected ExistsTransactionRequest readResolve() {
            return new ExistsTransactionRequest(getIdentifier(), getReplyTo(), getPath());
        }
    }

    private static final long serialVersionUID = 1L;

    public ExistsTransactionRequest(final TransactionRequestIdentifier identifier, final ActorRef frontendRef,
            final YangInstanceIdentifier path) {
        super(identifier, frontendRef, path);
    }

    @Override
    protected Proxy writeReplace() {
        return new Proxy(getIdentifier(), getReplyTo(), getPath());
    }
}
