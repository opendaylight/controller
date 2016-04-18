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

@Beta
public abstract class AbstractLocalTransactionRequest extends TransactionRequest {
    private static final long serialVersionUID = 1L;

    AbstractLocalTransactionRequest(final TransactionRequestIdentifier identifier, final ActorRef replyTo) {
        super(identifier, replyTo);
    }

    @Override
    protected final AbstractRequestProxy<TransactionRequestIdentifier> writeReplace() {
        throw new UnsupportedOperationException("Local transaction request should never be serialized");
    }
}
