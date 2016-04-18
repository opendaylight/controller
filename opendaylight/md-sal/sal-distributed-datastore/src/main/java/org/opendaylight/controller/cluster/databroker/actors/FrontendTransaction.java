package org.opendaylight.controller.cluster.databroker.actors;

import akka.actor.ActorRef;
import com.google.common.util.concurrent.SettableFuture;
import java.util.ArrayDeque;
import java.util.Queue;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequestBuilder;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequest;
import org.opendaylight.controller.cluster.access.concepts.GlobalTransactionIdentifier;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.LocalTransactionIdentifier;

final class FrontendTransaction {
    private final SettableFuture<Void> submitFuture = SettableFuture.create();
    private final Queue<ModifyTransactionRequest> readyOperations = new ArrayDeque<>(1);
    private final ModifyTransactionRequestBuilder builder;
    private final GlobalTransactionIdentifier transactionId;

    FrontendTransaction(final ActorRef frontendRef, final LocalHistoryIdentifier historyId, final long id) {
        this.transactionId = new GlobalTransactionIdentifier(historyId.getFrontendId(),
            new LocalTransactionIdentifier(historyId.getHistoryId(), id));

         this.builder = new ModifyTransactionRequestBuilder(transactionId, frontendRef);
    }

}
