package org.opendaylight.controller.cluster.access.commands;

import akka.actor.ActorRef;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Externalizable proxy for use with {@link ExistsTransactionRequest}. It implements the initial (Boron) serialization
 * format.
 *
 * @author Robert Varga
 */
final class ExistsTransactionRequestProxy extends AbstractReadTransactionRequestProxy<ExistsTransactionRequest> {
    private static final long serialVersionUID = 1L;

    public ExistsTransactionRequestProxy() {
        // For Externalizable
    }

    ExistsTransactionRequestProxy(final ExistsTransactionRequest request) {
        super(request);
    }

    @Override
    ExistsTransactionRequest createReadRequest(final TransactionIdentifier target, final long sequence,
            final ActorRef replyTo, final YangInstanceIdentifier path) {
        return new ExistsTransactionRequest(target, sequence, replyTo, path);
    }
}