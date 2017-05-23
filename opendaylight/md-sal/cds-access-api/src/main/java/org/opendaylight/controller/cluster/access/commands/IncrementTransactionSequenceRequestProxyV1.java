/*
 * Copyright (c) 2017 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import akka.actor.ActorRef;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.concepts.WritableObjects;

final class IncrementTransactionSequenceRequestProxyV1
        extends AbstractReadTransactionRequestProxyV1<IncrementTransactionSequenceRequest> {
    private long increment;

    // checkstyle flags the public modifier as redundant however it is explicitly needed for Java serialization to
    // be able to create instances via reflection.
    @SuppressWarnings("checkstyle:RedundantModifier")
    public IncrementTransactionSequenceRequestProxyV1() {
        // For Externalizable
    }

    IncrementTransactionSequenceRequestProxyV1(final IncrementTransactionSequenceRequest request) {
        super(request);
        this.increment = request.getIncrement();
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        super.writeExternal(out);
        WritableObjects.writeLong(out, increment);
    }

    @Override
    public void readExternal(final ObjectInput in) throws ClassNotFoundException, IOException {
        super.readExternal(in);
        increment = WritableObjects.readLong(in);
    }

    @Override
    IncrementTransactionSequenceRequest createReadRequest(final TransactionIdentifier target, final long sequence,
            final ActorRef replyToActor, final boolean snapshotOnly) {
        return new IncrementTransactionSequenceRequest(target, sequence, replyToActor, snapshotOnly, increment);
    }
}
