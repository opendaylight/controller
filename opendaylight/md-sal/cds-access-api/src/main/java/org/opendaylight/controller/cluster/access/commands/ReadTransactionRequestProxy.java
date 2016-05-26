package org.opendaylight.controller.cluster.access.commands;

import akka.actor.ActorRef;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Externalizable proxy for use with {@link ReadTransactionRequest}. It implements the initial (Boron) serialization
 * format.
 *
 * @author Robert Varga
 */
final class ReadTransactionRequestProxy extends AbstractReadTransactionRequestProxy<ReadTransactionRequest> {
    private static final long serialVersionUID = 1L;

    public ReadTransactionRequestProxy() {
        // For Externalizable
    }

    ReadTransactionRequestProxy(final ReadTransactionRequest request) {
        super(request);
    }

    @Override
    ReadTransactionRequest createReadRequest(final TransactionIdentifier target, final long sequence,
            final ActorRef replyTo, final YangInstanceIdentifier path) {
        return new ReadTransactionRequest(target, sequence, replyTo, path);
    }
}