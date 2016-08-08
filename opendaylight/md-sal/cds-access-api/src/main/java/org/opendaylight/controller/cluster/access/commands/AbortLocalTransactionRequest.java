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
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

/**
 * Request to abort a local transaction. Since local transactions do not introduce state on the backend until they
 * are ready, the purpose of this message is to inform the backend that a message identifier has been used. This is
 * not important for single transactions, but is critical to ensure transaction ordering within local histories.
 *
 * @author Robert Varga
 */
@Beta
public final class AbortLocalTransactionRequest extends AbstractLocalTransactionRequest<AbortLocalTransactionRequest> {
    private static final long serialVersionUID = 1L;

    public AbortLocalTransactionRequest(final @Nonnull TransactionIdentifier identifier,
            final @Nonnull ActorRef replyTo) {
        super(identifier, 0, replyTo);
    }
}