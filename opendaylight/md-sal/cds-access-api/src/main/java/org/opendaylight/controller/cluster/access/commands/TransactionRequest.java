package org.opendaylight.controller.cluster.access.commands;

import akka.actor.ActorRef;
import org.opendaylight.controller.cluster.access.concepts.TransactionRequestIdentifier;

public abstract class TransactionRequest extends AbstractRequest<TransactionRequestIdentifier> {
    private static final long serialVersionUID = 1L;

    TransactionRequest(final TransactionRequestIdentifier identifier, final ActorRef frontendRef) {
        super(identifier, frontendRef);
    }


}
