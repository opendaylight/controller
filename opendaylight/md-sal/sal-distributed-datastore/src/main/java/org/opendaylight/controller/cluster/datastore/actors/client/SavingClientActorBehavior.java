package org.opendaylight.controller.cluster.datastore.actors.client;

import akka.persistence.SaveSnapshotFailure;
import akka.persistence.SaveSnapshotSuccess;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SavingClientActorBehavior<T extends FrontendType> extends RecoveredClientActorBehavior<InitialClientActorContext<T>, T> {
    private static final Logger LOG = LoggerFactory.getLogger(SavingClientActorBehavior.class);
    private final ClientIdentifier<T> myId;

    SavingClientActorBehavior(final InitialClientActorContext<T> context, final ClientIdentifier<T> nextId) {
        super(context);
        this.myId = Preconditions.checkNotNull(nextId);
    }

    @Override
    AbstractClientActorBehavior<?> onReceiveCommand(Object command) {
        if (command instanceof SaveSnapshotFailure) {
            LOG.error("{}: failed to persist state", persistenceId(), ((SaveSnapshotFailure) command).cause());
            return null;
        } else if (command instanceof SaveSnapshotSuccess) {
            return context().createBehavior(new ClientActorContext<>(persistenceId(), myId));
        } else {
            LOG.warn("{}: ignoring command {}", persistenceId(), command);
            return this;
        }
    }
}