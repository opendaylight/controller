/*
 * Copyright (c) 2017 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import static com.google.common.base.Preconditions.checkArgument;

import akka.actor.ActorRef;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.concepts.WritableObjects;

/**
 * A blank transaction request. This is used to provide backfill requests in converted retransmit scenarios, such as
 * when a initial request to a transaction (such as a {@link ReadTransactionRequest}) is satisfied by the backend
 * before the need to replay the transaction to a different remote backend.
 */
public final class IncrementTransactionSequenceRequest extends
        AbstractReadTransactionRequest<IncrementTransactionSequenceRequest> {
    interface SerialForm extends AbstractReadTransactionRequest.SerialForm<IncrementTransactionSequenceRequest> {
        @Override
        default void writeExternal(final ObjectOutput out, final IncrementTransactionSequenceRequest msg)
                throws IOException {
            AbstractReadTransactionRequest.SerialForm.super.writeExternal(out, msg);
            WritableObjects.writeLong(out, msg.getIncrement());
        }

        @Override
        default IncrementTransactionSequenceRequest readExternal(final ObjectInput in,
                final TransactionIdentifier target, final long sequence, final ActorRef replyTo,
                final boolean snapshotOnly) throws IOException {
            return new IncrementTransactionSequenceRequest(target, sequence, replyTo, snapshotOnly,
                WritableObjects.readLong(in));
        }
    }

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final long increment;

    public IncrementTransactionSequenceRequest(final IncrementTransactionSequenceRequest request,
            final ABIVersion version) {
        super(request, version);
        increment = request.increment;
    }

    public IncrementTransactionSequenceRequest(final TransactionIdentifier identifier, final long sequence,
            final ActorRef replyTo, final boolean snapshotOnly, final long increment) {
        super(identifier, sequence, replyTo, snapshotOnly);
        checkArgument(increment >= 0, "Unexpected increment %s", increment);
        this.increment = increment;
    }

    /**
     * Return the sequence increment beyond this request's sequence.
     *
     * @return Sequence increment, guaranteed to be non-negative.
     */
    public long getIncrement() {
        return increment;
    }

    @Override
    protected SerialForm externalizableProxy(final ABIVersion version) {
        return ABIVersion.MAGNESIUM.lt(version) ? new ITSR(this) : new IncrementTransactionSequenceRequestProxyV1(this);
    }

    @Override
    protected IncrementTransactionSequenceRequest cloneAsVersion(final ABIVersion targetVersion) {
        return new IncrementTransactionSequenceRequest(this, targetVersion);
    }
}
