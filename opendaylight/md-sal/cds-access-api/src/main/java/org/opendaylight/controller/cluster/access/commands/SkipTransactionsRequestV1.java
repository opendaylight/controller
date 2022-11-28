/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import akka.actor.ActorRef;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.UnsignedLong;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serial;
import java.util.List;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.concepts.WritableObjects;

/**
 * Externalizable proxy for use with {@link SkipTransactionsRequest}. It implements the initial
 * (Phosphorus SR1) serialization format.
 */
final class SkipTransactionsRequestV1 extends AbstractTransactionRequestProxy<SkipTransactionsRequest> {
    @Serial
    private static final long serialVersionUID = -7493419007644462643L;

    private List<UnsignedLong> others;

    // checkstyle flags the public modifier as redundant however it is explicitly needed for Java serialization to
    // be able to create instances via reflection.
    @SuppressWarnings("checkstyle:RedundantModifier")
    public SkipTransactionsRequestV1() {
        // For Externalizable
    }

    SkipTransactionsRequestV1(final SkipTransactionsRequest request) {
        super(request);
        others = request.getOthers();
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);

        final int size = in.readInt();
        final var builder = ImmutableList.<UnsignedLong>builderWithExpectedSize(size);
        int idx;
        if (size % 2 != 0) {
            builder.add(UnsignedLong.fromLongBits(WritableObjects.readLong(in)));
            idx = 1;
        } else {
            idx = 0;
        }
        for (; idx < size; idx += 2) {
            final byte hdr = WritableObjects.readLongHeader(in);
            builder.add(UnsignedLong.fromLongBits(WritableObjects.readFirstLong(in, hdr)));
            builder.add(UnsignedLong.fromLongBits(WritableObjects.readSecondLong(in, hdr)));
        }
        others = builder.build();
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        super.writeExternal(out);

        final int size = others.size();
        out.writeInt(size);

        int idx;
        if (size % 2 != 0) {
            WritableObjects.writeLong(out, others.get(0).longValue());
            idx = 1;
        } else {
            idx = 0;
        }
        for (; idx < size; idx += 2) {
            WritableObjects.writeLongs(out, others.get(idx).longValue(), others.get(idx + 1).longValue());
        }
    }

    @Override
    protected SkipTransactionsRequest createRequest(final TransactionIdentifier target, final long sequence,
            final ActorRef replyToActor) {
        return new SkipTransactionsRequest(target, sequence, replyToActor, others);
    }
}
