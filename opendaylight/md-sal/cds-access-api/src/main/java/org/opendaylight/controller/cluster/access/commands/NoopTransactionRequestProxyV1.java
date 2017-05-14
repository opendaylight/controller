package org.opendaylight.controller.cluster.access.commands;

import akka.actor.ActorRef;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.concepts.WritableObjects;

final class IncrementTransactionSequenceRequestProxyV1
        extends AbstractTransactionRequestProxy<IncrementTransactionSequenceRequest> {
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
    protected IncrementTransactionSequenceRequest createRequest(final TransactionIdentifier target, final long sequence,
            final ActorRef replyToActor) {
        return new IncrementTransactionSequenceRequest(target, sequence, replyToActor, increment);
    }
}
