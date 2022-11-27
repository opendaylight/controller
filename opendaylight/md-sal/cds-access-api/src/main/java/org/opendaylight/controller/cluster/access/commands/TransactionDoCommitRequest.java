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
import java.io.ObjectInput;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

/**
 * A transaction request to perform the final, doCommit, step of the three-phase commit protocol.
 *
 * @author Robert Varga
 */
@Beta
public final class TransactionDoCommitRequest extends TransactionRequest<TransactionDoCommitRequest> {
    interface SerialForm extends TransactionRequest.SerialForm<TransactionDoCommitRequest> {
        @Override
        default TransactionDoCommitRequest readExternal(final ObjectInput in, final TransactionIdentifier target,
                final long sequence, final ActorRef replyTo) {
            return new TransactionDoCommitRequest(target, sequence, replyTo);
        }
    }

    private static final long serialVersionUID = 1L;

    private TransactionDoCommitRequest(final TransactionDoCommitRequest request, final ABIVersion version) {
        super(request, version);
    }

    public TransactionDoCommitRequest(final TransactionIdentifier target, final long sequence, final ActorRef replyTo) {
        super(target, sequence, replyTo);
    }

    @Override
    protected SerialForm externalizableProxy(final ABIVersion version) {
        return ABIVersion.MAGNESIUM.lt(version) ? new TDCR(this) : new TransactionDoCommitRequestProxyV1(this);
    }

    @Override
    protected TransactionDoCommitRequest cloneAsVersion(final ABIVersion version) {
        return new TransactionDoCommitRequest(this, version);
    }
}
