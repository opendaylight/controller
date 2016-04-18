package org.opendaylight.controller.cluster.access.commands;

import akka.actor.ActorRef;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.TransactionRequestIdentifier;

public final class ReadTransactionRequest extends TransactionRequest {
    private static final class Proxy extends AbstractRequestProxy<TransactionRequestIdentifier> {
        private static final long serialVersionUID = 1L;

        public Proxy() {
            // For Externalizable
        }

        Proxy(final TransactionRequestIdentifier identifier, final ActorRef frontendRef) {
            super(identifier, frontendRef);
        }

        @Override
        AbstractRequest<?> readResolve() {
            return new ReadTransactionRequest(getIdentifier(), getFrontendRef());
        }
    }

    private static final long serialVersionUID = 1L;

    ReadTransactionRequest(TransactionRequestIdentifier identifier, ActorRef frontendRef) {
        super(identifier, frontendRef);
    }

    @Override
    public FrontendIdentifier getFrontendIdentifier() {
        return getIdentifier().getTransactionId().getFrontendId();
    }

    @Override
    AbstractRequestProxy<TransactionRequestIdentifier> writeReplace() {
        return new Proxy(getIdentifier(), getFrontendRef());
    }
}
